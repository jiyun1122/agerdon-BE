package backend.agerdon.domain.place.service;

import backend.agerdon.domain.place.dto.PlaceResponse;
import backend.agerdon.domain.place.entity.Place;
import backend.agerdon.domain.place.entity.PlaceType;
import backend.agerdon.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;

    // 장소 목록 조회 및 검색 로직
    public List<PlaceResponse> search(String keyword, PlaceType type) {
        List<Place> places;

        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (hasKeyword && type != null) { // 1. 검색어와 타입 모두 지정된 경우
            places = placeRepository.findByNameContainingAndType(keyword, type);
        } else if (hasKeyword) { // 2. 검색어만 들어온 경우
            places = placeRepository.findByNameContaining(keyword);
        } else if (type != null) { // 3. 타입만 들어온 경우
            places = placeRepository.findByType(type);
        } else { // 4. 검색어도 없고 타입도 없는 경우 -> 예외 발생
            throw new IllegalArgumentException("검색어(keyword) 또는 장소 타입(type) 중 하나는 필수 입력 사항입니다.");
        }

        return places.stream()
                .map(PlaceResponse::from)
                .toList();
    }

    // 장소 단건 조회 로직
    public PlaceResponse getPlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소입니다. id=" + placeId));
        return PlaceResponse.from(place);
    }
}
