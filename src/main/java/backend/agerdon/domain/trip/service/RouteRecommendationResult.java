package backend.agerdon.domain.trip.service;

import backend.agerdon.domain.trip.provider.RouteCandidate;

import java.util.List;

public record RouteRecommendationResult(
        List<RouteCandidate> candidates,
        RouteCandidate recommended
) {
}
