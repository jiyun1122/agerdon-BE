package backend.agerdon.domain.member.service;

import backend.agerdon.domain.member.dto.response.LastTrainStatsResponse;
import backend.agerdon.domain.member.entity.Member;
import backend.agerdon.domain.member.repository.MemberRepository;
import backend.agerdon.domain.trip.dto.response.TripOutcomeSummary;
import backend.agerdon.domain.trip.service.TripOutcomeQueryService;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberStatsService {

    private final MemberRepository memberRepository;
    private final TripOutcomeQueryService tripOutcomeQueryService;

    public LastTrainStatsResponse getLastTrainStats(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        TripOutcomeSummary summary = tripOutcomeQueryService.getSummary(memberId);
        long totalCount = summary.totalCount();
        double successRate = (totalCount == 0) ? 0.0 : (summary.successCount() * 100.0) / totalCount;

        long totalMembers = memberRepository.count();
        long membersAhead = memberRepository.countByScoreGreaterThan(member.getScore());
        // 동점자는 모두 최상위 그룹으로 취급하는 단순화된 상위 % 계산 (회원이 1명뿐이면 0%)
        double topPercent = (totalMembers == 0) ? 0.0 : (membersAhead * 100.0) / totalMembers;

        return LastTrainStatsResponse.builder()
                .score(member.getScore())
                .successCount(summary.successCount())
                .missedCount(summary.missedCount())
                .totalCount(totalCount)
                .successRate(successRate)
                .topPercent(topPercent)
                .build();
    }
}
