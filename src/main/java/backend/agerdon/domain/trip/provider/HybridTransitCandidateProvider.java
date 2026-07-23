package backend.agerdon.domain.trip.provider;

import backend.agerdon.domain.trip.entity.RouteType;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HybridTransitCandidateProvider {

    private static final double WALKING_ROUTE_FACTOR = 1.25;
    private static final double WALKING_METERS_PER_MINUTE = 75.0;
    private static final double SUBWAY_AVERAGE_SPEED_KMH = 28.0;

    private final HybridRouteProperties properties;

    public List<RouteCandidate> findCandidates(
            RouteSearchLocation origin,
            RouteSearchLocation destination,
            LocalDateTime requestedAt
    ) {
        HybridRouteProperties.Station station = findNearestStation(origin);
        List<RouteCandidate> candidates = new ArrayList<>();
        candidates.add(createSubwayCandidate(station, origin, destination, requestedAt));
        station.getBusRoutes().stream()
                .flatMap(route -> scheduleTimes(route).stream()
                        .map(scheduleTime -> createBusCandidate(
                                station,
                                route,
                                origin,
                                destination,
                                requestedAt,
                                scheduleTime
                        )))
                .forEach(candidates::add);
        return candidates;
    }

    HybridRouteProperties.Station findNearestStation(RouteSearchLocation origin) {
        return properties.getStations().stream()
                .min(Comparator.comparingDouble(station -> distanceKm(
                        origin.latitude().doubleValue(),
                        origin.longitude().doubleValue(),
                        station.getLatitude(),
                        station.getLongitude()
                )))
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTE_NOT_AVAILABLE));
    }

    private RouteCandidate createSubwayCandidate(
            HybridRouteProperties.Station station,
            RouteSearchLocation origin,
            RouteSearchLocation destination,
            LocalDateTime requestedAt
    ) {
        boolean destinationIsEast =
                destination.longitude().doubleValue() >= station.getLongitude();
        HybridRouteProperties.DirectionSchedule schedule = destinationIsEast
                ? station.getEastbound()
                : station.getWestbound();
        LocalDateTime scheduledAt = parseSchedule(schedule.getLastTime(), requestedAt);
        int walkMinutes = walkingMinutes(
                origin,
                station.getLatitude(),
                station.getLongitude()
        );
        int rideMinutes = estimatedRideMinutes(
                station.getLatitude(),
                station.getLongitude(),
                destination,
                SUBWAY_AVERAGE_SPEED_KMH
        );

        return new RouteCandidate(
                "%s %s 막차".formatted(station.getName(), station.getLine()),
                "%s에서 가장 가까운 %s까지 약 %d분 도보 이동 후 %s(%s) 막차를 탑승합니다. "
                        .formatted(
                                origin.name(),
                                station.getName(),
                                walkMinutes,
                                schedule.getTerminal(),
                                schedule.getLabel()
                        )
                        + "막차 시각은 서비스에 입력된 고정 시간표 기준입니다.",
                walkMinutes + rideMinutes,
                walkMinutes,
                scheduledAt,
                station.getSubwayFare(),
                RouteType.SUBWAY
        );
    }

    private RouteCandidate createBusCandidate(
            HybridRouteProperties.Station station,
            HybridRouteProperties.BusRoute route,
            RouteSearchLocation origin,
            RouteSearchLocation destination,
            LocalDateTime requestedAt,
            String scheduleTime
    ) {
        LocalDateTime scheduledAt = parseSchedule(scheduleTime, requestedAt);
        int walkMinutes = walkingMinutes(
                origin,
                route.getStopLatitude(),
                route.getStopLongitude()
        );
        boolean nightBus = route.getType() == RouteType.NBUS;
        String transportName = nightBus ? "심야버스" : "버스";
        String routeName = nightBus
                ? "%s %s %s 출발 · %s".formatted(
                        route.getRouteNo(),
                        transportName,
                        scheduledAt.toLocalTime(),
                        station.getName()
                )
                : "%s %s 막차 · %s".formatted(
                        route.getRouteNo(),
                        transportName,
                        station.getName()
                );
        String scheduleGuide = nightBus
                ? "정류장 출발 시각은 %s이며 첫 운행부터 여러 편을 입력한 목데이터입니다."
                        .formatted(scheduledAt.toLocalTime())
                : "일반버스 막차 시각은 노선별 목데이터입니다.";

        return new RouteCandidate(
                routeName,
                "%s에서 %s까지 약 %d분 도보 이동 후 %s번을 탑승합니다. "
                        .formatted(
                                origin.name(),
                                route.getStopName(),
                                walkMinutes,
                                route.getRouteNo()
                        )
                        + destination.name() + " 방향 이동을 위한 경로입니다. "
                        + scheduleGuide,
                walkMinutes + route.getRideMinutes(),
                walkMinutes,
                scheduledAt,
                route.getFare(),
                route.getType()
        );
    }

    private List<String> scheduleTimes(HybridRouteProperties.BusRoute route) {
        if (route.getType() == RouteType.NBUS
                && route.getDepartureTimes() != null
                && !route.getDepartureTimes().isEmpty()) {
            return route.getDepartureTimes();
        }
        return List.of(route.getLastTime());
    }

    private LocalDateTime parseSchedule(String lastTime, LocalDateTime requestedAt) {
        return TransitScheduleParser.parse(lastTime, requestedAt)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTE_NOT_AVAILABLE));
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
        ) * 1_000 * WALKING_ROUTE_FACTOR;
        return Math.max(1, (int) Math.ceil(distanceMeters / WALKING_METERS_PER_MINUTE));
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
