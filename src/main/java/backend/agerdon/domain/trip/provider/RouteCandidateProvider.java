package backend.agerdon.domain.trip.provider;

import java.time.LocalDateTime;
import java.util.List;

public interface RouteCandidateProvider {

    List<RouteCandidate> findCandidates(
            RouteSearchLocation origin,
            RouteSearchLocation destination,
            LocalDateTime requestedAt
    );
}
