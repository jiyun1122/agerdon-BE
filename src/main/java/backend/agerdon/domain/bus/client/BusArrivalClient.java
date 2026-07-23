package backend.agerdon.domain.bus.client;

import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 공공데이터포털 버스 도착정보 API(XML) 호출만 전담한다.
 * serviceKey는 발급 시점에 이미 URL 인코딩되어 있으므로,
 * UriComponentsBuilder로 다시 인코딩하지 않고 문자열을 그대로 조립한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusArrivalClient {

    private static final String ARRIVAL_URL = "http://ws.bus.go.kr/api/rest/arrive/getArrInfoByRouteAll";

    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper = new XmlMapper();

    @Value("${api.bus.service-key}")
    private String busServiceKey;

    /**
     * msgBody.itemList 노드를 그대로 반환한다. 데이터가 없으면 null.
     */
    public JsonNode getArrivalItemList(String routeId) {
        String url = ARRIVAL_URL + "?serviceKey=" + busServiceKey + "&busRouteId=" + routeId;

        try {
            String xmlResponse = restTemplate.getForObject(new URI(url), String.class);
            JsonNode root = xmlMapper.readTree(xmlResponse);
            JsonNode msgBody = root.get("msgBody");

            if (msgBody == null) {
                throw new CustomException(ErrorCode.BUS_API_ERROR);
            }
            return msgBody.get("itemList");
        } catch (CustomException e) {
            throw e;
        } catch (RestClientException | IOException | URISyntaxException e) {
            log.error("버스 도착정보 API 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.BUS_API_ERROR);
        }
    }
}
