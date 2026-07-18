package backend.agerdon.global.security.principal;

import backend.agerdon.domain.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

// UserDetails : 스프링 시큐리티가 인증을 완료한 유저를 관리할 때 쓰는 표준 인터페이스
public class CustomUserDetails implements UserDetails {
    private final Member member;

    public CustomUserDetails(Member member){
        this.member = member;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
    @Override
    public String getPassword() {
        return member.getPassword();
    }
    @Override
    public String getUsername() {
        return member.getEmail(); // 우리 시스템의 식별자인 이메일을 리턴
    }
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
    // 로그인한 유저 편하게 꺼내 쓰기
    public Member getMember() {
        return this.member;
    }
}
