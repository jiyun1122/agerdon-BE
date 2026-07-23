package backend.agerdon.domain.metro.service;

import backend.agerdon.domain.metro.client.SeoulMetroClient;
import backend.agerdon.domain.metro.dto.response.MetroLastTrainResponse;
import backend.agerdon.domain.metro.dto.response.TrainInfo;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetroService {

    private static final String LAST_TRAIN_WINDOW_START = "23:00:00";
    private static final String LAST_TRAIN_WINDOW_END = "01:00:00";

    private final SeoulMetroClient seoulMetroClient;

    public MetroLastTrainResponse getLastTrain(String station, int weekTag, int inoutTag) {
        JsonNode rows = seoulMetroClient.getTimetableRows(station, weekTag, inoutTag);

        if (rows == null || !rows.isArray() || rows.isEmpty()) {
            throw new CustomException(ErrorCode.METRO_TIMETABLE_EMPTY);
        }

        String stationName = "";
        String lineName = "";
        List<TrainInfo> lastTrains = new ArrayList<>();

        for (JsonNode row : rows) {
            String leftTime = row.path("LEFTTIME").asText();

            if (stationName.isEmpty()) {
                stationName = row.path("STATION_NM").asText();
                lineName = row.path("LINE_NUM").asText();
            }

            // 23시 이후 또는 00시대 열차 = 막차 후보
            if (leftTime.compareTo(LAST_TRAIN_WINDOW_START) >= 0 || leftTime.compareTo(LAST_TRAIN_WINDOW_END) < 0) {
                lastTrains.add(new TrainInfo(
                        row.path("TRAIN_NO").asText(),
                        leftTime,
                        row.path("SUBWAYENAME").asText()
                ));
            }
        }

        String direction = inoutTag == 1 ? "상행/내선" : "하행/외선";
        return new MetroLastTrainResponse(stationName, lineName, direction, lastTrains);
    }
}
