package backend.agerdon.global.security.auth.controller;

import backend.agerdon.global.security.auth.dto.LoginRequest;
import backend.agerdon.global.security.auth.dto.LoginResponse;
import backend.agerdon.global.security.auth.dto.SignUpRequest;
import backend.agerdon.global.security.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
