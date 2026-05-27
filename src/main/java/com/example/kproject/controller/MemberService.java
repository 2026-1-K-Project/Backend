package com.example.kproject.controller; // 또는 적절한 서비스 패키지 지정

import com.example.kproject.domain.Member;
import com.example.kproject.domain.MemberRepository;
import com.example.kproject.dto.LoginRequest;
import com.example.kproject.dto.SignUpRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Long signUp(SignUpRequest request) {
        memberRepository.findByEmail(request.email())
                .ifPresent(m -> {
                    throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
                });

        Member member = new Member(request.email(), request.password(), request.name());
        return memberRepository.save(member).getId();
    }

    public String login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!member.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return member.getEmail();
    }
}