package backend.agerdon.domain.metro.client;

import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 서울 열린데이터광장 지하철 실시간 도착정보 API 호출만 전담한다.
 * 노선 필터링/막차 판별 등 비즈니스 로직은 다루지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeoulMetroClient {

    private static final String BASE_URL = "http://swopenapi.seoul.go.kr";
    private static final String REALTIME_PATH =
            "/api/subway/{apiKey}/json/realtimeStationArrival/0/{maxRows}/{stationName}";
    private static final int MAX_ROWS = 30;
    private static final String SUCCESS_CODE_PREFIX = "INFO";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.seoul.metro-realtime-key}")
    private String seoulMetroRealtimeKey;

    /**
     * 역명 기준 실시간 도착정보 배열을 그대로 반환한다.
     * 현재 도착 예정 열차가 없는 경우(정상 응답이지만 목록이 비어있음)는
     * null을 반환하고, 실제 API 호출/응답 실패만 예외로 처리한다.
     */
    public JsonNode getRealtimeArrivals(String stationName) {
        URI uri = UriComponentsBuilder
                .fromUriString(BASE_URL + REALTIME_PATH)
                .buildAndExpand(seoulMetroRealtimeKey, MAX_ROWS, stationName)
                .encode()
                .toUri();

        try {
            String rawResponse = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode errorMessage = root.path("errorMessage");
            String resultCode = errorMessage.path("code").asText();

            if (!resultCode.startsWith(SUCCESS_CODE_PREFIX)) {
                log.error("지하철 실시간 도착 API 응답 오류: {}", rawResponse);
                throw new CustomException(ErrorCode.METRO_API_ERROR);
            }

            JsonNode list = root.get("realtimeArrivalList");
            return (list != null && list.isArray()) ? list : null;
        } catch (CustomException e) {
            throw e;
        } catch (RestClientException | java.io.IOException e) {
            log.error("지하철 실시간 도착 API 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.METRO_API_ERROR);
        }
    }
}
