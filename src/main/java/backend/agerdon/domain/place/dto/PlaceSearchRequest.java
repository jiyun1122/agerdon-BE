package backend.agerdon.domain.place.dto;

import backend.agerdon.domain.place.entity.PlaceType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaceSearchRequest {

    private String keyword;    // 검색어 (선택)
    private PlaceType type;    // ORIGIN(출발지) 또는 DEST(목적지) (선택)
}
