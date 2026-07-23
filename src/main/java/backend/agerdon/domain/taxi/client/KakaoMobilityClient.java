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
        } catch (RestClientException e) {
            log.error("카카오 길찾기 API 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.KAKAO_API_ERROR);
        }
    }
}
