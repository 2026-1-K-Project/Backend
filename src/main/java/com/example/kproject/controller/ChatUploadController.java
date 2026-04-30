package com.example.kproject.controller;

import com.example.kproject.dto.KakaoChatUploadResponse;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.service.report.KakaoChatUploadReportService;
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

    private final KakaoChatUploadReportService kakaoChatUploadReportService;

    public ChatUploadController(KakaoChatUploadReportService kakaoChatUploadReportService) {
        this.kakaoChatUploadReportService = kakaoChatUploadReportService;
    }

    @Operation(
            summary = "Upload a KakaoTalk txt export",
            description = "Parses a KakaoTalk exported txt file and immediately returns the analysis report."
    )
    @PostMapping(value = "/kakao-txt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReportResponse uploadKakaoTxt(
            @Parameter(description = "KakaoTalk exported txt file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional upload category")
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Optional analysis target name")
            @RequestParam(value = "targetName", required = false) String targetName
    ) {
        return kakaoChatUploadReportService.generateFromKakaoTxt(file, category, targetName);
    }

    @Operation(
            summary = "Upload a KakaoTalk txt export and return parsed data only",
            description = "Debug endpoint for returning the parsed KakaoTalk message structure without generating a report."
    )
    @PostMapping(value = "/kakao-txt/parsed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KakaoChatUploadResponse uploadKakaoTxtParsed(
            @Parameter(description = "KakaoTalk exported txt file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional upload category")
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Optional analysis target name")
            @RequestParam(value = "targetName", required = false) String targetName
    ) {
        return kakaoChatUploadReportService.parseOnly(file, category, targetName);
    }
}
