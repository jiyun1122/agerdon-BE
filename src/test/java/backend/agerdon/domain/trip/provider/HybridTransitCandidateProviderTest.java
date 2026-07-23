package backend.agerdon.domain.trip.provider;

import backend.agerdon.domain.trip.entity.RouteType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridTransitCandidateProviderTest {

    private final HybridTransitCandidateProvider provider =
            new HybridTransitCandidateProvider(properties());

    @Test
    void choosesSangsuAndUsesNextDayEastboundLastTrain() {
        RouteSearchLocation origin = location("상수동", 37.5480, 126.9229);
        RouteSearchLocation destination = location("공덕", 37.5445, 126.9510);
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 23, 23, 0);

        List<RouteCandidate> candidates =
                provider.findCandidates(origin, destination, requestedAt);

        RouteCandidate subway = candidates.getFirst();
        assertEquals(RouteType.SUBWAY, subway.type());
        assertTrue(subway.name().contains("상수역"));
        assertTrue(subway.guide().contains("공덕행"));
        assertEquals(
                LocalDateTime.of(2026, 7, 24, 0, 53),
                subway.scheduledAt()
        );
        assertEquals(
                List.of(
                        RouteType.SUBWAY,
                        RouteType.BUS,
                        RouteType.NBUS,
                        RouteType.NBUS,
                        RouteType.NBUS
                ),
                candidates.stream().map(RouteCandidate::type).toList()
        );
        assertEquals(
                List.of(
                        LocalDateTime.of(2026, 7, 23, 23, 50),
                        LocalDateTime.of(2026, 7, 24, 0, 15),
                        LocalDateTime.of(2026, 7, 24, 1, 5)
                ),
                candidates.stream()
                        .filter(candidate -> candidate.type() == RouteType.NBUS)
                        .map(RouteCandidate::scheduledAt)
                        .toList()
        );
    }

    @Test
    void choosesHongikAndUsesWestboundLastTrain() {
        RouteSearchLocation origin = location("홍익대학교 T동", 37.5526, 126.9248);
        RouteSearchLocation destination = location("영등포구청역", 37.5258, 126.8966);
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 23, 23, 0);

        RouteCandidate subway =
                provider.findCandidates(origin, destination, requestedAt).getFirst();

        assertTrue(subway.name().contains("홍대입구역"));
        assertTrue(subway.guide().contains("신도림행"));
        assertEquals(
                LocalDateTime.of(2026, 7, 24, 0, 50),
                subway.scheduledAt()
        );
        assertEquals(
                subway.scheduledAt().minusMinutes(subway.walkMinutes()),
                subway.departureDeadline()
        );
    }

    @Test
    void keepsAfterMidnightLastTrainOnCurrentDateDuringEarlyMorning() {
        RouteSearchLocation origin = location("상수동", 37.5480, 126.9229);
        RouteSearchLocation destination = location("공덕", 37.5445, 126.9510);
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 24, 1, 0);

        RouteCandidate subway =
                provider.findCandidates(origin, destination, requestedAt).getFirst();

        assertEquals(
                LocalDateTime.of(2026, 7, 24, 0, 53),
                subway.scheduledAt()
        );
        assertTrue(subway.departureDeadline().isBefore(requestedAt));
    }

    private HybridRouteProperties properties() {
        HybridRouteProperties properties = new HybridRouteProperties();
        properties.setStations(List.of(
                station(
                        "HONGIK",
                        "홍대입구역",
                        "2호선",
                        37.557192,
                        126.925381,
                        schedule("상행·내선", "을지로입구행", "00:49"),
                        schedule("하행·외선", "신도림행", "00:50"),
                        bus(RouteType.BUS, "7612", 37.557039, 126.923756, "00:32"),
                        nightBus(
                                "N62",
                                37.557039,
                                126.923756,
                                "23:45",
                                "00:10",
                                "01:00"
                        )
                ),
                station(
                        "SANGSU",
                        "상수역",
                        "6호선",
                        37.547716,
                        126.922852,
                        schedule("하행", "공덕행", "00:53"),
                        schedule("상행", "새절행", "00:42"),
                        bus(RouteType.BUS, "7011", 37.547871, 126.923111, "00:18"),
                        nightBus(
                                "N62",
                                37.548165,
                                126.923754,
                                "23:50",
                                "00:15",
                                "01:05"
                        )
                )
        ));
        return properties;
    }

    private HybridRouteProperties.Station station(
            String code,
            String name,
            String line,
            double latitude,
            double longitude,
            HybridRouteProperties.DirectionSchedule eastbound,
            HybridRouteProperties.DirectionSchedule westbound,
            HybridRouteProperties.BusRoute... busRoutes
    ) {
        HybridRouteProperties.Station station = new HybridRouteProperties.Station();
        station.setCode(code);
        station.setName(name);
        station.setLine(line);
        station.setLatitude(latitude);
        station.setLongitude(longitude);
        station.setSubwayFare(1_550);
        station.setEastbound(eastbound);
        station.setWestbound(westbound);
        station.setBusRoutes(List.of(busRoutes));
        return station;
    }

    private HybridRouteProperties.DirectionSchedule schedule(
            String label,
            String terminal,
            String lastTime
    ) {
        HybridRouteProperties.DirectionSchedule schedule =
                new HybridRouteProperties.DirectionSchedule();
        schedule.setLabel(label);
        schedule.setTerminal(terminal);
        schedule.setLastTime(lastTime);
        return schedule;
    }

    private HybridRouteProperties.BusRoute bus(
            RouteType type,
            String routeNo,
            double latitude,
            double longitude,
            String lastTime
    ) {
        HybridRouteProperties.BusRoute route = new HybridRouteProperties.BusRoute();
        route.setType(type);
        route.setRouteNo(routeNo);
        route.setStopName("테스트 정류장");
        route.setStopLatitude(latitude);
        route.setStopLongitude(longitude);
        route.setLastTime(lastTime);
        route.setRideMinutes(40);
        route.setFare(type == RouteType.NBUS ? 2_500 : 1_500);
        return route;
    }

    private HybridRouteProperties.BusRoute nightBus(
            String routeNo,
            double latitude,
            double longitude,
            String... departureTimes
    ) {
        HybridRouteProperties.BusRoute route = new HybridRouteProperties.BusRoute();
        route.setType(RouteType.NBUS);
        route.setRouteNo(routeNo);
        route.setStopName("테스트 심야 정류장");
        route.setStopLatitude(latitude);
        route.setStopLongitude(longitude);
        route.setDepartureTimes(List.of(departureTimes));
        route.setRideMinutes(40);
        route.setFare(2_500);
        return route;
    }

    private RouteSearchLocation location(String name, double latitude, double longitude) {
        return new RouteSearchLocation(
                name,
                "서울특별시",
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude)
        );
    }
}
