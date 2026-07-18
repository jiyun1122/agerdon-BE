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

    @Column(nullable = false, unique = true, length = 50)
    private String email;
    // 우리 서비스에 같은 이메일로 가입한 사람이 없도록 해주기 위함
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Builder
    public Member(String email, String password, String nickname){
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

}
