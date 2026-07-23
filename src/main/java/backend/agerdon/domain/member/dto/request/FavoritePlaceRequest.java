package backend.agerdon.domain.member.dto.request;

import backend.agerdon.domain.member.entity.FavoriteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FavoritePlaceRequest {

    @NotNull(message = "장소 ID는 필수입니다.")
    @Schema(description = "즐겨찾기할 장소 ID (GET /api/v1/places 로 검색한 결과의 id)", example = "1")
    private Long placeId;

    @Schema(description = "즐겨찾기 타입 (HOME: 내 주소, GENERAL: 일반 즐겨찾기). 생략 시 GENERAL", example = "GENERAL")
    private FavoriteType favoriteType;

    @Schema(description = "즐겨찾기 별칭 (선택)", example = "우리집")
    private String alias;
}
