package backend.agerdon.domain.member.service;

import backend.agerdon.domain.member.dto.request.FavoritePlaceRequest;
import backend.agerdon.domain.member.dto.request.FavoritePlaceUpdateRequest;
import backend.agerdon.domain.member.dto.response.FavoritePlaceResponse;
import backend.agerdon.domain.member.entity.FavoriteType;
import backend.agerdon.domain.member.entity.Member;
import backend.agerdon.domain.member.entity.MemberFavoritePlace;
import backend.agerdon.domain.member.repository.MemberFavoritePlaceRepository;
import backend.agerdon.domain.member.repository.MemberRepository;
import backend.agerdon.domain.place.entity.Place;
import backend.agerdon.domain.place.repository.PlaceRepository;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberFavoritePlaceService {

    private final MemberFavoritePlaceRepository memberFavoritePlaceRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public FavoritePlaceResponse addFavorite(Long memberId, FavoritePlaceRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Place place = placeRepository.findById(request.getPlaceId())
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));

        FavoriteType favoriteType = (request.getFavoriteType() != null) ? request.getFavoriteType() : FavoriteType.GENERAL;

        // 이미 즐겨찾기한 장소라면 새로 만들지 않고 갱신 (멱등 처리)
        MemberFavoritePlace favorite = memberFavoritePlaceRepository
                .findByMemberIdAndPlaceId(memberId, request.getPlaceId())
                .orElse(null);

        if (favoriteType == FavoriteType.HOME) {
            demoteExistingHome(memberId, favorite);
        }

        if (favorite != null) {
            favorite.update(favoriteType, request.getAlias());
        } else {
            favorite = MemberFavoritePlace.builder()
                    .member(member)
                    .place(place)
                    .favoriteType(favoriteType)
                    .alias(request.getAlias())
                    .build();
            memberFavoritePlaceRepository.save(favorite);
        }

        return FavoritePlaceResponse.from(favorite);
    }

    public List<FavoritePlaceResponse> listFavorites(Long memberId, FavoriteType typeFilter) {
        List<MemberFavoritePlace> favorites = (typeFilter != null)
                ? memberFavoritePlaceRepository.findByMemberIdAndFavoriteType(memberId, typeFilter)
                : memberFavoritePlaceRepository.findByMemberId(memberId);

        return favorites.stream()
                .map(FavoritePlaceResponse::from)
                .toList();
    }

    public FavoritePlaceResponse getFavorite(Long memberId, Long favoriteId) {
        return FavoritePlaceResponse.from(findOwnedFavorite(memberId, favoriteId));
    }

    public FavoritePlaceResponse getHomeAddress(Long memberId) {
        return memberFavoritePlaceRepository.findByMemberIdAndFavoriteType(memberId, FavoriteType.HOME)
                .stream()
                .findFirst()
                .map(FavoritePlaceResponse::from)
                .orElse(null);
    }

    @Transactional
    public FavoritePlaceResponse updateFavorite(Long memberId, Long favoriteId, FavoritePlaceUpdateRequest request) {
        MemberFavoritePlace favorite = findOwnedFavorite(memberId, favoriteId);

        if (request.getFavoriteType() == FavoriteType.HOME) {
            demoteExistingHome(memberId, favorite);
        }

        favorite.update(request.getFavoriteType(), request.getAlias());
        return FavoritePlaceResponse.from(favorite);
    }

    @Transactional
    public void deleteFavorite(Long memberId, Long favoriteId) {
        MemberFavoritePlace favorite = findOwnedFavorite(memberId, favoriteId);
        memberFavoritePlaceRepository.delete(favorite);
    }

    private MemberFavoritePlace findOwnedFavorite(Long memberId, Long favoriteId) {
        return memberFavoritePlaceRepository.findByIdAndMemberId(favoriteId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.FAVORITE_PLACE_NOT_FOUND));
    }

    // 회원당 HOME은 최대 1개만 존재하도록, 새 HOME 등록/변경 시 기존 HOME은 GENERAL로 강등
    private void demoteExistingHome(Long memberId, MemberFavoritePlace exclude) {
        memberFavoritePlaceRepository.findByMemberIdAndFavoriteType(memberId, FavoriteType.HOME).stream()
                .filter(existing -> exclude == null || !existing.getId().equals(exclude.getId()))
                .forEach(MemberFavoritePlace::demoteToGeneral);
    }
}
