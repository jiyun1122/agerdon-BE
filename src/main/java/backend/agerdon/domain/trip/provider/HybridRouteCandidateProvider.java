package backend.agerdon.domain.trip.provider;

import backend.agerdon.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "trip.route-provider", havingValue = "hybrid")
public class HybridRouteCandidateProvider implements RouteCandidateProvider {

    private final HybridTransitCandidateProvider transitCandidateProvider;
    private final KakaoTaxiRouteCandidateProvider taxiCandidateProvider;

    @Override
    public List<RouteCandidate> findCandidates(
            RouteSearchLocation origin,
            RouteSearchLocation destination,
            LocalDateTime requestedAt
    ) {
        List<RouteCandidate> candidates = new ArrayList<>(
                transitCandidateProvider.findCandidates(origin, destination, requestedAt)
        );

        try {
            candidates.add(taxiCandidateProvider.findCandidate(origin, destination));
        } catch (CustomException exception) {
            log.warn("하이브리드 택시 후보 조회 실패: {}", exception.getMessage());
        }
        return candidates;
    }
}
