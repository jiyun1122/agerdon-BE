package backend.agerdon.domain.member.controller;

import backend.agerdon.domain.member.dto.response.LastTrainStatsResponse;
import backend.agerdon.domain.member.service.MemberStatsService;
import backend.agerdon.global.response.ApiResponse;
import backend.agerdon.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member - Stats", description = "마이페이지 막차력/지각횟수/성공률 통계 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/me")
public class MemberStatsController {

    private final MemberStatsService memberStatsService;

    @Operation(
            summary = "막차력 통계 조회",
            description = "막차력 점수, 지각(놓침) 횟수, 성공률, 전체 회원 중 상위 n%(막차력 기준)를 조회합니다."
    )
    @GetMapping("/last-train-stats")
    public ResponseEntity<ApiResponse<LastTrainStatsResponse>> getLastTrainStats(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LastTrainStatsResponse response = memberStatsService.getLastTrainStats(currentMemberId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long currentMemberId(CustomUserDetails userDetails) {
        return userDetails.getMember().getId();
    }
}
