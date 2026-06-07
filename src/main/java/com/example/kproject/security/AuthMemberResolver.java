package com.example.kproject.security;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthMemberResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthTokenService authTokenService;

    public AuthMemberResolver(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    public Long resolveMemberId(String authorizationHeader, Long fallbackMemberId) {
        Long tokenMemberId = authTokenService.verifyAndExtractMemberId(extractBearerToken(authorizationHeader));
        return tokenMemberId == null ? fallbackMemberId : tokenMemberId;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }
}
