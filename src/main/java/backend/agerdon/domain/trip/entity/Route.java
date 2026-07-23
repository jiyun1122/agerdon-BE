package backend.agerdon.domain.trip.entity;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "routes",
        indexes = @Index(name = "idx_route_trip", columnList = "trip_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String guide;

    @Column(name = "total_minutes", nullable = false)
    private int totalMinutes;

    @Column(name = "walk_minutes", nullable = false)
    private int walkMinutes;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private int fare;

    @Column(nullable = false)
    private boolean recommended;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteType type;

    @Builder
    public Route(
            String name,
            String guide,
            int totalMinutes,
            int walkMinutes,
            LocalDateTime scheduledAt,
            int fare,
            boolean recommended,
            RouteType type
    ) {
        this.name = name;
        this.guide = guide;
        this.totalMinutes = totalMinutes;
        this.walkMinutes = walkMinutes;
        this.scheduledAt = scheduledAt;
        this.fare = fare;
        this.recommended = recommended;
        this.type = type;
    }

    void assignTrip(Trip trip) {
        this.trip = trip;
    }

    public LocalDateTime getDepartureDeadline() {
        return scheduledAt == null ? null : scheduledAt.minusMinutes(walkMinutes);
    }
}
