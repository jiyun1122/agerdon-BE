package backend.agerdon.domain.trip.provider;

import java.math.BigDecimal;

public record RouteSearchLocation(
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
