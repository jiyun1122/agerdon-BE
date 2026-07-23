package backend.agerdon.domain.taxi.controller;

import backend.agerdon.domain.taxi.dto.request.DirectionsRequest;
import backend.agerdon.domain.taxi.service.TaxiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Taxi", description = "카카오 길찾기 기반 택시비 조회 API")
@RestController
@RequiredArgsConstructor
public class TaxiController {

    private final TaxiService taxiService;

    @Operation(summary = "택시비/경로 조회", description = "출발지-도착지 경도,위도를 받아 카카오 길찾기 응답을 그대로 반환합니다.")
    @PostMapping(value = "/api/directions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDirections(@RequestBody DirectionsRequest request) {
        return ResponseEntity.ok(taxiService.getDirections(request));
    }
}
