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
import java.util.ArrayList;
import java.util.List;
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
        return upload(file, category, targetName, null);
    }

    public ChatUploadResponse upload(MultipartFile file, String category, String targetName, String description) {
        if (file == null || file.isEmpty()) {
            throw new ChatUploadException("업로드할 대화 파일이 필요합니다.");
        }

        String resolvedCategory = StringUtils.hasText(category) ? category.trim() : DEFAULT_CATEGORY;
        ChatSourceType sourceType = detectSourceType(file);
        NormalizedConversationResult normalizedResult = switch (sourceType) {
            case TXT -> normalizeTxt(file, targetName);
            case IMAGE -> normalizeImage(file, targetName);
            case MIXED -> throw new ChatUploadException("단일 파일 업로드에서는 MIXED sourceType을 사용할 수 없습니다.");
        };

        ConversationReport report = reportStorageService.createReport(
                resolvedCategory,
                sourceType,
                normalizedResult,
                description,
                1
        );

        return new ChatUploadResponse(
                report.getId(),
                report.getStatus(),
                normalizedResult.analysisMode(),
                "대화 업로드 및 정형화가 완료되었습니다.",
                normalizedResult.warning()
        );
    }

    public ChatUploadResponse uploadBatch(
            List<MultipartFile> files,
            String category,
            String targetName,
            String description
    ) {
        List<MultipartFile> validFiles = files == null ? List.of() : files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (validFiles.isEmpty()) {
            throw new ChatUploadException("업로드할 대화 파일이 필요합니다.");
        }

        String resolvedCategory = StringUtils.hasText(category) ? category.trim() : DEFAULT_CATEGORY;
        ChatSourceType sourceType = detectBatchSourceType(validFiles);
        NormalizedConversationResult normalizedResult =
                validFiles.size() == 1 && sourceType != ChatSourceType.MIXED
                        ? switch (sourceType) {
                            case TXT -> normalizeTxt(validFiles.get(0), targetName);
                            case IMAGE -> normalizeImage(validFiles.get(0), targetName);
                            case MIXED -> normalizeFiles(validFiles, targetName, description);
                        }
                        : normalizeFiles(validFiles, targetName, description);

        ConversationReport report = reportStorageService.createReport(
                resolvedCategory,
                sourceType,
                normalizedResult,
                description,
                validFiles.size()
        );

        return new ChatUploadResponse(
                report.getId(),
                report.getStatus(),
                normalizedResult.analysisMode(),
                "대화 파일 묶음 업로드 및 정형화가 완료되었습니다.",
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

    private NormalizedConversationResult normalizeFiles(List<MultipartFile> files, String targetName, String description) {
        Optional<NormalizedConversationResult> aiResult =
                aiConversationNormalizeService.normalizeFiles(files, targetName, description);
        if (aiResult.isPresent()) {
            return aiResult.get();
        }

        StringBuilder rawText = new StringBuilder();
        List<String> warnings = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            rawText.append("\n\n[upload ")
                    .append(index + 1)
                    .append("/")
                    .append(files.size())
                    .append(": ")
                    .append(file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename())
                    .append("]\n");
            if (detectSourceType(file) == ChatSourceType.IMAGE) {
                ImageTextExtractionService.ImageTextExtractionResult extractionResult = imageTextExtractionService.extract(file);
                rawText.append(extractionResult.rawText());
                if (StringUtils.hasText(extractionResult.warning())) {
                    warnings.add(extractionResult.warning());
                }
            } else {
                rawText.append(readFileAsText(file));
            }
        }

        return conversationNormalizeService.normalizeRawText(
                rawText.toString(),
                targetName,
                warnings.isEmpty()
                        ? "OpenAI 정형화를 사용할 수 없어 업로드 파일의 텍스트 기반 fallback으로 분석했습니다."
                        : String.join(" ", warnings)
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

    private ChatSourceType detectBatchSourceType(List<MultipartFile> files) {
        boolean hasImage = false;
        boolean hasText = false;
        for (MultipartFile file : files) {
            if (detectSourceType(file) == ChatSourceType.IMAGE) {
                hasImage = true;
            } else {
                hasText = true;
            }
        }
        if (hasImage && hasText) {
            return ChatSourceType.MIXED;
        }
        return hasImage ? ChatSourceType.IMAGE : ChatSourceType.TXT;
    }

    private String readFileAsText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ChatUploadException("업로드 파일을 텍스트로 읽을 수 없습니다.", exception);
        }
    }
}
