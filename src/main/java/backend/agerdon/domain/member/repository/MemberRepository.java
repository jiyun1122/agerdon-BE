package backend.agerdon.domain.member.repository;


import backend.agerdon.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// 추상적인 인터페이스에 의존
// 확장에는 열려있고 변경에는 닫혀있음
public interface MemberRepository extends JpaRepository<Member, Long> {
    //로그인 및 토큰 검증시 이메일로 회원 찾기
    Optional<Member> findByEmail(String email);
    // 회원가입시, 이메일 중복 가입 방지 검증 메서드
    boolean existsByEmail(String email);
    // 막차력 상위 n% 계산용: 나보다 점수가 높은 회원 수
    long countByScoreGreaterThan(Integer score);
}
