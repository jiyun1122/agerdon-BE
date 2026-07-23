package backend.agerdon.global.config;

import backend.agerdon.global.exception.ErrorCode;
import backend.agerdon.global.exception.ErrorResponse;
import backend.agerdon.global.security.jwt.JwtAuthenticationFilter;
import backend.agerdon.global.security.jwt.JwtTokenProvider;
import backend.agerdon.global.security.principal.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtTokenProvider jwtTokenProvider,
            CustomUserDetailsService customUserDetailsService,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 방어 비활성화 (REST API 환경)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. ★ CORS 상세 세팅 연동 (아래 생성한 Bean 메서드 조인) ★
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 무상태(Stateless) 세션 정책 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(response, ErrorCode.UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, ErrorCode.ACCESS_DENIED)
                        )
                )

                // 4. API 인증 및 인가 가드레일 설정
                .authorizeHttpRequests(auth -> auth
                        // 회원가입, 로그인은 프리패스
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // 스웨거 UI 및 OpenAPI 명세서 리소스 완전 개방
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Docker healthcheck / 모니터링용 헬스 엔드포인트 개방
                        .requestMatchers("/actuator/health").permitAll()
                        // 그 외 나머지 도메인 API는 무조건 JWT 검증 필요
                        .anyRequest().authenticated()
                )

                // 5. JWT 커스텀 인증 필터를 아이디/비번 검증 필터 앞에 배치
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(errorCode));
    }

    // 운영 배포 도메인과 로컬 개발 서버 CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://mutsasession7.store",
                "https://mutsasession7.store",
                "http://www.mutsasession7.store",
                "https://www.mutsasession7.store",
                "https://2026-agadon-frontend-two.vercel.app",
                "https://2026-agadon-frontend.vercel.app"
        ));
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // JWT 헤더나 쿠키 인증 교환 필수 옵션

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
