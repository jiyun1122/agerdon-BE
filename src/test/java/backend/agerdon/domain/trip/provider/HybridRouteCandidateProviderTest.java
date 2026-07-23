package backend.agerdon.domain.trip.provider;

import backend.agerdon.domain.trip.entity.RouteType;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HybridRouteCandidateProviderTest {

    private final RouteSearchLocation origin = new RouteSearchLocation(
            "홍익대학교",
            "서울특별시 마포구",
            new BigDecimal("37.5500"),
            new BigDecimal("126.9200")
    );
    private final RouteSearchLocation destination = new RouteSearchLocation(
            "집",
            "서울특별시",
            new BigDecimal("37.5200"),
            new BigDecimal("126.9000")
    );
    private final LocalDateTime requestedAt =
            LocalDateTime.of(2026, 7, 23, 23, 0);

    @Test
    void combinesStaticTransitWithRealTaxi() {
        HybridTransitCandidateProvider transitProvider =
                mock(HybridTransitCandidateProvider.class);
        KakaoTaxiRouteCandidateProvider taxiProvider =
                mock(KakaoTaxiRouteCandidateProvider.class);
        RouteCandidate subway = candidate(RouteType.SUBWAY, requestedAt.plusHours(1));
        RouteCandidate bus = candidate(RouteType.BUS, requestedAt.plusMinutes(50));
        RouteCandidate taxi = candidate(RouteType.TAXI, null);
        when(transitProvider.findCandidates(origin, destination, requestedAt))
                .thenReturn(List.of(subway, bus));
        when(taxiProvider.findCandidate(origin, destination)).thenReturn(taxi);
        HybridRouteCandidateProvider provider =
                new HybridRouteCandidateProvider(transitProvider, taxiProvider);

        List<RouteCandidate> candidates =
                provider.findCandidates(origin, destination, requestedAt);

        assertEquals(
                List.of(RouteType.SUBWAY, RouteType.BUS, RouteType.TAXI),
                candidates.stream().map(RouteCandidate::type).toList()
        );
    }

    @Test
    void returnsStaticTransitWhenTaxiApiFails() {
        HybridTransitCandidateProvider transitProvider =
                mock(HybridTransitCandidateProvider.class);
        KakaoTaxiRouteCandidateProvider taxiProvider =
                mock(KakaoTaxiRouteCandidateProvider.class);
        RouteCandidate subway = candidate(RouteType.SUBWAY, requestedAt.plusHours(1));
        when(transitProvider.findCandidates(origin, destination, requestedAt))
                .thenReturn(List.of(subway));
        when(taxiProvider.findCandidate(origin, destination))
                .thenThrow(new CustomException(ErrorCode.KAKAO_API_ERROR));
        HybridRouteCandidateProvider provider =
                new HybridRouteCandidateProvider(transitProvider, taxiProvider);

        List<RouteCandidate> candidates =
                provider.findCandidates(origin, destination, requestedAt);

        assertEquals(List.of(subway), candidates);
    }

    private RouteCandidate candidate(RouteType type, LocalDateTime scheduledAt) {
        return new RouteCandidate(
                type.name(),
                "테스트 경로",
                30,
                type == RouteType.TAXI ? 0 : 5,
                scheduledAt,
                1_500,
                type
        );
    }
}
