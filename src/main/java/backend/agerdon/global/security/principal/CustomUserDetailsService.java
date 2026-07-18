package backend.agerdon.global.security.principal;

import backend.agerdon.domain.member.entity.Member;
import backend.agerdon.domain.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService{

    private final MemberRepository memberRepository;

    public CustomUserDetailsService(MemberRepository memberRepository){
        this.memberRepository = memberRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 가진 회원을 찾을 수 없습니다: " + email));
        return new CustomUserDetails(member); // 찾은 엔티티를 시큐리티 규격으로 감싸서 리턴
    }

}
