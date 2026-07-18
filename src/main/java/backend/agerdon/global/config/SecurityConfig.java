package backend.agerdon.global.config;

import backend.agerdon.global.security.jwt.JwtAuthenticationFilter;
import backend.agerdon.global.security.jwt.JwtTokenProvider;
import backend.agerdon.global.security.principal.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 방어 비활성화 (REST API는 세션을 쓰지 않으므로 끕니다)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS 기본 설정 (나중에 프론트엔드 연결 시 필요에 맞게 수정)
                .cors(cors -> cors.configure(http))

                // 3. 세션을 절대 생성하거나 사용하지 않음 (JWT 기반 Stateless 상태 유지)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. API별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 회원가입, 로그인은 토큰 없이 누구나 접근 가능하도록 허용
                        .requestMatchers("/api/v1/**").permitAll()
                        // 그 외 모든 API 요청은 인증(JWT)이 필요함
                        .anyRequest().authenticated()
                )

                // 5. 우리가 만든 JWT 필터를 시큐리티 기본 인증 필터(UsernamePasswordAuthenticationFilter) 앞에 끼워 넣음
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}