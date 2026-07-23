package backend.agerdon.domain.favorite.repository;

import backend.agerdon.domain.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 특정 회원의 즐겨찾기 목록 전체 조회 (회원 ID 기준)
    List<Favorite> findByMemberId(Long memberId);

    // 특정 회원이 해당 장소를 이미 즐겨찾기했는지 여부 확인 (중복 등록 방지용)
    boolean existsByMemberIdAndPlaceId(Long memberId, Long placeId);

    // 특정 회원의 특정 장소 즐겨찾기 단건 조회 (즐겨찾기 취소/삭제용)
    Optional<Favorite> findByMemberIdAndPlaceId(Long memberId, Long placeId);
}
