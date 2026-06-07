package com.example.kproject.service.normalize;

import com.example.kproject.domain.NormalizedConversationResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface AiConversationNormalizeService {

    default Optional<NormalizedConversationResult> normalizeText(String rawText, String targetName) {
        return normalizeText(rawText, targetName, null);
    }

    Optional<NormalizedConversationResult> normalizeText(String rawText, String targetName, String myName);

    default Optional<NormalizedConversationResult> normalizeImage(MultipartFile file, String targetName) {
        return normalizeImage(file, targetName, null);
    }

    Optional<NormalizedConversationResult> normalizeImage(MultipartFile file, String targetName, String myName);

    default Optional<NormalizedConversationResult> normalizeFiles(
            List<MultipartFile> files,
            String targetName,
            String description
    ) {
        return normalizeFiles(files, targetName, null, description);
    }

    default Optional<NormalizedConversationResult> normalizeFiles(
            List<MultipartFile> files,
            String targetName,
            String myName,
            String description
    ) {
        return Optional.empty();
    }
}
