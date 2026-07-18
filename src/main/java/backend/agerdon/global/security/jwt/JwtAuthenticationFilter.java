package backend.agerdon.global.security.jwt;

import backend.agerdon.global.security.principal.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    // 시큐리티 설정에서 주입해주기 위해 생성자 정의
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService customUserDetailsService){
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // HTTP 요청 헤더에서 JWT 토큰을 추출
        String token = resolveToken(request);
        // 토큰이 존재하고, 위조나 만료 없이 유효하다면 인증 진행
        if(token != null && jwtTokenProvider.validateToken(token)){
            // 토큰에서 식별자 꺼내기
            String email = jwtTokenProvider.getEmail(token);
            // 꺼낸 이메일로 정보 조회 & 시큐리티 규격으로 감쌈
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
            // 시큐리티 전용 인증 도장 생성
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
            // 요청에 대한 웹 정보 인증 객체에 생성
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 시큐리티 보관함에 인증 도장 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request,response);
    }
    private String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }
}
