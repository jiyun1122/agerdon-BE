package backend.agerdon.domain.trip.service;

import backend.agerdon.domain.trip.dto.response.LocationResponse;
import backend.agerdon.domain.trip.dto.response.RouteResponse;
import backend.agerdon.domain.trip.dto.response.TimerResponse;
import backend.agerdon.domain.trip.dto.response.TripDetailResponse;
import backend.agerdon.domain.trip.entity.Route;
import backend.agerdon.domain.trip.entity.RouteType;
import backend.agerdon.domain.trip.entity.TimerState;
import backend.agerdon.domain.trip.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TripResponseMapper {

    private final Clock clock;

    public TripDetailResponse toResponse(Trip trip, LocalDateTime now) {
        LocationResponse origin = new LocationResponse(
                trip.getOriginName(),
                trip.getOriginAddress(),
                trip.getOriginLatitude(),
                trip.getOriginLongitude()
        );
        LocationResponse destination = new LocationResponse(
                trip.getDestinationName(),
                trip.getDestinationAddress(),
                trip.getDestinationLatitude(),
                trip.getDestinationLongitude()
        );
        List<RouteResponse> routes = trip.getRoutes().stream()
                .filter(route -> isAvailableNightBusOrOtherRoute(route, now))
                .map(this::toRouteResponse)
                .toList();

        return new TripDetailResponse(
                trip.getId(),
                trip.getStatus(),
                origin,
                destination,
                toOffsetDateTime(trip.getStartedAt()),
                toOffsetDateTime(trip.getCompletedAt()),
                toTimerResponse(trip, now),
                routes
        );
    }

    private boolean isAvailableNightBusOrOtherRoute(Route route, LocalDateTime now) {
        if (route.getType() != RouteType.NBUS) {
            return true;
        }
        LocalDateTime departureDeadline = route.getDepartureDeadline();
        return departureDeadline != null && !departureDeadline.isBefore(now);
    }

    private RouteResponse toRouteResponse(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getName(),
                route.getGuide(),
                route.getType(),
                route.getTotalMinutes(),
                route.getWalkMinutes(),
                toOffsetDateTime(route.getScheduledAt()),
                toOffsetDateTime(route.getDepartureDeadline()),
                route.getFare(),
                route.isRecommended()
        );
    }

    private TimerResponse toTimerResponse(Trip trip, LocalDateTime now) {
        Route recommendedRoute = trip.getRoutes().stream()
                .filter(Route::isRecommended)
                .findFirst()
                .orElse(null);

        if (recommendedRoute == null || recommendedRoute.getDepartureDeadline() == null) {
            return new TimerResponse(
                    toOffsetDateTime(now),
                    null,
                    null,
                    TimerState.UNAVAILABLE
            );
        }

        LocalDateTime goldenTime = recommendedRoute.getDepartureDeadline();
        long remainingSeconds = Duration.between(now, goldenTime).getSeconds();
        TimerState state = remainingSeconds >= 0 ? TimerState.RUNNING : TimerState.EXPIRED;

        return new TimerResponse(
                toOffsetDateTime(now),
                toOffsetDateTime(goldenTime),
                remainingSeconds,
                state
        );
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(clock.getZone()).toOffsetDateTime();
    }
}
