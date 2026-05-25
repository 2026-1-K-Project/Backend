package com.example.kproject.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "종합 분석 리포트 응답")
public record ReportResponse(
        @Schema(description = "저장된 리포트 ID", example = "1")
        Long reportId,
        @Schema(description = "분석 카테고리", example = "썸/연애")
        String category,
        @Schema(description = "분석 모드. 구조화 분석 가능 여부에 따라 STRUCTURED 또는 FLEXIBLE 값이 반환됩니다.")
        ReportAnalysisMode analysisMode,
        @Schema(description = "구조화 파싱 가능 여부", example = "true")
        boolean structuredParsingAvailable,
        @Schema(description = "분석 과정 경고 메시지. 없으면 null")
        String warning,
        @Schema(description = "상단 요약 정보")
        Summary summary,
        @Schema(description = "관계 역동성 분석 결과")
        RelationshipDynamics relationshipDynamics,
        @Schema(description = "성격 및 심리 추정 결과")
        PersonalityPsychology personalityPsychology,
        @Schema(description = "자유 분석 기반 정성 신호")
        QualitativeSignals qualitativeSignals,
        @Schema(description = "감정 타임라인 점수 목록")
        List<EmotionTimelinePoint> emotionTimeline,
        @Schema(description = "분위기 변화에 영향을 준 결정적 순간 목록")
        List<DecisiveMoment> decisiveMoments,
        @Schema(description = "실전 활용 인사이트")
        ActionableInsights actionableInsights
) {
    @Schema(description = "상단 요약 정보")
    public record Summary(
            @Schema(description = "호감 지수", example = "62")
            int interestScore,
            @Schema(description = "한 줄 요약 메시지", example = "긍정적인 기류가 흐르고 있어요. 조금만 더!")
            String headline
    ) {
    }

    @Schema(description = "관계 역동성 분석")
    public record RelationshipDynamics(
            @Schema(description = "대화 점유율")
            TalkRatio talkRatio,
            @Schema(description = "평균 답장 시간(분)", example = "12")
            int averageReplyMinutes,
            @Schema(description = "언어 동기화 지수", example = "88")
            int languageSync,
            @Schema(description = "주요 키워드 목록")
            List<String> keywords
    ) {
    }

    @Schema(description = "대화 점유율")
    public record TalkRatio(
            @Schema(description = "내 메시지 비율", example = "45")
            int me,
            @Schema(description = "상대 메시지 비율", example = "55")
            int other
    ) {
    }

    @Schema(description = "성격 및 심리 추정 결과")
    public record PersonalityPsychology(
            @Schema(description = "예상 MBTI")
            Estimate mbti,
            @Schema(description = "애착 유형 추정")
            Estimate attachmentType,
            @Schema(description = "Big Five 추정 점수")
            BigFive bigFive
    ) {
    }

    @Schema(description = "자유 분석 기반 정성 신호")
    public record QualitativeSignals(
            @Schema(description = "관계 분위기 요약")
            String relationshipSummary,
            @Schema(description = "상대 성향 추정")
            String counterpartyTendency,
            @Schema(description = "긍정 반응 주제 목록")
            List<String> positiveTopics,
            @Schema(description = "좋아할 가능성이 높은 요소")
            List<String> likelyPreferences,
            @Schema(description = "선호도가 낮아 보이는 요소")
            List<String> likelyDislikes,
            @Schema(description = "추천 답장 목록")
            List<String> recommendedReplies
    ) {
    }

    @Schema(description = "추정 결과 공통 구조")
    public record Estimate(
            @Schema(description = "추정 타입", example = "ENFP")
            String type,
            @Schema(description = "추정 신뢰도", example = "0.63")
            double confidence,
            @Schema(description = "추정 설명", example = "활발하고 반응이 빠른 성향이 추정됩니다.")
            String description
    ) {
    }

    @Schema(description = "Big Five 성격 지표 추정치")
    public record BigFive(
            @Schema(description = "개방성", example = "72")
            int openness,
            @Schema(description = "성실성", example = "58")
            int conscientiousness,
            @Schema(description = "외향성", example = "81")
            int extraversion,
            @Schema(description = "친화성", example = "76")
            int agreeableness,
            @Schema(description = "신경성", example = "41")
            int neuroticism
    ) {
    }

    @Schema(description = "감정 타임라인 포인트")
    public record EmotionTimelinePoint(
            @Schema(description = "구간 인덱스", example = "1")
            int index,
            @Schema(description = "감정 점수", example = "42")
            int score,
            @Schema(description = "근거가 된 메시지")
            String message
    ) {
    }

    @Schema(description = "결정적 순간")
    public record DecisiveMoment(
            @Schema(description = "섹션 제목", example = "결정적 순간")
            String title,
            @Schema(description = "해당 시점", example = "2026-04-29T20:14:00")
            String dateTime,
            @Schema(description = "핵심 메시지", example = "내일 우리 같이 영화 볼래?")
            String message,
            @Schema(description = "분석 설명", example = "함께하자는 제안 이후 상대방 반응이 더 적극적으로 바뀐 것으로 추정됩니다.")
            String description
    ) {
    }

    @Schema(description = "실전 활용 인사이트")
    public record ActionableInsights(
            @Schema(description = "대화 꿀팁 목록")
            List<String> tips,
            @Schema(description = "주의할 점 목록")
            List<String> warnings,
            @Schema(description = "추천 질문 목록")
            List<String> recommendedQuestions
    ) {
    }
}
