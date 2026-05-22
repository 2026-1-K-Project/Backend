package com.example.kproject.service.upload;

import com.example.kproject.exception.ChatUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageTextExtractionService {

    public ImageTextExtractionResult extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ChatUploadException("업로드된 이미지 파일이 비어 있습니다.");
        }

        return new ImageTextExtractionResult(
                "",
                "이미지 OCR/비전 분석은 아직 연결되지 않았습니다. sourceType=IMAGE 리포트만 생성됩니다."
        );
    }

    public record ImageTextExtractionResult(
            String rawText,
            String warning
    ) {
    }
}
