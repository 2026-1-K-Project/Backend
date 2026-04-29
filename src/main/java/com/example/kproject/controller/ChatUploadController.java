package com.example.kproject.controller;

import com.example.kproject.dto.KakaoChatUploadResponse;
import com.example.kproject.service.KakaoChatFileParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Chat Upload", description = "KakaoTalk txt upload parsing API")
@RestController
@RequestMapping("/api/chat/upload")
public class ChatUploadController {

    private final KakaoChatFileParserService kakaoChatFileParserService;

    public ChatUploadController(KakaoChatFileParserService kakaoChatFileParserService) {
        this.kakaoChatFileParserService = kakaoChatFileParserService;
    }

    @Operation(
            summary = "Upload a KakaoTalk txt export",
            description = "Parses KakaoTalk exported txt files into message data and analysis-ready plain text."
    )
    @PostMapping(value = "/kakao-txt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KakaoChatUploadResponse uploadKakaoTxt(
            @Parameter(description = "KakaoTalk exported txt file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional upload category")
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Optional analysis target name")
            @RequestParam(value = "targetName", required = false) String targetName
    ) {
        KakaoChatUploadResponse parsedResponse = kakaoChatFileParserService.parse(file);
        return parsedResponse.withMeta(parsedResponse.meta().withCategoryAndTargetName(category, targetName));
    }
}
