package com.example.kproject.dto;

public record KakaoChatMetaDto(
        String title,
        String savedAt,
        String roomName,
        String category,
        String targetName
) {
    public KakaoChatMetaDto withCategoryAndTargetName(String category, String targetName) {
        return new KakaoChatMetaDto(title, savedAt, roomName, category, targetName);
    }
}
