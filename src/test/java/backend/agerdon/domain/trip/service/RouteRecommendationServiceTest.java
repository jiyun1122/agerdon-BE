package backend.agerdon.domain.trip.service;

import backend.agerdon.domain.trip.entity.RouteType;
import backend.agerdon.domain.trip.provider.RouteCandidate;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteRecommendationServiceTest {

    private final RouteRecommendationService service = new RouteRecommendationService();
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 23, 23, 30);

    @Test
    void recommendsRegularTransitWithLatestGoldenTime() {
        RouteCandidate subway = candidate(RouteType.SUBWAY, now.plusMinutes(40), 15, 50, 1_400);
        RouteCandidate bus = candidate(RouteType.BUS, now.plusMinutes(38), 5, 60, 1_500);
        RouteCandidate taxi = candidate(RouteType.TAXI, null, 0, 30, 18_000);

        RouteRecommendationResult result = service.recommend(List.of(subway, bus, taxi), now);

        assertSame(bus, result.recommended());
    }

    @Test
    void breaksGoldenTimeTieByTotalMinutesThenFare() {
        RouteCandidate slower = candidate(RouteType.SUBWAY, now.plusMinutes(40), 10, 60, 1_300);
        RouteCandidate faster = candidate(RouteType.BUS, now.plusMinutes(35), 5, 50, 1_500);

        RouteRecommendationResult result = service.recommend(List.of(slower, faster), now);

        assertSame(faster, result.recommended());
    }

    @Test
    void excludesNightBusOverTwentyMinuteWalkAndFallsBackToTaxi() {
        RouteCandidate expiredSubway =
                candidate(RouteType.SUBWAY, now.minusMinutes(1), 5, 50, 1_400);
        RouteCandidate farNightBus =
                candidate(RouteType.NBUS, now.plusMinutes(60), 21, 70, 2_500);
        RouteCandidate taxi = candidate(RouteType.TAXI, null, 0, 30, 18_000);

        RouteRecommendationResult result =
                service.recommend(List.of(expiredSubway, farNightBus, taxi), now);

        assertSame(taxi, result.recommended());
        assertEquals(List.of(expiredSubway, taxi), result.candidates());
    }

    @Test
    void recommendsAvailableNightBusWhenRegularTransitExpired() {
        RouteCandidate expiredBus =
                candidate(RouteType.BUS, now.minusMinutes(1), 5, 60, 1_500);
        RouteCandidate nightBus =
                candidate(RouteType.NBUS, now.plusMinutes(45), 15, 75, 2_500);
        RouteCandidate taxi = candidate(RouteType.TAXI, null, 0, 30, 18_000);

        RouteRecommendationResult result =
                service.recommend(List.of(expiredBus, nightBus, taxi), now);

        assertSame(nightBus, result.recommended());
    }

    @Test
    void throwsWhenNoUsableRouteExists() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.recommend(List.of(), now)
        );

        assertEquals(ErrorCode.ROUTE_NOT_AVAILABLE, exception.getErrorCode());
    }

    private RouteCandidate candidate(
            RouteType type,
            LocalDateTime scheduledAt,
            int walkMinutes,
            int totalMinutes,
            int fare
    ) {
        return new RouteCandidate(
                type.name(),
                type.name() + " 안내",
                totalMinutes,
                walkMinutes,
                scheduledAt,
                fare,
                type
        );
    }
}
