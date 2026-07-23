package backend.agerdon.domain.trip.entity;

import backend.agerdon.domain.member.entity.Member;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
        name = "trips",
        indexes = @Index(name = "idx_trip_member_status", columnList = "member_id,status")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "origin_name", nullable = false, length = 100)
    private String originName;

    @Column(name = "origin_address", nullable = false, length = 255)
    private String originAddress;

    @Column(name = "origin_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal originLatitude;

    @Column(name = "origin_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal originLongitude;

    @Column(name = "destination_name", nullable = false, length = 100)
    private String destinationName;

    @Column(name = "destination_address", nullable = false, length = 255)
    private String destinationAddress;

    @Column(name = "destination_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLatitude;

    @Column(name = "destination_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLongitude;

    /**
     * 사용자가 입력하는 탑승 결과다. 결과 입력 전에는 null을 유지한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TripStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Route> routes = new ArrayList<>();

    @Builder
    public Trip(
            Member member,
            String originName,
            String originAddress,
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            String destinationName,
            String destinationAddress,
            BigDecimal destinationLatitude,
            BigDecimal destinationLongitude,
            LocalDateTime startedAt
    ) {
        this.member = member;
        this.originName = originName;
        this.originAddress = originAddress;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationName = destinationName;
        this.destinationAddress = destinationAddress;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.startedAt = startedAt;
    }

    public List<Route> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    public void addRoute(Route route) {
        routes.add(route);
        route.assignTrip(this);
    }

    public boolean hasResult() {
        return status != null;
    }

    public void submitResult(TripStatus status, LocalDateTime completedAt) {
        if (status == null) {
            throw new IllegalArgumentException("탑승 결과는 필수입니다.");
        }
        if (this.status != null) {
            throw new IllegalStateException("이미 결과가 입력된 여정입니다.");
        }
        this.status = status;
        this.completedAt = completedAt;
    }
}
