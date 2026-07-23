package backend.agerdon.domain.metro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TrainInfo {
    private String trainNo;
    private String departTime;
    private String destination;
}
