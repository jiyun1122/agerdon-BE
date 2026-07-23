package backend.agerdon.domain.metro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubwayArrival {
    private String direction;    // 상행/하행, 내선/외선 (updnLine)
    private String trainLineNm;  // 행선지 방면 (예: "성수행 - 합정방면")
    private String arvlMsg2;     // 도착 메시지 (예: "2분 후", "전역 출발")
    private String arvlMsg3;     // 도착 예정역
    private boolean isLastTrain; // lstcarAt == "1"
}
