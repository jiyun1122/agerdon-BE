package backend.agerdon.global.security.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String grantType; // 보통 "Bearer" 고정
    private String accessToken;

    public static LoginResponse of(String accessToken) {
        return new LoginResponse("Bearer", accessToken);
    }
}
