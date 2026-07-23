package backend.agerdon.domain.trip.dto.response;

import backend.agerdon.domain.trip.entity.RouteType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class RouteResponse {
    private Long routeId;
    private String name;
    private String guide;
    private RouteType type;
    private int totalMinutes;
    private int walkMinutes;
    private OffsetDateTime scheduledAt;
    private OffsetDateTime departureDeadline;
    private int fare;
    private boolean recommended;
}
