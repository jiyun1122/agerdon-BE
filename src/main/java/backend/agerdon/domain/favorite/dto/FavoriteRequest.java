package backend.agerdon.domain.favorite.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FavoriteRequest {

    @NotNull(message = "장소 아이디는 필수입니다.")
    private Long placeId; // 즐겨찾기할 장소 ID
}
