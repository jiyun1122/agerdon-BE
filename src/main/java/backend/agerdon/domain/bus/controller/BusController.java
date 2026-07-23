package backend.agerdon.domain.bus.controller;

import backend.agerdon.domain.bus.dto.response.BusArrivalResponse;
import backend.agerdon.domain.bus.service.BusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bus", description = "버스 도착정보(막차 포함) 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bus")
public class BusController {

    private final BusService busService;

    @Operation(summary = "버스 도착정보 조회", description = "노선 ID로 정류소별 실시간 도착 예정 정보(막차 여부 포함)를 반환합니다.")
    @GetMapping("/arrival")
    public ResponseEntity<BusArrivalResponse> getArrival(
            @Parameter(description = "버스 노선 ID (예: 753번 = 100100118)") @RequestParam String routeId
    ) {
        return ResponseEntity.ok(busService.getArrival(routeId));
    }
}
