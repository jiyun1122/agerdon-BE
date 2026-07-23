package backend.agerdon.domain.metro.service;

import backend.agerdon.domain.metro.client.SeoulMetroClient;
import backend.agerdon.domain.metro.dto.response.MetroLastTrainResponse;
import backend.agerdon.domain.metro.dto.response.SubwayArrival;
import backend.agerdon.domain.metro.dto.response.TrainInfo;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetroService {

    private static final String LAST_TRAIN_FLAG = "1";
    private static final String LAST_TRAIN_WINDOW_START = "23:00:00";
    private static final String LAST_TRAIN_WINDOW_END = "01:00:00";

    // 호선 번호 -> 서울 열린데이터광장 subwayId. 필요 시 경의중앙선(1063), 공항철도(1065) 등 확장 가능.
    private static final Map<Integer, String> LINE_TO_SUBWAY_ID = Map.of(
            1, "1001",
            2, "1002",
            3, "1003",
            4, "1004",
            5, "1005",
            6, "1006",
            7, "1007",
            8, "1008",
            9, "1009"
    );

    private final SeoulMetroClient seoulMetroClient;

    public MetroLastTrainResponse getLastTrain(String stationName, int line) {
        String subwayId = LINE_TO_SUBWAY_ID.get(line);
        if (subwayId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 호선입니다: " + line);
        }

        JsonNode rows = seoulMetroClient.getRealtimeArrivals(stationName);
        if (rows == null || rows.isEmpty()) {
            throw new CustomException(ErrorCode.METRO_TIMETABLE_EMPTY);
        }

        List<SubwayArrival> arrivals = new ArrayList<>();
        for (JsonNode row : rows) {
            if (!subwayId.equals(row.path("subwayId").asText())) {
                continue;
            }
            arrivals.add(new SubwayArrival(
                    row.path("updnLine").asText(),
                    row.path("trainLineNm").asText(),
                    row.path("arvlMsg2").asText(),
                    row.path("arvlMsg3").asText(),
                    LAST_TRAIN_FLAG.equals(row.path("lstcarAt").asText())
            ));
        }

        if (arrivals.isEmpty()) {
            throw new CustomException(ErrorCode.METRO_TIMETABLE_EMPTY);
        }

        SubwayArrival lastTrainArrival = arrivals.stream()
                .filter(SubwayArrival::isLastTrain)
                .findFirst()
                .orElse(null);

        return new MetroLastTrainResponse(stationName, line, arrivals, lastTrainArrival);
    }

    /**
     * Trip 골든타임 계산용 당일 막차 시간표를 반환한다.
     * 실시간 도착 API는 운행 직전 열차만 제공하므로 당일 전체 시간표와 분리한다.
     */
    public List<TrainInfo> getScheduledLastTrains(String station, int weekTag, int inoutTag) {
        JsonNode rows = seoulMetroClient.getTimetableRows(station, weekTag, inoutTag);
        if (rows == null || !rows.isArray() || rows.isEmpty()) {
            throw new CustomException(ErrorCode.METRO_TIMETABLE_EMPTY);
        }

        List<TrainInfo> lastTrains = new ArrayList<>();
        for (JsonNode row : rows) {
            String departureTime = text(row, "LEFTTIME", "trainDptreTm");
            if (departureTime.isBlank()) {
                continue;
            }

            if (departureTime.compareTo(LAST_TRAIN_WINDOW_START) >= 0
                    || departureTime.compareTo(LAST_TRAIN_WINDOW_END) < 0) {
                lastTrains.add(new TrainInfo(
                        text(row, "TRAIN_NO", "trainno"),
                        departureTime,
                        text(row, "SUBWAYENAME", "arvlStnNm")
                ));
            }
        }

        return lastTrains;
    }

    private String text(JsonNode node, String legacyField, String modernField) {
        if (node.hasNonNull(legacyField)) {
            return node.path(legacyField).asText();
        }
        if (node.hasNonNull(modernField)) {
            return node.path(modernField).asText();
        }
        return "";
    }
}
