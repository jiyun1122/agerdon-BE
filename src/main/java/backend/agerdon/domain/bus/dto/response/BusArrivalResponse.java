package backend.agerdon.domain.bus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BusArrivalResponse {
    private String routeName;
    private List<BusStopInfo> stops;
}
