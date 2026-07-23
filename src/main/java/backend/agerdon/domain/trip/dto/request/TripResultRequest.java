package backend.agerdon.domain.trip.dto.request;

import backend.agerdon.domain.trip.entity.TripStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TripResultRequest {

    @NotNull(message = "탑승 결과는 필수입니다.")
    private TripStatus status;
}
