package backend.agerdon.domain.taxi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DirectionsRequest {
    @Schema(description = "출발지 좌표 (경도,위도)", example = "127.111202,37.394912")
    private String origin;

    @Schema(description = "도착지 좌표 (경도,위도)", example = "127.099323,37.401120")
    private String destination;
}
