package com.example.kproject.service;

import com.example.kproject.dto.KakaoChatMetaDto;
import org.springframework.stereotype.Service;

@Service
public class PlaceholderConversationAnalysisService implements ConversationAnalysisService {

    @Override
    public String analyze(String analysisText, KakaoChatMetaDto meta) {
        throw new UnsupportedOperationException("Conversation analysis integration is not implemented yet.");
    }
}
