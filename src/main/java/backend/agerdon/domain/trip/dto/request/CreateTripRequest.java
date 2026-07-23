package backend.agerdon.domain.trip.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {

    @Valid
    @NotNull(message = "출발지는 필수입니다.")
    private LocationRequest origin;

    @Valid
    @NotNull(message = "목적지는 필수입니다.")
    private LocationRequest destination;
}
