package com.example.kproject.controller;

import com.example.kproject.dto.upload.ChatUploadResponse;
import com.example.kproject.service.upload.ChatUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "업로드", description = "대화 원본 업로드 및 reportId 생성 API")
@RestController
@RequestMapping("/api/uploads")
public class ChatUploadPlatformController {

    private final ChatUploadService chatUploadService;

    public ChatUploadPlatformController(ChatUploadService chatUploadService) {
        this.chatUploadService = chatUploadService;
    }

    @Operation(
            summary = "대화 파일 업로드 및 리포트 생성",
            description = "카카오톡 txt 또는 채팅 캡처 이미지를 업로드해 raw text와 정형화 데이터를 저장하고 reportId를 발급합니다."
    )
    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatUploadResponse uploadChat(
            @Parameter(description = "카카오톡 txt 파일 또는 채팅 캡처 이미지", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "분석 카테고리")
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "분석 대상 이름")
            @RequestParam(value = "targetName", required = false) String targetName
    ) {
        return chatUploadService.upload(file, category, targetName);
    }
}
