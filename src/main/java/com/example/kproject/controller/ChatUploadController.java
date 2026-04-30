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

@Tag(name = "채팅 업로드", description = "카카오톡 txt 업로드 및 분석 API")
@RestController
@RequestMapping("/api/chat/upload")
public class ChatUploadController {

    private final KakaoChatUploadReportService kakaoChatUploadReportService;

    public ChatUploadController(KakaoChatUploadReportService kakaoChatUploadReportService) {
        this.kakaoChatUploadReportService = kakaoChatUploadReportService;
    }

    @Operation(
            summary = "카카오톡 txt 파일 업로드 후 바로 분석",
            description = "카카오톡 내보내기 txt 파일을 업로드하면 파싱과 분석을 거쳐 종합 리포트를 즉시 반환합니다."
    )
    @PostMapping(value = "/kakao-txt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReportResponse uploadKakaoTxt(
            @Parameter(description = "카카오톡에서 내보낸 txt 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "분석 카테고리(선택). 예: 썸/연애, 친구/우정")
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "분석 대상 이름(선택)")
            @RequestParam(value = "targetName", required = false) String targetName
    ) {
        return kakaoChatUploadReportService.generateFromKakaoTxt(file, category, targetName);
    }

    @Operation(
            summary = "카카오톡 txt 파일 업로드 후 파싱 결과만 조회",
            description = "리포트 생성 없이 파싱된 메타데이터와 메시지 구조만 반환하는 디버그용 엔드포인트입니다."
    )
    @PostMapping(value = "/kakao-txt/parsed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KakaoChatUploadResponse uploadKakaoTxtParsed(
            @Parameter(description = "카카오톡에서 내보낸 txt 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "분석 카테고리(선택). 예: 썸/연애, 친구/우정")
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "분석 대상 이름(선택)")
            @RequestParam(value = "targetName", required = false) String targetName
    ) {
        return kakaoChatUploadReportService.parseOnly(file, category, targetName);
    }
}
