package backend.agerdon.domain.member.dto.response;

import backend.agerdon.domain.member.entity.FavoriteType;
import backend.agerdon.domain.member.entity.MemberFavoritePlace;
import backend.agerdon.domain.place.dto.PlaceResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FavoritePlaceResponse {

    @Schema(description = "즐겨찾기 ID", example = "1")
    private Long id;

    @Schema(description = "즐겨찾기한 장소 정보")
    private PlaceResponse place;

    @Schema(description = "즐겨찾기 타입", example = "HOME")
    private FavoriteType favoriteType;

    @Schema(description = "즐겨찾기 별칭", example = "우리집")
    private String alias;

    public static FavoritePlaceResponse from(MemberFavoritePlace favorite) {
        return FavoritePlaceResponse.builder()
                .id(favorite.getId())
                .place(PlaceResponse.from(favorite.getPlace()))
                .favoriteType(favorite.getFavoriteType())
                .alias(favorite.getAlias())
                .build();
    }
}
