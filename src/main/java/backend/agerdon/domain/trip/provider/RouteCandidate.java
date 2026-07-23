package backend.agerdon.domain.trip.provider;

import backend.agerdon.domain.trip.entity.RouteType;

import java.time.LocalDateTime;

public record RouteCandidate(
        String name,
        String guide,
        int totalMinutes,
        int walkMinutes,
        LocalDateTime scheduledAt,
        int fare,
        RouteType type
) {
    public LocalDateTime departureDeadline() {
        return scheduledAt == null ? null : scheduledAt.minusMinutes(walkMinutes);
    }
}
