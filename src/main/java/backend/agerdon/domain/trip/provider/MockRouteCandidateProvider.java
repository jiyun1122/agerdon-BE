package backend.agerdon.domain.trip.provider;

import backend.agerdon.domain.trip.entity.RouteType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(
        name = "trip.route-provider",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockRouteCandidateProvider implements RouteCandidateProvider {

    @Override
    public List<RouteCandidate> findCandidates(
            RouteSearchLocation origin,
            RouteSearchLocation destination,
            LocalDateTime requestedAt
    ) {
        return List.of(
                new RouteCandidate(
                        "지하철 막차",
                        origin.name() + "에서 홍대입구역까지 도보 이동 후 지하철을 탑승합니다.",
                        52,
                        12,
                        requestedAt.plusMinutes(45),
                        1_400,
                        RouteType.SUBWAY
                ),
                new RouteCandidate(
                        "일반 버스 막차",
                        origin.name() + " 인근 정류장에서 일반 버스를 탑승합니다.",
                        60,
                        8,
                        requestedAt.plusMinutes(38),
                        1_500,
                        RouteType.BUS
                ),
                new RouteCandidate(
                        "N버스",
                        "홍대입구역 인근 심야버스 정류장까지 도보 이동한 뒤 "
                                + destination.name() + " 방향 N버스를 탑승합니다.",
                        75,
                        15,
                        requestedAt.plusMinutes(90),
                        2_500,
                        RouteType.NBUS
                ),
                new RouteCandidate(
                        "택시",
                        origin.name() + "에서 택시를 탑승해 " + destination.name() + "까지 이동합니다.",
                        30,
                        0,
                        null,
                        18_000,
                        RouteType.TAXI
                )
        );
    }
}
