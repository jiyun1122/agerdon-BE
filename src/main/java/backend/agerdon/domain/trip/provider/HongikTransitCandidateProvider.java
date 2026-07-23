package backend.agerdon.domain.trip.provider;

import backend.agerdon.domain.bus.dto.response.BusArrivalResponse;
import backend.agerdon.domain.bus.dto.response.BusStopInfo;
import backend.agerdon.domain.bus.service.BusService;
import backend.agerdon.domain.metro.dto.response.MetroLastTrainResponse;
import backend.agerdon.domain.metro.dto.response.TrainInfo;
import backend.agerdon.domain.metro.service.MetroService;
import backend.agerdon.domain.trip.entity.RouteType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 홍익대학교 서울캠퍼스 주변에서 이용할 수 있는 실제 대중교통 후보를 만든다.
 *
 * 외부 API는 좌표 기반 대중교통 길찾기를 제공하지 않으므로 출발 역/정류장과
 * 대표 노선은 캠퍼스 특화 카탈로그로 제한한다. 배차/막차 시각은 실제 API 응답을 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HongikTransitCandidateProvider {

    private static final double CAMPUS_LATITUDE = 37.5509;
    private static final double CAMPUS_LONGITUDE = 126.9255;
    private static final double HONGIK_STATION_LATITUDE = 37.5572;
    private static final double HONGIK_STATION_LONGITUDE = 126.9245;
    private static final double HONGIK_BUS_STOP_LATITUDE = 37.5567;
    private static final double HONGIK_BUS_STOP_LONGITUDE = 126.9237;
    private static final double MAX_CAMPUS_DISTANCE_KM = 3.0;

    private static final String HONGIK_STATION_CODE = "239";
    private static final String REGULAR_BUS_ROUTE_ID = "100100118"; // 753
    private static final String NIGHT_BUS_ROUTE_ID = "100100588";   // N62

    private static final int SUBWAY_BASE_FARE = 1_550;
    private static final int BUS_FARE = 1_500;
    private static final int NIGHT_BUS_FARE = 2_500;

    private static final List<String> REGULAR_BUS_DESTINATION_KEYWORDS = List.of(
            "은평", "서대문", "연남", "신촌", "여의도", "영등포", "동작", "상도", "숭실"
    );
    private static final List<String> NIGHT_BUS_DESTINATION_KEYWORDS = List.of(
            "양천", "목동", "강서", "염창", "영등포", "합정", "마포", "서대문", "신촌",
            "중구", "시청", "종로", "동대문", "성동", "왕십리", "광진", "건대", "중랑", "면목"
    );

    private final MetroService metroService;
    private final BusService busService;

    public List<RouteCandidate> findCandidates(
            RouteSearchLocation origin,
            RouteSearchLocation destination,
            LocalDateTime requestedAt
    ) {
        if (!isHongikArea(origin)) {
            return List.of();
        }

        List<RouteCandidate> candidates = new ArrayList<>();
        findSubway(origin, destination, requestedAt).ifPresent(candidates::add);

        if (matchesDestination(destination, REGULAR_BUS_DESTINATION_KEYWORDS)) {
            findBus(origin, destination, requestedAt, REGULAR_BUS_ROUTE_ID, RouteType.BUS)
                    .ifPresent(candidates::add);
        }
        if (matchesDestination(destination, NIGHT_BUS_DESTINATION_KEYWORDS)) {
            findBus(origin, destination, requestedAt, NIGHT_BUS_ROUTE_ID, RouteType.NBUS)
                    .ifPresent(candidates::add);
        }
        return candidates;
    }

    private Optional<RouteCandidate> findSubway(
            RouteSearchLocation origin,
            RouteSearchLocation destination,
            LocalDateTime requestedAt
    ) {
        int weekTag = weekTag(serviceDate(requestedAt));
        int direction = destination.longitude().doubleValue() >= HONGIK_STATION_LONGITUDE ? 1 : 2;

        try {
            MetroLastTrainResponse response =
                    metroService.getLastTrain(HONGIK_STATION_CODE, weekTag, direction);
            Optional<LocalDateTime> lastDeparture = response.getLastTrains().stream()
                    .map(TrainInfo::getDepartTime)
                    .map(value -> TransitScheduleParser.parse(value, requestedAt))
                    .flatMap(Optional::stream)
                    .max(Comparator.naturalOrder());

            if (lastDeparture.isEmpty()) {
                return Optional.empty();
            }

            int walkMinutes = walkingMinutes(
                    origin,
                    HONGIK_STATION_LATITUDE,
                    HONGIK_STATION_LONGITUDE
            );
            int rideMinutes = estimatedRideMinutes(
                    HONGIK_STATION_LATITUDE,
                    HONGIK_STATION_LONGITUDE,
                    destination,
                    26.0
            );
            String terminal = response.getLastTrains().stream()
                    .filter(train -> TransitScheduleParser.parse(train.getDepartTime(), requestedAt)
                            .filter(lastDeparture.get()::equals)
                            .isPresent())
                    .map(TrainInfo::getDestination)
                    .findFirst()
                    .orElse(direction == 1 ? "내선" : "외선");

            return Optional.of(new RouteCandidate(
                    "지하철 막차 · " + response.getLine(),
                    "%s에서 홍대입구역까지 약 %d분 도보 이동 후 %s 방면 막차를 탑승합니다. "
                            .formatted(origin.name(), walkMinutes, terminal)
                            + "막차 시각은 서울교통공사 시간표 기준이며 총소요시간은 예상치입니다.",
                    walkMinutes + rideMinutes,
                    walkMinutes,
                    lastDeparture.get(),
                    estimatedSubwayFare(destination),
                    RouteType.SUBWAY
            ));
        } catch (RuntimeException exception) {
            log.warn("홍대입구역 막차 후보 조회 실패: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<RouteCandidate> findBus(
            RouteSearchLocation origin,
            RouteSearchLocation destination,
            LocalDateTime requestedAt,
            String routeId,
            RouteType routeType
    ) {
        try {
            BusArrivalResponse response = busService.getArrival(routeId);
            Optional<BusStopInfo> stop = response.getStops().stream()
                    .filter(info -> info.getStationName().contains(
                            routeType == RouteType.NBUS ? "홍대입구역" : "동교동삼거리"
                    ))
                    .filter(info -> !info.getLastBusTime().isBlank())
                    .findFirst();

            if (stop.isEmpty()) {
                return Optional.empty();
            }

            Optional<LocalDateTime> scheduledAt =
                    TransitScheduleParser.parse(stop.get().getLastBusTime(), requestedAt);
            if (scheduledAt.isEmpty()) {
                return Optional.empty();
            }

            int walkMinutes = walkingMinutes(
                    origin,
                    HONGIK_BUS_STOP_LATITUDE,
                    HONGIK_BUS_STOP_LONGITUDE
            );
            int rideMinutes = estimatedRideMinutes(
                    HONGIK_BUS_STOP_LATITUDE,
                    HONGIK_BUS_STOP_LONGITUDE,
                    destination,
                    routeType == RouteType.NBUS ? 20.0 : 18.0
            );
            String routeName = response.getRouteName().isBlank()
                    ? (routeType == RouteType.NBUS ? "N62" : "753")
                    : response.getRouteName();
            String kind = routeType == RouteType.NBUS ? "심야버스" : "일반 버스";

            return Optional.of(new RouteCandidate(
                    kind + " " + routeName,
                    "%s에서 %s 정류장까지 약 %d분 도보 이동 후 %s번을 탑승합니다. "
                            .formatted(origin.name(), stop.get().getStationName(), walkMinutes, routeName)
                            + destination.name() + " 인근 정류장에서 하차하며, "
                            + "막차 시각은 서울 버스 도착정보 기준입니다.",
                    walkMinutes + rideMinutes,
                    walkMinutes,
                    scheduledAt.get(),
                    routeType == RouteType.NBUS ? NIGHT_BUS_FARE : BUS_FARE,
                    routeType
            ));
        } catch (RuntimeException exception) {
            log.warn("{} 경로 후보 조회 실패: {}", routeType, exception.getMessage());
            return Optional.empty();
        }
    }

    private boolean isHongikArea(RouteSearchLocation origin) {
        return distanceKm(
                origin.latitude().doubleValue(),
                origin.longitude().doubleValue(),
                CAMPUS_LATITUDE,
                CAMPUS_LONGITUDE
        ) <= MAX_CAMPUS_DISTANCE_KM;
    }

    private boolean matchesDestination(
            RouteSearchLocation destination,
            List<String> keywords
    ) {
        String searchable = (destination.name() + " " + destination.address())
                .toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(searchable::contains);
    }

    private int walkingMinutes(
            RouteSearchLocation origin,
            double targetLatitude,
            double targetLongitude
    ) {
        double distanceMeters = distanceKm(
                origin.latitude().doubleValue(),
                origin.longitude().doubleValue(),
                targetLatitude,
                targetLongitude
        ) * 1_000 * 1.25;
        return Math.max(1, (int) Math.ceil(distanceMeters / 75.0));
    }

    private int estimatedRideMinutes(
            double startLatitude,
            double startLongitude,
            RouteSearchLocation destination,
            double averageSpeedKmh
    ) {
        double distance = distanceKm(
                startLatitude,
                startLongitude,
                destination.latitude().doubleValue(),
                destination.longitude().doubleValue()
        );
        return Math.max(5, (int) Math.ceil(distance / averageSpeedKmh * 60));
    }

    private int estimatedSubwayFare(RouteSearchLocation destination) {
        double distance = distanceKm(
                HONGIK_STATION_LATITUDE,
                HONGIK_STATION_LONGITUDE,
                destination.latitude().doubleValue(),
                destination.longitude().doubleValue()
        );
        if (distance <= 10) {
            return SUBWAY_BASE_FARE;
        }
        return SUBWAY_BASE_FARE + (int) Math.ceil((distance - 10) / 5) * 100;
    }

    private LocalDate serviceDate(LocalDateTime requestedAt) {
        return requestedAt.toLocalTime().isBefore(LocalTime.of(5, 0))
                ? requestedAt.toLocalDate().minusDays(1)
                : requestedAt.toLocalDate();
    }

    private int weekTag(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return 2;
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return 3;
        }
        return 1;
    }

    private double distanceKm(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {
        double earthRadiusKm = 6_371.0;
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1))
                * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2)
                * Math.sin(longitudeDelta / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
