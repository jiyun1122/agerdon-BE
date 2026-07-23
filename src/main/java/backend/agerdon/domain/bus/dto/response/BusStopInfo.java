package backend.agerdon.domain.bus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BusStopInfo {
    private String stationName;
    private String arsId;
    private String firstBusMsg;
    private String secondBusMsg;
    private boolean isLast1;
    private boolean isLast2;
    private String lastBusTime;
}
