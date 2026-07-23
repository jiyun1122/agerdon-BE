package backend.agerdon.domain.taxi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DirectionsRequest {
    private String origin;      // 경도,위도 (출발지)
    private String destination; // 경도,위도 (도착지)
}
