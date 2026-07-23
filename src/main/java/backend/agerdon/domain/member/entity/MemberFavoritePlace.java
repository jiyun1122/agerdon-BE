package backend.agerdon.domain.member.entity;

import backend.agerdon.domain.place.entity.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "member_favorite_places",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_favorite_place",
                columnNames = {"member_id", "place_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberFavoritePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_favorite_place_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Enumerated(EnumType.STRING)
    @Column(name = "favorite_type", nullable = false, length = 20)
    private FavoriteType favoriteType;

    @Column(length = 30)
    private String alias;

    @Builder
    public MemberFavoritePlace(Member member, Place place, FavoriteType favoriteType, String alias) {
        this.member = member;
        this.place = place;
        this.favoriteType = (favoriteType != null) ? favoriteType : FavoriteType.GENERAL;
        this.alias = alias;
    }

    // 즐겨찾기 정보(별칭, 타입) 수정용 메서드
    public void update(FavoriteType favoriteType, String alias) {
        if (favoriteType != null) {
            this.favoriteType = favoriteType;
        }
        this.alias = alias;
    }

    // 새 HOME 등록 시 기존 HOME을 일반 즐겨찾기로 강등시키기 위한 메서드
    public void demoteToGeneral() {
        this.favoriteType = FavoriteType.GENERAL;
    }
}
