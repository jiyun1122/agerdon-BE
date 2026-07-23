package backend.agerdon.domain.trip.dto.response;

import backend.agerdon.domain.trip.entity.TimerState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class TimerResponse {
    private OffsetDateTime serverTime;
    private OffsetDateTime goldenTime;
    private Long remainingSeconds;
    private TimerState state;
}
