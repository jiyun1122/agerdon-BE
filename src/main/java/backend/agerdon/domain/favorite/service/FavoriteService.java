package backend.agerdon.domain.favorite.service;

import backend.agerdon.domain.favorite.dto.FavoriteResponse;
import backend.agerdon.domain.favorite.entity.Favorite;
import backend.agerdon.domain.favorite.repository.FavoriteRepository;
import backend.agerdon.domain.member.entity.Member;
import backend.agerdon.domain.member.repository.MemberRepository;
import backend.agerdon.domain.place.entity.Place;
import backend.agerdon.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;

    // 1. 즐겨찾기 추가
    @Transactional
    public FavoriteResponse addFavorite(Long memberId, Long placeId) {
        // 1. 중복 체크 (DB 조회 최소화)
        if (favoriteRepository.existsByMemberIdAndPlaceId(memberId, placeId)) {
            throw new IllegalStateException("이미 즐겨찾기에 추가된 장소입니다.");
        }

        // 2. 회원 및 장소 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + memberId));
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소입니다. id=" + placeId));

        // 3. 즐겨찾기 저장
        Favorite favorite = Favorite.builder()
                .member(member)
                .place(place)
                .build();

        return FavoriteResponse.from(favoriteRepository.save(favorite));
    }

    // 2. 즐겨찾기 삭제
    @Transactional
    public void removeFavorite(Long memberId, Long favoriteId) {
        // 1. 즐겨찾기 존재 여부 확인
        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 즐겨찾기입니다. id=" + favoriteId));

        // 2. 본인의 즐겨찾기인지 검증
        if (!favorite.getMember().getId().equals(memberId)) {
            throw new IllegalStateException("본인의 즐겨찾기만 삭제할 수 있습니다.");
        }

        // 3. 삭제
        favoriteRepository.delete(favorite);
    }
}
