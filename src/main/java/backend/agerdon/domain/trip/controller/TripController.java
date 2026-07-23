package backend.agerdon.domain.trip.controller;

import backend.agerdon.domain.trip.dto.request.CreateTripRequest;
import backend.agerdon.domain.trip.dto.request.TripResultRequest;
import backend.agerdon.domain.trip.dto.response.TripDetailResponse;
import backend.agerdon.domain.trip.service.TripService;
import backend.agerdon.global.response.ApiResponse;
import backend.agerdon.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Trip", description = "막차 여정, 경로 추천 및 탑승 결과 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService tripService;

    @Operation(summary = "여정 시작", description = "경로 후보를 생성하고 막차 타이머를 시작합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<TripDetailResponse>> createTrip(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTripRequest request
    ) {
        TripDetailResponse response = tripService.createTrip(currentMemberId(userDetails), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("여정이 시작되었습니다.", response));
    }

    @Operation(summary = "현재 여정 조회", description = "결과를 입력하지 않은 현재 여정을 조회합니다.")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<TripDetailResponse>> getCurrentTrip(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        TripDetailResponse response = tripService.getCurrentTrip(currentMemberId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "여정 상세 조회")
    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripDetailResponse>> getTrip(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tripId
    ) {
        TripDetailResponse response = tripService.getTrip(currentMemberId(userDetails), tripId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "탑승 결과 입력", description = "SUCCESS 또는 MISSED 결과를 한 번 입력합니다.")
    @PatchMapping("/{tripId}/result")
    public ResponseEntity<ApiResponse<TripDetailResponse>> submitResult(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tripId,
            @Valid @RequestBody TripResultRequest request
    ) {
        TripDetailResponse response = tripService.submitResult(
                currentMemberId(userDetails),
                tripId,
                request.getStatus()
        );
        return ResponseEntity.ok(ApiResponse.success("탑승 결과가 저장되었습니다.", response));
    }

    @Operation(summary = "여정 취소", description = "결과를 입력하지 않은 여정을 삭제합니다.")
    @DeleteMapping("/{tripId}")
    public ResponseEntity<ApiResponse<Void>> cancelTrip(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tripId
    ) {
        tripService.cancelTrip(currentMemberId(userDetails), tripId);
        return ResponseEntity.ok(ApiResponse.success("여정이 취소되었습니다.", null));
    }

    private Long currentMemberId(CustomUserDetails userDetails) {
        return userDetails.getMember().getId();
    }
}
