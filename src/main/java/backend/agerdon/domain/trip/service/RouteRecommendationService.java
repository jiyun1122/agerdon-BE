package backend.agerdon.domain.trip.service;

import backend.agerdon.domain.trip.entity.RouteType;
import backend.agerdon.domain.trip.provider.RouteCandidate;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class RouteRecommendationService {

    private static final int MAX_NIGHT_BUS_WALK_MINUTES = 20;
    private static final Set<RouteType> REGULAR_TRANSIT_TYPES =
            Set.of(RouteType.BUS, RouteType.SUBWAY);

    private static final Comparator<RouteCandidate> SCHEDULED_ROUTE_PREFERENCE =
            Comparator.comparing(RouteCandidate::departureDeadline, Comparator.reverseOrder())
                    .thenComparingInt(RouteCandidate::totalMinutes)
                    .thenComparingInt(RouteCandidate::fare);

    private static final Comparator<RouteCandidate> TAXI_PREFERENCE =
            Comparator.comparingInt(RouteCandidate::totalMinutes)
                    .thenComparingInt(RouteCandidate::fare);

    public RouteRecommendationResult recommend(
            List<RouteCandidate> candidates,
            LocalDateTime now
    ) {
        if (candidates == null || candidates.isEmpty()) {
            throw new CustomException(ErrorCode.ROUTE_NOT_AVAILABLE);
        }

        List<RouteCandidate> usableCandidates = candidates.stream()
                .filter(candidate -> candidate.type() != RouteType.NBUS
                        || candidate.walkMinutes() <= MAX_NIGHT_BUS_WALK_MINUTES)
                .toList();

        RouteCandidate recommended = findRegularTransit(usableCandidates, now);
        if (recommended == null) {
            recommended = findNightBus(usableCandidates, now);
        }
        if (recommended == null) {
            recommended = findTaxi(usableCandidates);
        }
        if (recommended == null) {
            throw new CustomException(ErrorCode.ROUTE_NOT_AVAILABLE);
        }

        return new RouteRecommendationResult(usableCandidates, recommended);
    }

    private RouteCandidate findRegularTransit(List<RouteCandidate> candidates, LocalDateTime now) {
        return candidates.stream()
                .filter(candidate -> REGULAR_TRANSIT_TYPES.contains(candidate.type()))
                .filter(candidate -> canDepart(candidate, now))
                .sorted(SCHEDULED_ROUTE_PREFERENCE)
                .findFirst()
                .orElse(null);
    }

    private RouteCandidate findNightBus(List<RouteCandidate> candidates, LocalDateTime now) {
        return candidates.stream()
                .filter(candidate -> candidate.type() == RouteType.NBUS)
                .filter(candidate -> canDepart(candidate, now))
                .sorted(SCHEDULED_ROUTE_PREFERENCE)
                .findFirst()
                .orElse(null);
    }

    private RouteCandidate findTaxi(List<RouteCandidate> candidates) {
        return candidates.stream()
                .filter(candidate -> candidate.type() == RouteType.TAXI)
                .sorted(TAXI_PREFERENCE)
                .findFirst()
                .orElse(null);
    }

    private boolean canDepart(RouteCandidate candidate, LocalDateTime now) {
        return candidate.departureDeadline() != null
                && !candidate.departureDeadline().isBefore(now);
    }
}
