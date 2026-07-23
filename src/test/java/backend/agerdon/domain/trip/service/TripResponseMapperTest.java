package backend.agerdon.domain.trip.service;

import backend.agerdon.domain.member.entity.Member;
import backend.agerdon.domain.trip.dto.response.TripDetailResponse;
import backend.agerdon.domain.trip.entity.Route;
import backend.agerdon.domain.trip.entity.RouteType;
import backend.agerdon.domain.trip.entity.TimerState;
import backend.agerdon.domain.trip.entity.Trip;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TripResponseMapperTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    void calculatesGoldenTimeAcrossMidnight() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T14:50:00Z"), SEOUL);
        TripResponseMapper mapper = new TripResponseMapper(clock);
        LocalDateTime now = LocalDateTime.of(2026, 7, 23, 23, 50);
        Trip trip = tripWithRecommendedRoute(
                LocalDateTime.of(2026, 7, 24, 0, 10),
                10,
                RouteType.SUBWAY
        );

        TripDetailResponse response = mapper.toResponse(trip, now);

        assertEquals(TimerState.RUNNING, response.getTimer().getState());
        assertEquals(600L, response.getTimer().getRemainingSeconds());
        assertEquals(
                LocalDateTime.of(2026, 7, 24, 0, 0).atOffset(ZoneOffset.ofHours(9)),
                response.getTimer().getGoldenTime()
        );
    }

    @Test
    void expiredTimerKeepsNegativeRemainingSecondsAndNullResult() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T15:20:00Z"), SEOUL);
        TripResponseMapper mapper = new TripResponseMapper(clock);
        LocalDateTime now = LocalDateTime.of(2026, 7, 24, 0, 20);
        Trip trip = tripWithRecommendedRoute(
                LocalDateTime.of(2026, 7, 24, 0, 10),
                10,
                RouteType.SUBWAY
        );

        TripDetailResponse response = mapper.toResponse(trip, now);

        assertEquals(TimerState.EXPIRED, response.getTimer().getState());
        assertEquals(-1_200L, response.getTimer().getRemainingSeconds());
        assertNull(response.getStatus());
    }

    @Test
    void taxiRecommendationHasUnavailableTimer() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T14:50:00Z"), SEOUL);
        TripResponseMapper mapper = new TripResponseMapper(clock);
        Trip trip = tripWithRecommendedRoute(null, 0, RouteType.TAXI);

        TripDetailResponse response = mapper.toResponse(
                trip,
                LocalDateTime.of(2026, 7, 23, 23, 50)
        );

        assertEquals(TimerState.UNAVAILABLE, response.getTimer().getState());
        assertNull(response.getTimer().getGoldenTime());
        assertNull(response.getTimer().getRemainingSeconds());
    }

    @Test
    void exposesOnlyNightBusDeparturesReachableFromCurrentLocation() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T15:41:00Z"), SEOUL);
        TripResponseMapper mapper = new TripResponseMapper(clock);
        LocalDateTime now = LocalDateTime.of(2026, 7, 24, 0, 41);
        Trip trip = tripWithRecommendedRoute(
                LocalDateTime.of(2026, 7, 24, 0, 50),
                9,
                RouteType.SUBWAY
        );
        trip.addRoute(route(
                "이미 놓친 N62",
                RouteType.NBUS,
                LocalDateTime.of(2026, 7, 24, 0, 45),
                5
        ));
        trip.addRoute(route(
                "다음 N62",
                RouteType.NBUS,
                LocalDateTime.of(2026, 7, 24, 1, 0),
                9
        ));

        TripDetailResponse response = mapper.toResponse(trip, now);

        assertEquals(
                List.of(RouteType.SUBWAY, RouteType.NBUS),
                response.getRoutes().stream().map(route -> route.getType()).toList()
        );
        assertEquals("다음 N62", response.getRoutes().get(1).getName());
    }

    private Trip tripWithRecommendedRoute(
            LocalDateTime scheduledAt,
            int walkMinutes,
            RouteType routeType
    ) {
        Member member = Member.builder()
                .email("trip-test@example.com")
                .password("password")
                .nickname("trip-test")
                .build();
        Trip trip = Trip.builder()
                .member(member)
                .originName("홍익대학교")
                .originAddress("서울시 마포구")
                .originLatitude(new BigDecimal("37.5500"))
                .originLongitude(new BigDecimal("126.9200"))
                .destinationName("집")
                .destinationAddress("서울시")
                .destinationLatitude(new BigDecimal("37.5000"))
                .destinationLongitude(new BigDecimal("127.0000"))
                .startedAt(LocalDateTime.of(2026, 7, 23, 23, 0))
                .build();
        trip.addRoute(Route.builder()
                .name(routeType.name())
                .guide("안내")
                .totalMinutes(30)
                .walkMinutes(walkMinutes)
                .scheduledAt(scheduledAt)
                .fare(1_400)
                .recommended(true)
                .type(routeType)
                .build());
        return trip;
    }

    private Route route(
            String name,
            RouteType type,
            LocalDateTime scheduledAt,
            int walkMinutes
    ) {
        return Route.builder()
                .name(name)
                .guide("안내")
                .totalMinutes(60)
                .walkMinutes(walkMinutes)
                .scheduledAt(scheduledAt)
                .fare(2_500)
                .recommended(false)
                .type(type)
                .build();
    }
}
