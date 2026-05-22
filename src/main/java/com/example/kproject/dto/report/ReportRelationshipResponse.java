package com.example.kproject.dto.report;

import java.util.List;

public record ReportRelationshipResponse(
        Long reportId,
        int interestScore,
        int intimacyScore,
        int relationshipTemperature,
        int activenessScore,
        int continuityScore,
        ReportResponse.TalkRatio talkRatio,
        int averageReplyMinutes,
        int languageSync,
        List<String> keywords,
        List<ReportResponse.EmotionTimelinePoint> emotionTimeline
) {
}
