package backend.agerdon.domain.favorite.entity;

import backend.agerdon.domain.member.entity.Member;
import backend.agerdon.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "favorite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long id;

    // 사용자(Member)와의 N:1 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 장소(Place)와의 N:1 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Builder
    public Favorite(Member member, Place place) {
        this.member = member;
        this.place = place;
    }

    // 정적 팩토리 메서드 (생성 편의용)
    public static Favorite createFavorite(Member member, Place place) {
        return Favorite.builder()
                .member(member)
                .place(place)
                .build();
    }
}
