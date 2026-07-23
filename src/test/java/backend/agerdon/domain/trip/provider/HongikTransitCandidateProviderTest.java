package backend.agerdon.domain.trip.provider;

import backend.agerdon.domain.bus.dto.response.BusArrivalResponse;
import backend.agerdon.domain.bus.dto.response.BusStopInfo;
import backend.agerdon.domain.bus.service.BusService;
import backend.agerdon.domain.metro.dto.response.MetroLastTrainResponse;
import backend.agerdon.domain.metro.dto.response.TrainInfo;
import backend.agerdon.domain.metro.service.MetroService;
import backend.agerdon.domain.trip.entity.RouteType;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HongikTransitCandidateProviderTest {

    private final RouteSearchLocation origin = new RouteSearchLocation(
            "홍익대학교 T동",
            "서울특별시 마포구 와우산로 94",
            new BigDecimal("37.552568681927504"),
            new BigDecimal("126.9247935427622")
    );
    private final RouteSearchLocation destination = new RouteSearchLocation(
            "집",
            "서울특별시 영등포구",
            new BigDecimal("37.52281066689467"),
            new BigDecimal("126.92803614524192")
    );

    @Test
    void createsSubwayBusAndNightBusFromActualApiResponses() {
        MetroService metroService = mock(MetroService.class);
        BusService busService = mock(BusService.class);
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 23, 23, 0);

        when(metroService.getLastTrain("239", 1, 1))
                .thenReturn(new MetroLastTrainResponse(
                        "홍대입구",
                        "02호선",
                        "상행/내선",
                        List.of(
                                new TrainInfo("2001", "23:45:00", "성수"),
                                new TrainInfo("2003", "00:10:00", "을지로입구")
                        )
                ));
        when(busService.getArrival("100100118"))
                .thenReturn(new BusArrivalResponse(
                        "753",
                        List.of(new BusStopInfo(
                                "동교동삼거리연희동방면",
                                "13144",
                                "12분 후",
                                "25분 후",
                                false,
                                true,
                                "20260724003600"
                        ))
                ));
        when(busService.getArrival("100100588"))
                .thenReturn(new BusArrivalResponse(
                        "N62",
                        List.of(new BusStopInfo(
                                "홍대입구역",
                                "14015",
                                "18분 후",
                                "출발대기",
                                false,
                                false,
                                "03:20"
                        ))
                ));

        HongikTransitCandidateProvider provider =
                new HongikTransitCandidateProvider(metroService, busService);

        List<RouteCandidate> candidates =
                provider.findCandidates(origin, destination, requestedAt);

        assertEquals(List.of(RouteType.SUBWAY, RouteType.BUS, RouteType.NBUS),
                candidates.stream().map(RouteCandidate::type).toList());
        assertEquals(LocalDateTime.of(2026, 7, 24, 0, 10), candidates.get(0).scheduledAt());
        assertEquals(1_550, candidates.get(0).fare());
        assertEquals(LocalDateTime.of(2026, 7, 24, 0, 36), candidates.get(1).scheduledAt());
        assertEquals(1_500, candidates.get(1).fare());
        assertEquals(LocalDateTime.of(2026, 7, 24, 3, 20), candidates.get(2).scheduledAt());
        assertEquals(2_500, candidates.get(2).fare());
    }

    @Test
    void keepsOtherTransitCandidatesWhenOneExternalApiFails() {
        MetroService metroService = mock(MetroService.class);
        BusService busService = mock(BusService.class);
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 23, 23, 0);

        when(metroService.getLastTrain("239", 1, 1))
                .thenThrow(new CustomException(ErrorCode.METRO_API_ERROR));
        when(busService.getArrival("100100118"))
                .thenReturn(new BusArrivalResponse(
                        "753",
                        List.of(new BusStopInfo(
                                "동교동삼거리",
                                "13144",
                                "",
                                "",
                                false,
                                false,
                                "00:36"
                        ))
                ));
        when(busService.getArrival("100100588"))
                .thenThrow(new CustomException(ErrorCode.BUS_API_ERROR));

        HongikTransitCandidateProvider provider =
                new HongikTransitCandidateProvider(metroService, busService);

        List<RouteCandidate> candidates =
                provider.findCandidates(origin, destination, requestedAt);

        assertEquals(1, candidates.size());
        assertEquals(RouteType.BUS, candidates.getFirst().type());
    }

    @Test
    void doesNotApplyHongikCatalogOutsideCampusArea() {
        MetroService metroService = mock(MetroService.class);
        BusService busService = mock(BusService.class);
        RouteSearchLocation gangnam = new RouteSearchLocation(
                "강남역",
                "서울특별시 강남구",
                new BigDecimal("37.4979"),
                new BigDecimal("127.0276")
        );
        HongikTransitCandidateProvider provider =
                new HongikTransitCandidateProvider(metroService, busService);

        List<RouteCandidate> candidates = provider.findCandidates(
                gangnam,
                destination,
                LocalDateTime.of(2026, 7, 23, 23, 0)
        );

        assertTrue(candidates.isEmpty());
        verify(metroService, never()).getLastTrain(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()
        );
        verify(busService, never()).getArrival(org.mockito.ArgumentMatchers.anyString());
    }
}
