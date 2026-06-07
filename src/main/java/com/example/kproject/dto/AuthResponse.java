package com.example.kproject.dto;

public record AuthResponse(
        Long memberId,
        String email,
        String name,
        String token,
        String message
) {
}
