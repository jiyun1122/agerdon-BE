package backend.agerdon.domain.trip.service;

import backend.agerdon.domain.member.entity.Member;
import backend.agerdon.domain.member.repository.MemberRepository;
import backend.agerdon.domain.trip.dto.request.CreateTripRequest;
import backend.agerdon.domain.trip.dto.request.LocationRequest;
import backend.agerdon.domain.trip.dto.response.TripDetailResponse;
import backend.agerdon.domain.trip.entity.Route;
import backend.agerdon.domain.trip.entity.Trip;
import backend.agerdon.domain.trip.entity.TripStatus;
import backend.agerdon.domain.trip.provider.RouteCandidate;
import backend.agerdon.domain.trip.provider.RouteCandidateProvider;
import backend.agerdon.domain.trip.provider.RouteSearchLocation;
import backend.agerdon.domain.trip.repository.TripRepository;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    // 막차력(Member.score) 자동 갱신 폭: 성공 시 가점, 지각(놓침) 시 감점
    private static final int SUCCESS_SCORE_DELTA = 2;
    private static final int MISSED_SCORE_DELTA = -5;

    private final TripRepository tripRepository;
    private final MemberRepository memberRepository;
    private final RouteCandidateProvider routeCandidateProvider;
    private final RouteRecommendationService routeRecommendationService;
    private final TripResponseMapper tripResponseMapper;
    private final Clock clock;

    @Transactional
    public TripDetailResponse createTrip(Long memberId, CreateTripRequest request) {
        if (tripRepository.existsByMemberIdAndStatusIsNull(memberId)) {
            throw new CustomException(ErrorCode.TRIP_IN_PROGRESS);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);

        RouteRecommendationResult recommendation = routeRecommendationService.recommend(
                routeCandidateProvider.findCandidates(
                        toSearchLocation(request.getOrigin()),
                        toSearchLocation(request.getDestination()),
                        now
                ),
                now
        );

        Trip trip = Trip.builder()
                .member(member)
                .originName(request.getOrigin().getName())
                .originAddress(request.getOrigin().getAddress())
                .originLatitude(request.getOrigin().getLatitude())
                .originLongitude(request.getOrigin().getLongitude())
                .destinationName(request.getDestination().getName())
                .destinationAddress(request.getDestination().getAddress())
                .destinationLatitude(request.getDestination().getLatitude())
                .destinationLongitude(request.getDestination().getLongitude())
                .startedAt(now)
                .build();

        for (RouteCandidate candidate : recommendation.candidates()) {
            trip.addRoute(toRoute(candidate, candidate.equals(recommendation.recommended())));
        }

        Trip savedTrip = tripRepository.saveAndFlush(trip);
        return tripResponseMapper.toResponse(savedTrip, now);
    }

    public TripDetailResponse getCurrentTrip(Long memberId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return tripRepository.findFirstByMemberIdAndStatusIsNullOrderByStartedAtDesc(memberId)
                .map(trip -> tripResponseMapper.toResponse(trip, now))
                .orElse(null);
    }

    public TripDetailResponse getTrip(Long memberId, Long tripId) {
        Trip trip = findOwnedTrip(memberId, tripId);
        return tripResponseMapper.toResponse(trip, LocalDateTime.now(clock));
    }

    @Transactional
    public TripDetailResponse submitResult(Long memberId, Long tripId, TripStatus status) {
        Trip trip = findOwnedTrip(memberId, tripId);
        if (trip.hasResult()) {
            throw new CustomException(ErrorCode.TRIP_RESULT_ALREADY_SUBMITTED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        trip.submitResult(status, now);
        trip.getMember().adjustScore(status == TripStatus.SUCCESS ? SUCCESS_SCORE_DELTA : MISSED_SCORE_DELTA);
        return tripResponseMapper.toResponse(trip, now);
    }

    @Transactional
    public void cancelTrip(Long memberId, Long tripId) {
        Trip trip = findOwnedTrip(memberId, tripId);
        if (trip.hasResult()) {
            throw new CustomException(ErrorCode.TRIP_RESULT_ALREADY_SUBMITTED);
        }
        tripRepository.delete(trip);
    }

    private Trip findOwnedTrip(Long memberId, Long tripId) {
        return tripRepository.findByIdAndMemberId(tripId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));
    }

    private RouteSearchLocation toSearchLocation(LocationRequest location) {
        return new RouteSearchLocation(
                location.getName(),
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude()
        );
    }

    private Route toRoute(RouteCandidate candidate, boolean recommended) {
        return Route.builder()
                .name(candidate.name())
                .guide(candidate.guide())
                .totalMinutes(candidate.totalMinutes())
                .walkMinutes(candidate.walkMinutes())
                .scheduledAt(candidate.scheduledAt())
                .fare(candidate.fare())
                .recommended(recommended)
                .type(candidate.type())
                .build();
    }
}
