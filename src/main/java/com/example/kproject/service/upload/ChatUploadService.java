package com.example.kproject.service.upload;

import com.example.kproject.domain.ChatSourceType;
import com.example.kproject.domain.ConversationReport;
import com.example.kproject.domain.KakaoChatParsedDocument;
import com.example.kproject.domain.NormalizedConversationResult;
import com.example.kproject.dto.upload.ChatUploadResponse;
import com.example.kproject.exception.ChatUploadException;
import com.example.kproject.service.KakaoChatFileParserService;
import com.example.kproject.service.normalize.AiConversationNormalizeService;
import com.example.kproject.service.normalize.ConversationNormalizeService;
import com.example.kproject.service.report.ReportStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
public class ChatUploadService {

    private static final String DEFAULT_CATEGORY = "일반 분석";

    private final KakaoChatFileParserService kakaoChatFileParserService;
    private final AiConversationNormalizeService aiConversationNormalizeService;
    private final ConversationNormalizeService conversationNormalizeService;
    private final ImageTextExtractionService imageTextExtractionService;
    private final ReportStorageService reportStorageService;

    public ChatUploadService(
            KakaoChatFileParserService kakaoChatFileParserService,
            AiConversationNormalizeService aiConversationNormalizeService,
            ConversationNormalizeService conversationNormalizeService,
            ImageTextExtractionService imageTextExtractionService,
            ReportStorageService reportStorageService
    ) {
        this.kakaoChatFileParserService = kakaoChatFileParserService;
        this.aiConversationNormalizeService = aiConversationNormalizeService;
        this.conversationNormalizeService = conversationNormalizeService;
        this.imageTextExtractionService = imageTextExtractionService;
        this.reportStorageService = reportStorageService;
    }

    public ChatUploadResponse upload(MultipartFile file, String category, String targetName) {
        if (file == null || file.isEmpty()) {
            throw new ChatUploadException("업로드할 대화 파일이 필요합니다.");
        }

        String resolvedCategory = StringUtils.hasText(category) ? category.trim() : DEFAULT_CATEGORY;
        ChatSourceType sourceType = detectSourceType(file);
        NormalizedConversationResult normalizedResult = switch (sourceType) {
            case TXT -> normalizeTxt(file, targetName);
            case IMAGE -> normalizeImage(file, targetName);
        };

        ConversationReport report = reportStorageService.createReport(
                resolvedCategory,
                sourceType,
                normalizedResult
        );

        return new ChatUploadResponse(
                report.getId(),
                report.getStatus(),
                normalizedResult.analysisMode(),
                "대화 업로드 및 정형화가 완료되었습니다.",
                normalizedResult.warning()
        );
    }

    private NormalizedConversationResult normalizeTxt(MultipartFile file, String targetName) {
        String rawText = readFileAsText(file);
        Optional<NormalizedConversationResult> aiResult =
                aiConversationNormalizeService.normalizeText(rawText, targetName);
        if (aiResult.isPresent()) {
            return aiResult.get();
        }

        KakaoChatParsedDocument parsedDocument = kakaoChatFileParserService.parseDocument(file);
        return conversationNormalizeService.normalize(parsedDocument, targetName);
    }

    private NormalizedConversationResult normalizeImage(MultipartFile file, String targetName) {
        Optional<NormalizedConversationResult> aiResult =
                aiConversationNormalizeService.normalizeImage(file, targetName);
        if (aiResult.isPresent()) {
            return aiResult.get();
        }

        ImageTextExtractionService.ImageTextExtractionResult extractionResult = imageTextExtractionService.extract(file);
        return conversationNormalizeService.normalizeRawText(
                extractionResult.rawText(),
                targetName,
                extractionResult.warning()
        );
    }

    private ChatSourceType detectSourceType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return ChatSourceType.IMAGE;
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")) {
                return ChatSourceType.IMAGE;
            }
        }

        return ChatSourceType.TXT;
    }

    private String readFileAsText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ChatUploadException("업로드 파일을 텍스트로 읽을 수 없습니다.", exception);
        }
    }
}
