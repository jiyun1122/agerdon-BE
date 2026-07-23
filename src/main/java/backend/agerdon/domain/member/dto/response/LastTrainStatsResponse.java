package backend.agerdon.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LastTrainStatsResponse {

    @Schema(description = "막차력 점수 (0~100)", example = "92")
    private Integer score;

    @Schema(description = "막차 탑승 성공 횟수", example = "18")
    private long successCount;

    @Schema(description = "막차 지각(놓침) 횟수", example = "2")
    private long missedCount;

    @Schema(description = "전체 여정 결과 입력 횟수", example = "20")
    private long totalCount;

    @Schema(description = "성공률 (%)", example = "90.0")
    private double successRate;

    @Schema(description = "전체 회원 중 상위 n% (막차력 기준, 낮을수록 상위)", example = "12.5")
    private double topPercent;
}
