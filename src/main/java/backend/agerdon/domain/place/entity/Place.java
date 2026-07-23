package backend.agerdon.domain.place.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "place")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String addr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceType type;

    @Builder
    public Place(String name, String addr, PlaceType type) {
        this.name = name;
        this.addr = addr;
        this.type = type;
    }

    // 정보(장소 이름, 주소, 구분) 수정용 메서드
    public void update(String name, String addr, PlaceType type) {
        this.name = name;
        this.addr = addr;
        this.type = type;
    }
}
