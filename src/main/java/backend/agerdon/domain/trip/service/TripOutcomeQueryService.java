package backend.agerdon.domain.trip.service;

import backend.agerdon.domain.trip.dto.response.TripOutcomeSummary;
import backend.agerdon.domain.trip.entity.TripStatus;
import backend.agerdon.domain.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripOutcomeQueryService {

    private final TripRepository tripRepository;

    public TripOutcomeSummary getSummary(Long memberId) {
        long successCount = tripRepository.countByMemberIdAndStatus(memberId, TripStatus.SUCCESS);
        long missedCount = tripRepository.countByMemberIdAndStatus(memberId, TripStatus.MISSED);
        return new TripOutcomeSummary(successCount, missedCount);
    }
}
