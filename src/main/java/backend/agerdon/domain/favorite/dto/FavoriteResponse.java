package backend.agerdon.domain.favorite.dto;

import backend.agerdon.domain.favorite.entity.Favorite;
import backend.agerdon.domain.place.dto.PlaceResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FavoriteResponse {

    private Long favoriteId;
    private PlaceResponse place; // 장소 정보 (id, name, addr, type 모두 포함)

    // Entity -> DTO 변환 정적 팩토리 메서드
    public static FavoriteResponse from(Favorite favorite) {
        return FavoriteResponse.builder()
                .favoriteId(favorite.getId())
                .place(PlaceResponse.from(favorite.getPlace()))
                .build();
    }
}
