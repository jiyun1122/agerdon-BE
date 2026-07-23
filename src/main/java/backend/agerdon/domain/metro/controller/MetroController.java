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

@Tag(name = "Metro", description = "지하철 막차 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metro")
public class MetroController {

    private final MetroService metroService;

    @Operation(
            summary = "지하철 막차 조회",
            description = "역 외부코드/요일/상하행 구분으로 23시~01시 사이 열차(막차 후보)를 반환합니다. "
                    + "외부 API가 2호선 등 일부 노선의 시간표 데이터를 제공하지 않아, 해당 노선/역은 404(METRO-002)가 정상 응답일 수 있습니다."
    )
    @GetMapping("/last-train")
    public ResponseEntity<MetroLastTrainResponse> getLastTrain(
            @Parameter(description = "역 외부코드 (예: 서울역(1호선) = 133). 역명이 아닌 FR_CODE 값이며, 앞자리 0은 포함하지 않습니다.") @RequestParam String station,
            @Parameter(description = "1: 평일, 2: 토요일, 3: 일요일/공휴일") @RequestParam int weekTag,
            @Parameter(description = "1: 상행/내선, 2: 하행/외선") @RequestParam int inoutTag
    ) {
        return ResponseEntity.ok(metroService.getLastTrain(station, weekTag, inoutTag));
    }
}
