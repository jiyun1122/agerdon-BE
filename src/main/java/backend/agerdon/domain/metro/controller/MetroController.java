package backend.agerdon.domain.metro.controller;

import backend.agerdon.domain.metro.dto.response.MetroLastTrainResponse;
import backend.agerdon.domain.metro.service.MetroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Metro", description = "지하철 실시간 도착/막차 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metro")
public class MetroController {

    private final MetroService metroService;

    @Operation(
            summary = "지하철 실시간 도착/막차 조회",
            description = "역명과 호선 번호(1~9)로 서울 열린데이터광장 실시간 도착정보를 조회합니다. "
                    + "환승역은 여러 노선이 섞여 오므로 line 파라미터로 원하는 노선만 필터링합니다. "
                    + "각 도착 항목의 isLastTrain이 true면 해당 열차가 막차입니다. "
                    + "예시: station=홍대입구, line=2"
    )
    @GetMapping("/last-train")
    public ResponseEntity<MetroLastTrainResponse> getLastTrain(
            @Parameter(description = "역명 (예: 홍대입구)") @RequestParam String station,
            @Parameter(description = "호선 번호 1~9") @RequestParam int line
    ) {
        return ResponseEntity.ok(metroService.getLastTrain(station, line));
    }
}
