package backend.agerdon.domain.trip.service;

import backend.agerdon.domain.member.entity.Member;
import backend.agerdon.domain.member.repository.MemberRepository;
import backend.agerdon.domain.trip.dto.request.CreateTripRequest;
import backend.agerdon.domain.trip.dto.request.LocationRequest;
import backend.agerdon.domain.trip.dto.response.TripDetailResponse;
import backend.agerdon.domain.trip.dto.response.TripOutcomeSummary;
import backend.agerdon.domain.trip.entity.Route;
import backend.agerdon.domain.trip.entity.RouteType;
import backend.agerdon.domain.trip.entity.TimerState;
import backend.agerdon.domain.trip.entity.Trip;
import backend.agerdon.domain.trip.entity.TripStatus;
import backend.agerdon.domain.trip.repository.TripRepository;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class TripServiceIntegrationTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private TripOutcomeQueryService tripOutcomeQueryService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private Clock clock;

    @Test
    void createsTripWithNullResultAndOneRecommendedRoute() {
        Member member = saveMember("create@example.com");

        TripDetailResponse response = tripService.createTrip(member.getId(), createRequest());

        assertNotNull(response.getTripId());
        assertNull(response.getStatus());
        assertEquals(4, response.getRoutes().size());
        assertEquals(1, response.getRoutes().stream().filter(route -> route.isRecommended()).count());
        assertEquals(TimerState.RUNNING, response.getTimer().getState());
        assertNotNull(tripService.getCurrentTrip(member.getId()));
    }

    @Test
    void rejectsSecondTripUntilCurrentTripIsResolved() {
        Member member = saveMember("duplicate@example.com");
        tripService.createTrip(member.getId(), createRequest());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> tripService.createTrip(member.getId(), createRequest())
        );

        assertEquals(ErrorCode.TRIP_IN_PROGRESS, exception.getErrorCode());
    }

    @Test
    void recordsUserResultOnceAndProvidesOutcomeSummary() {
        Member member = saveMember("result@example.com");
        TripDetailResponse created = tripService.createTrip(member.getId(), createRequest());

        TripDetailResponse completed = tripService.submitResult(
                member.getId(),
                created.getTripId(),
                TripStatus.SUCCESS
        );
        TripOutcomeSummary summary = tripOutcomeQueryService.getSummary(member.getId());

        assertEquals(TripStatus.SUCCESS, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertNull(tripService.getCurrentTrip(member.getId()));
        assertEquals(1, summary.successCount());
        assertEquals(0, summary.missedCount());
        assertEquals(1, summary.totalCount());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> tripService.submitResult(
                        member.getId(),
                        created.getTripId(),
                        TripStatus.MISSED
                )
        );
        assertEquals(ErrorCode.TRIP_RESULT_ALREADY_SUBMITTED, exception.getErrorCode());
    }

    @Test
    void expiredTimerDoesNotAutomaticallyRecordMissedResult() {
        Member member = saveMember("expired@example.com");
        LocalDateTime now = LocalDateTime.now(clock);
        Trip trip = buildTrip(member, now.minusHours(1));
        trip.addRoute(Route.builder()
                .name("지난 막차")
                .guide("지난 경로")
                .totalMinutes(50)
                .walkMinutes(10)
                .scheduledAt(now.minusMinutes(20))
                .fare(1_400)
                .recommended(true)
                .type(RouteType.SUBWAY)
                .build());
        tripRepository.saveAndFlush(trip);

        TripDetailResponse response = tripService.getTrip(member.getId(), trip.getId());

        assertEquals(TimerState.EXPIRED, response.getTimer().getState());
        assertTrue(response.getTimer().getRemainingSeconds() < 0);
        assertNull(response.getStatus());
        assertNull(tripRepository.findById(trip.getId()).orElseThrow().getStatus());
    }

    @Test
    void hidesOtherMembersTripAndAllowsOwnerToCancelUnresolvedTrip() {
        Member owner = saveMember("owner@example.com");
        Member other = saveMember("other@example.com");
        TripDetailResponse created = tripService.createTrip(owner.getId(), createRequest());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> tripService.getTrip(other.getId(), created.getTripId())
        );
        assertEquals(ErrorCode.TRIP_NOT_FOUND, exception.getErrorCode());

        tripService.cancelTrip(owner.getId(), created.getTripId());

        assertFalse(tripRepository.existsById(created.getTripId()));
        assertNull(tripService.getCurrentTrip(owner.getId()));
    }

    private Member saveMember(String email) {
        return memberRepository.save(Member.builder()
                .email(email)
                .password("encoded-password")
                .nickname("tester")
                .build());
    }

    private CreateTripRequest createRequest() {
        return new CreateTripRequest(
                new LocationRequest(
                        "홍익대학교 T동",
                        "서울특별시 마포구",
                        new BigDecimal("37.5500"),
                        new BigDecimal("126.9200")
                ),
                new LocationRequest(
                        "집",
                        "서울특별시",
                        new BigDecimal("37.5000"),
                        new BigDecimal("127.0000")
                )
        );
    }

    private Trip buildTrip(Member member, LocalDateTime startedAt) {
        return Trip.builder()
                .member(member)
                .originName("홍익대학교")
                .originAddress("서울시 마포구")
                .originLatitude(new BigDecimal("37.5500"))
                .originLongitude(new BigDecimal("126.9200"))
                .destinationName("집")
                .destinationAddress("서울시")
                .destinationLatitude(new BigDecimal("37.5000"))
                .destinationLongitude(new BigDecimal("127.0000"))
                .startedAt(startedAt)
                .build();
    }
}
