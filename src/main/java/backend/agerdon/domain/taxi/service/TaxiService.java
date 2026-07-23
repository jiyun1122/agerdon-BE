package backend.agerdon.domain.taxi.service;

import backend.agerdon.domain.taxi.client.KakaoMobilityClient;
import backend.agerdon.domain.taxi.dto.request.DirectionsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxiService {

    private final KakaoMobilityClient kakaoMobilityClient;

    /**
     * 카카오 응답을 그대로 프론트로 전달한다 (가공 없이 pass-through).
     */
    public String getDirections(DirectionsRequest request) {
        return kakaoMobilityClient.getDirections(request.getOrigin(), request.getDestination());
    }
}
