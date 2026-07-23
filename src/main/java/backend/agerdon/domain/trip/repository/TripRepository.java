package backend.agerdon.domain.trip.repository;

import backend.agerdon.domain.trip.entity.Trip;
import backend.agerdon.domain.trip.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    boolean existsByMemberIdAndStatusIsNull(Long memberId);

    Optional<Trip> findFirstByMemberIdAndStatusIsNullOrderByStartedAtDesc(Long memberId);

    Optional<Trip> findByIdAndMemberId(Long tripId, Long memberId);

    long countByMemberIdAndStatus(Long memberId, TripStatus status);
}
