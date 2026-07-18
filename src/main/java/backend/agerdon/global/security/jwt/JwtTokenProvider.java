package backend.agerdon.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long validityInMilliseconds;

    // application.yml에서 비밀키/만료시간 주입
    public JwtTokenProvider(
            @Value("${spring.security.jwt.token.secret-key}") String secretKey,
            @Value("${spring.security.jwt.token.expire-length:3600000}") long validityInMilliseconds
    ){
        // HMAC-SHA 알고리즘에 적합한 SecretKey 객체로 변환
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
    }
    // 토큰 생성 로직
    public String createToken(String email){
        Claims claims = Jwts.claims()  // 토큰 문서에 아래 값을 밀어넣겠다
                .subject(email)
                .build(); // JSON 문자열로 반환
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }
    // 토큰에서 이메일 추출 로직
    public String getEmail(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    // 토큰 유효성 검증 로직
    public boolean validateToken(String token){
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }
}
