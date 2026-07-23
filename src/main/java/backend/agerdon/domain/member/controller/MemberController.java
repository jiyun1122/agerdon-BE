package backend.agerdon.domain.member.controller;


import backend.agerdon.global.security.principal.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {
    @GetMapping("/me")
    public ResponseEntity<String> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // userDetails가 null일 경우를 대비한 안전장치 추가
        if (userDetails == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        // JwtAuthenticationFilter가 채워준 인프라를 바탕으로 유저 정보를 꺼냅니다.
        String currentEmail = userDetails.getUsername();

        return ResponseEntity.ok("현재 로그인한 유저의 이메일은 " + currentEmail + " 입니다.");
    }
}
