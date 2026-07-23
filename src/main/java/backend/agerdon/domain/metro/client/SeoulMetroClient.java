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
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 서울 열린데이터광장 지하철 시간표 및 실시간 도착정보 API 호출만 전담한다.
 * 노선 필터링과 막차 판별 같은 비즈니스 로직은 다루지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeoulMetroClient {

    private static final String LEGACY_TIMETABLE_URL_FORMAT =
            "http://openAPI.seoul.go.kr:8088/%s/json/SearchSTNTimeTableByFRCodeService/1/500/%s/%d/%d/";
    private static final String MODERN_TIMETABLE_URL_PREFIX =
            "http://openapi.seoul.go.kr:8088/%s/json/getTrainSch/1/500/%%20/N/";
    private static final String HONGIK_EXTERNAL_CODE = "239";
    private static final String BASE_URL = "http://swopenapi.seoul.go.kr";
    private static final String REALTIME_PATH =
            "/api/subway/{apiKey}/json/realtimeStationArrival/0/{maxRows}/{stationName}";
    private static final int MAX_ROWS = 30;
    private static final String SUCCESS_CODE_PREFIX = "INFO";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.seoul.metro-key}")
    private String seoulMetroKey;

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

    /**
     * 역 외부코드와 요일/방향 기준 시간표 전체 배열을 반환한다.
     * 홍대입구 2호선은 기존 API에 데이터가 없어 getTrainSch API를 사용한다.
     */
    public JsonNode getTimetableRows(String station, int weekTag, int inoutTag) {
        if (HONGIK_EXTERNAL_CODE.equals(station) || "0239".equals(station)) {
            return getHongikTimetableRows(weekTag, inoutTag);
        }

        String url = String.format(
                LEGACY_TIMETABLE_URL_FORMAT,
                seoulMetroKey,
                station,
                weekTag,
                inoutTag
        );

        try {
            String rawResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode service = root.get("SearchSTNTimeTableByFRCodeService");

            if (service == null) {
                JsonNode result = root.path("RESULT");
                log.error(
                        "지하철 시간표 API 오류 응답: code={}, message={}",
                        result.path("CODE").asText(),
                        result.path("MESSAGE").asText()
                );
                throw new CustomException(ErrorCode.METRO_API_ERROR);
            }

            JsonNode result = service.path("RESULT");
            if (result.hasNonNull("CODE") && !"INFO-000".equals(result.path("CODE").asText())) {
                log.error(
                        "지하철 시간표 API 오류 응답: code={}, message={}",
                        result.path("CODE").asText(),
                        result.path("MESSAGE").asText()
                );
                throw new CustomException(ErrorCode.METRO_API_ERROR);
            }
            return service.get("row");
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException | java.io.IOException exception) {
            log.error("지하철 시간표 API 호출 실패: {}", exception.getMessage());
            throw new CustomException(ErrorCode.METRO_API_ERROR);
        }
    }

    /**
     * 기존 SearchSTNTimeTable API에는 2호선 데이터가 없어 신규 getTrainSch를 사용한다.
     * 응답의 response.body.items.item 배열을 반환한다.
     */
    private JsonNode getHongikTimetableRows(int weekTag, int inoutTag) {
        String direction = inoutTag == 1 ? "내선" : "외선";
        String weekday = weekTag == 1 ? "평일" : "주말";
        String url = String.format(MODERN_TIMETABLE_URL_PREFIX, seoulMetroKey)
                + encode(direction)
                + "/"
                + encode(weekday)
                + "/"
                + encode("2호선")
                + "//"
                + encode("홍대입구")
                + "/";

        try {
            String rawResponse = restTemplate.getForObject(URI.create(url), String.class);
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode response = root.path("response");
            String resultCode = response.path("header").path("resultCode").asText();

            if (!"00".equals(resultCode)) {
                log.error(
                        "신규 지하철 시간표 API 오류 응답: code={}, message={}",
                        resultCode,
                        response.path("header").path("resultMsg").asText()
                );
                throw new CustomException(ErrorCode.METRO_API_ERROR);
            }

            JsonNode items = response.path("body").path("items").path("item");
            if (!items.isArray() || items.isEmpty()) {
                return null;
            }
            return items;
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException | java.io.IOException | IllegalArgumentException exception) {
            log.error("신규 지하철 시간표 API 호출 실패: {}", exception.getMessage());
            throw new CustomException(ErrorCode.METRO_API_ERROR);
        }
    }

    private String encode(String pathSegment) {
        return UriUtils.encodePathSegment(pathSegment, StandardCharsets.UTF_8);
    }
}
