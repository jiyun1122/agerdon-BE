package backend.agerdon.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(length = 30)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String email;
    // 우리 서비스에 같은 이메일로 가입한 사람이 없도록 해주기 위함
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "score", nullable = false)
    private Integer score = 100;

    @Builder
    public Member(String email, String password, String nickname, String name, Integer score){
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.score = (score != null) ? score : 100;
    }

    // 막차지수(막차력) 변경용 메서드
    public void updateScore(Integer score) {
        this.score = score;
    }

}
