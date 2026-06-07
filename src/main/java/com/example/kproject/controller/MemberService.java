package com.example.kproject.controller;

import com.example.kproject.domain.Member;
import com.example.kproject.domain.MemberRepository;
import com.example.kproject.dto.AuthResponse;
import com.example.kproject.dto.LoginRequest;
import com.example.kproject.dto.SignUpRequest;
import com.example.kproject.security.AuthTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public MemberService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
    }

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        memberRepository.findByEmail(request.email())
                .ifPresent(member -> {
                    throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
                });

        Member member = new Member(request.email(), passwordEncoder.encode(request.password()), request.name());
        Member saved = memberRepository.save(member);
        return toResponse(saved, "회원가입이 완료되었습니다.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!passwordMatches(request.password(), member)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return toResponse(member, "로그인 성공");
    }

    public AuthResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        return toResponse(member, "회원 조회 성공");
    }

    private AuthResponse toResponse(Member member, String message) {
        return new AuthResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                authTokenService.issueToken(member),
                message
        );
    }

    private boolean passwordMatches(String rawPassword, Member member) {
        String storedPassword = member.getPassword();
        if (storedPassword != null && storedPassword.startsWith("$2") && passwordEncoder.matches(rawPassword, storedPassword)) {
            return true;
        }
        if (storedPassword != null && storedPassword.equals(rawPassword)) {
            member.changePassword(passwordEncoder.encode(rawPassword));
            return true;
        }
        return false;
    }
}
