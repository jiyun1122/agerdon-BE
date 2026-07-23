package backend.agerdon.domain.bus.service;

import backend.agerdon.domain.bus.client.BusArrivalClient;
import backend.agerdon.domain.bus.dto.response.BusArrivalResponse;
import backend.agerdon.domain.bus.dto.response.BusStopInfo;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusService {

    private final BusArrivalClient busArrivalClient;

    public BusArrivalResponse getArrival(String routeId) {
        JsonNode itemList = busArrivalClient.getArrivalItemList(routeId);

        if (itemList == null || itemList.isMissingNode()) {
            throw new CustomException(ErrorCode.BUS_STOP_EMPTY);
        }

        String routeName = "";
        List<BusStopInfo> stops = new ArrayList<>();

        // itemList가 정류소 1개면 단일 객체, 여러 개면 배열로 내려온다.
        if (itemList.isArray()) {
            for (JsonNode item : itemList) {
                if (routeName.isEmpty()) {
                    routeName = text(item, "rtNm");
                }
                stops.add(toBusStopInfo(item));
            }
        } else {
            routeName = text(itemList, "rtNm");
            stops.add(toBusStopInfo(itemList));
        }

        return new BusArrivalResponse(routeName, stops);
    }

    private BusStopInfo toBusStopInfo(JsonNode item) {
        return new BusStopInfo(
                text(item, "stNm"),
                text(item, "arsId"),
                text(item, "arrmsg1"),
                text(item, "arrmsg2"),
                "1".equals(text(item, "isLast1")),
                "1".equals(text(item, "isLast2")),
                text(item, "lastTm")
        );
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : "";
    }
}
