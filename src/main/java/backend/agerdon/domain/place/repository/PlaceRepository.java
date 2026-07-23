package backend.agerdon.domain.place.repository;

import backend.agerdon.domain.place.entity.Place;
import backend.agerdon.domain.place.entity.PlaceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    // 1. 이름 키워드 검색 (검색어만 입력 시)
    List<Place> findByNameContaining(String keyword);

    // 2. 장소 구분(출발지/도착지) 검색 (검색어 없이 출발지/도착지 탭 클릭 시)
    List<Place> findByType(PlaceType type);

    // 3. 이름 키워드 및 장소 구분 검색 (탭 선택하고 검색어까지 입력 시)
    List<Place> findByNameContainingAndType(String keyword, PlaceType type);
}
