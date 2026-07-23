package backend.agerdon.domain.place.dto;

import backend.agerdon.domain.place.entity.Place;
import backend.agerdon.domain.place.entity.PlaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlaceResponse {

    private Long id;
    private String name;
    private String addr;
    private PlaceType type;

    // Entity -> DTO 변환 정적 팩토리 메서드
    public static PlaceResponse from(Place place) {
        return PlaceResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .addr(place.getAddr())
                .type(place.getType())
                .build();
    }
}
