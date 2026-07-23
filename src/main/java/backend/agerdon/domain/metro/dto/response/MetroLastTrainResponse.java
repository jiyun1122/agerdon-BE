package backend.agerdon.domain.metro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MetroLastTrainResponse {
    private String stationName;
    private int line;
    private List<SubwayArrival> arrivals;
    private SubwayArrival lastTrainArrival; // arrivals 중 막차로 표시된 항목 (없으면 null)
}
