package backend.agerdon.domain.trip.dto.response;

import backend.agerdon.domain.trip.entity.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class TripDetailResponse {
    private Long tripId;
    private TripStatus status;
    private LocationResponse origin;
    private LocationResponse destination;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private TimerResponse timer;
    private List<RouteResponse> routes;
}
