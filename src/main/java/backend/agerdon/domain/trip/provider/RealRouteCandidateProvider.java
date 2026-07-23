package backend.agerdon.domain.trip.provider;

import backend.agerdon.domain.taxi.client.KakaoMobilityClient;
import backend.agerdon.domain.trip.entity.RouteType;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 실제 외부 API 데이터를 Trip 경로 후보로 변환한다.
 * 홍익대 상권 대중교통 후보와 카카오 자동차 경로를 각각 조회하며,
 * 한 외부 API가 실패해도 조회에 성공한 다른 후보는 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "trip.route-provider", havingValue = "real")
public class RealRouteCandidateProvider implements RouteCandidateProvider {

    private final KakaoMobilityClient kakaoMobilityClient;
    private final HongikTransitCandidateProvider transitCandidateProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<RouteCandidate> findCandidates(
            RouteSearchLocation origin,
            RouteSearchLocation destination,
            LocalDateTime requestedAt
    ) {
        List<RouteCandidate> candidates = new ArrayList<>(
                transitCandidateProvider.findCandidates(origin, destination, requestedAt)
        );

        CustomException taxiFailure = null;
        try {
            candidates.add(createTaxiCandidate(origin, destination));
        } catch (CustomException exception) {
            log.warn("택시 경로 후보 조회 실패: {}", exception.getMessage());
            taxiFailure = exception;
        }

        if (candidates.isEmpty()) {
            if (taxiFailure != null) {
                throw taxiFailure;
            }
            throw new CustomException(ErrorCode.ROUTE_NOT_AVAILABLE);
        }
        return candidates;
    }

    private RouteCandidate createTaxiCandidate(
            RouteSearchLocation origin,
            RouteSearchLocation destination
    ) {
        String rawResponse = kakaoMobilityClient.getDirections(
                toCoordinate(origin),
                toCoordinate(destination)
        );
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new CustomException(ErrorCode.KAKAO_API_ERROR);
        }
        try {
            JsonNode routes = objectMapper.readTree(rawResponse).path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                throw new CustomException(ErrorCode.ROUTE_NOT_AVAILABLE);
            }

            JsonNode summary = routes.get(0).path("summary");
            int durationSeconds = summary.path("duration").asInt(-1);
            int distanceMeters = summary.path("distance").asInt(-1);
            int taxiFare = summary.path("fare").path("taxi").asInt(-1);

            if (durationSeconds < 0 || distanceMeters < 0 || taxiFare < 0) {
                throw new CustomException(ErrorCode.KAKAO_API_ERROR);
            }

            int totalMinutes = Math.max(1, (durationSeconds + 59) / 60);
            String guide = "%s에서 택시를 탑승해 %s까지 이동합니다. 예상 거리 %.1fkm, 약 %d분입니다."
                    .formatted(
                            origin.name(),
                            destination.name(),
                            distanceMeters / 1_000.0,
                            totalMinutes
                    );

            return new RouteCandidate(
                    "택시",
                    guide,
                    totalMinutes,
                    0,
                    null,
                    taxiFare,
                    RouteType.TAXI
            );
        } catch (CustomException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            log.error("카카오 길찾기 응답 변환 실패: {}", exception.getMessage());
            throw new CustomException(ErrorCode.KAKAO_API_ERROR);
        }
    }

    private String toCoordinate(RouteSearchLocation location) {
        return location.longitude().stripTrailingZeros().toPlainString()
                + ","
                + location.latitude().stripTrailingZeros().toPlainString();
    }
}
