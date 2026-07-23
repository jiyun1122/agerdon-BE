package backend.agerdon.domain.metro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MetroLastTrainResponse {
    private String stationName;
    private String line;
    private String direction;
    private List<TrainInfo> lastTrains;
}
