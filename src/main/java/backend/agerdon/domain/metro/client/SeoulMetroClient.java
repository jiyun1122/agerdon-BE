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

/**
 * 서울 열린데이터광장 지하철 시간표 API 호출만 전담한다.
 * 막차 필터링 등 비즈니스 로직은 다루지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeoulMetroClient {

    private static final String TIMETABLE_URL_FORMAT =
            "http://openAPI.seoul.go.kr:8088/%s/json/SearchSTNTimeTableByFRCodeService/1/500/%s/%d/%d/";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.seoul.metro-key}")
    private String seoulMetroKey;

    /**
     * 시간표 전체(row 배열)를 그대로 반환한다. 데이터가 없으면 null.
     */
    public JsonNode getTimetableRows(String station, int weekTag, int inoutTag) {
        String url = String.format(TIMETABLE_URL_FORMAT, seoulMetroKey, station, weekTag, inoutTag);

        try {
            String rawResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode service = root.get("SearchSTNTimeTableByFRCodeService");

            if (service == null) {
                throw new CustomException(ErrorCode.METRO_API_ERROR);
            }
            return service.get("row");
        } catch (CustomException e) {
            throw e;
        } catch (RestClientException | java.io.IOException e) {
            log.error("지하철 시간표 API 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.METRO_API_ERROR);
        }
    }
}
