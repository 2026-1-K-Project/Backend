package com.example.kproject.service.normalize;

import com.example.kproject.domain.NormalizedConversationResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface AiConversationNormalizeService {

    Optional<NormalizedConversationResult> normalizeText(String rawText, String targetName);

    Optional<NormalizedConversationResult> normalizeImage(MultipartFile file, String targetName);
}
