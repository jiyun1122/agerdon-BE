package backend.agerdon.domain.place.controller;

import backend.agerdon.domain.place.dto.PlaceResponse;
import backend.agerdon.domain.place.entity.PlaceType;
import backend.agerdon.domain.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    // 우리 DB 안에서 장소 검색 - 즐겨찾기 후보/빠른 선택용
    @GetMapping
    public List<PlaceResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PlaceType type
    ) {
        return placeService.search(keyword, type);
    }

    // 장소 단건 조회
    // GET /api/v1/places/1
    @GetMapping("/{placeId}")
    public PlaceResponse getPlace(@PathVariable Long placeId) {
        return placeService.getPlace(placeId);
    }
}
