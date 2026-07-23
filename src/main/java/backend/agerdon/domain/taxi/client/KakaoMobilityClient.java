package backend.agerdon.domain.taxi.client;

import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 카카오모빌리티 길찾기 API 호출만 전담한다.
 * 요청 조립/인증 헤더/HTTP 호출 외의 비즈니스 로직은 다루지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoMobilityClient {

    private static final String DIRECTIONS_URL = "https://apis-navi.kakaomobility.com/v1/directions";

    private final RestTemplate restTemplate;

    @Value("${api.kakao.rest-key}")
    private String kakaoRestKey;

    public String getDirections(String origin, String destination) {
        String url = UriComponentsBuilder
                .fromUriString(DIRECTIONS_URL)
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            // 카카오가 4xx로 응답한 경우 = 호출자가 보낸 좌표/파라미터 자체가 잘못된 것이므로
            // 서버 장애(502)가 아닌 400으로 알려준다.
            log.warn("카카오 길찾기 API 잘못된 요청: {}", e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.KAKAO_INVALID_REQUEST, e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("카카오 길찾기 API 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.KAKAO_API_ERROR);
        }
    }
}
