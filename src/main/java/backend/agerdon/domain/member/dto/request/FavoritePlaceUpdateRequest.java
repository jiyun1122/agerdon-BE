package backend.agerdon.domain.member.dto.request;

import backend.agerdon.domain.member.entity.FavoriteType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FavoritePlaceUpdateRequest {

    @Schema(description = "변경할 즐겨찾기 타입 (HOME으로 변경 시 기존 HOME은 GENERAL로 자동 강등)", example = "HOME")
    private FavoriteType favoriteType;

    @Schema(description = "변경할 즐겨찾기 별칭", example = "회사")
    private String alias;
}
