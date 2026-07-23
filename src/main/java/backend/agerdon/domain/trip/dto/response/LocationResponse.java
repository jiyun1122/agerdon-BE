package backend.agerdon.domain.trip.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class LocationResponse {
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
