package com.example.kproject.chat;

import org.springframework.web.multipart.MultipartFile;

public record ChatRequest(
        String message,
        MultipartFile image
) {}