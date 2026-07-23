package backend.agerdon.domain.trip.provider;

import backend.agerdon.domain.taxi.client.KakaoMobilityClient;
import backend.agerdon.domain.trip.entity.RouteType;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealRouteCandidateProviderTest {

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

    @Test
    void convertsKakaoSummaryToRealTaxiCandidate() {
        KakaoMobilityClient client = mock(KakaoMobilityClient.class);
        when(client.getDirections("126.92,37.55", "126.9,37.52"))
                .thenReturn("""
                        {
                          "routes": [
                            {
                              "summary": {
                                "distance": 5316,
                                "duration": 840,
                                "fare": {
                                  "taxi": 9100
                                }
                              }
                            }
                          ]
                        }
                        """);
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 23, 23, 0);
        HongikTransitCandidateProvider transitProvider = mock(HongikTransitCandidateProvider.class);
        when(transitProvider.findCandidates(origin, destination, requestedAt))
                .thenReturn(List.of());
        RealRouteCandidateProvider provider = new RealRouteCandidateProvider(
                new KakaoTaxiRouteCandidateProvider(client),
                transitProvider
        );

        List<RouteCandidate> candidates =
                provider.findCandidates(origin, destination, requestedAt);

        assertEquals(1, candidates.size());
        RouteCandidate taxi = candidates.getFirst();
        assertEquals(RouteType.TAXI, taxi.type());
        assertEquals(14, taxi.totalMinutes());
        assertEquals(9_100, taxi.fare());
        assertEquals(0, taxi.walkMinutes());
        assertNull(taxi.scheduledAt());
        verify(client).getDirections("126.92,37.55", "126.9,37.52");
    }

    @Test
    void throwsRouteNotAvailableWhenKakaoReturnsNoRoute() {
        KakaoMobilityClient client = mock(KakaoMobilityClient.class);
        when(client.getDirections("126.92,37.55", "126.9,37.52"))
                .thenReturn("""
                        {"routes":[]}
                        """);
        HongikTransitCandidateProvider transitProvider = emptyTransitProvider();
        RealRouteCandidateProvider provider = new RealRouteCandidateProvider(
                new KakaoTaxiRouteCandidateProvider(client),
                transitProvider
        );

        CustomException exception = assertThrows(
                CustomException.class,
                () -> provider.findCandidates(origin, destination, LocalDateTime.now())
        );

        assertEquals(ErrorCode.ROUTE_NOT_AVAILABLE, exception.getErrorCode());
    }

    @Test
    void throwsKakaoApiErrorWhenSummaryIsMalformed() {
        KakaoMobilityClient client = mock(KakaoMobilityClient.class);
        when(client.getDirections("126.92,37.55", "126.9,37.52"))
                .thenReturn("""
                        {"routes":[{"summary":{}}]}
                        """);
        HongikTransitCandidateProvider transitProvider = emptyTransitProvider();
        RealRouteCandidateProvider provider = new RealRouteCandidateProvider(
                new KakaoTaxiRouteCandidateProvider(client),
                transitProvider
        );

        CustomException exception = assertThrows(
                CustomException.class,
                () -> provider.findCandidates(origin, destination, LocalDateTime.now())
        );

        assertEquals(ErrorCode.KAKAO_API_ERROR, exception.getErrorCode());
    }

    private HongikTransitCandidateProvider emptyTransitProvider() {
        HongikTransitCandidateProvider provider = mock(HongikTransitCandidateProvider.class);
        when(provider.findCandidates(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of());
        return provider;
    }
}
