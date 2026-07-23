package backend.agerdon.domain.member.repository;

import backend.agerdon.domain.member.entity.FavoriteType;
import backend.agerdon.domain.member.entity.MemberFavoritePlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberFavoritePlaceRepository extends JpaRepository<MemberFavoritePlace, Long> {

    List<MemberFavoritePlace> findByMemberId(Long memberId);

    List<MemberFavoritePlace> findByMemberIdAndFavoriteType(Long memberId, FavoriteType favoriteType);

    Optional<MemberFavoritePlace> findByIdAndMemberId(Long id, Long memberId);

    Optional<MemberFavoritePlace> findByMemberIdAndPlaceId(Long memberId, Long placeId);
}
