package backend.agerdon.global.security.auth.service;


import backend.agerdon.domain.member.entity.Member;
import backend.agerdon.domain.member.repository.MemberRepository;
import backend.agerdon.global.exception.CustomException;
import backend.agerdon.global.exception.ErrorCode;
import backend.agerdon.global.security.auth.dto.LoginRequest;
import backend.agerdon.global.security.auth.dto.LoginResponse;
import backend.agerdon.global.security.auth.dto.SignUpRequest;
import backend.agerdon.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void signUp(SignUpRequest request){
        // 이메일 중복 검증
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        // 2. 패스워드 암호화 및 빌드
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = Member.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .build();
        memberRepository.save(member);
    }
    public LoginResponse login(LoginRequest request){
        //회원 정보 조회
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        //암호화된 비밀번호 매칭 검증
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        //JWT 토큰 생성
        String accessToken = jwtTokenProvider.createToken(member.getEmail());
        return LoginResponse.of(accessToken);
    }
}
