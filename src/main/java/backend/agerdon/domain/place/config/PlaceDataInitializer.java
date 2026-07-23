package backend.agerdon.domain.place.config;

import backend.agerdon.domain.place.entity.Place;
import backend.agerdon.domain.place.entity.PlaceType;
import backend.agerdon.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PlaceDataInitializer implements ApplicationRunner {

    private final PlaceRepository placeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (placeRepository.count() > 0) {
            return;
        }

        placeRepository.saveAll(List.of(
                place("상수역", "서울특별시 마포구 독막로 지하 85"),
                place("강남역", "서울특별시 강남구 강남대로 지하 396"),
                place("건대입구역", "서울특별시 광진구 능동로 110"),
                place("신촌역", "서울특별시 서대문구 신촌로 지하 124"),
                place("잠실역", "서울특별시 송파구 올림픽로 지하 265"),
                place("서울역", "서울특별시 용산구 한강대로 405"),
                place("합정역", "서울특별시 마포구 양화로 지하 55"),
                place("신림역", "서울특별시 관악구 남부순환로 지하 1614"),
                place("성수역", "서울특별시 성동구 아차산로 113"),
                place("여의도역", "서울특별시 영등포구 의사당대로 지하 101")
        ));
    }

    private Place place(String name, String address) {
        return Place.builder()
                .name(name)
                .addr(address)
                .type(PlaceType.DEST)
                .build();
    }
}
