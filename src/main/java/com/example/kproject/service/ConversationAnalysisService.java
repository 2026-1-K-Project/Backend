package com.example.kproject.service;

import com.example.kproject.dto.KakaoChatMetaDto;

public interface ConversationAnalysisService {

    String analyze(String analysisText, KakaoChatMetaDto meta);
}
