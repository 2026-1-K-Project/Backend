package com.example.kproject.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "reports")
public class ConversationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String sourceType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String normalizedJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String participantsJson;

    @Column(nullable = false)
    private int messageCount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String analysisMode;

    @Column(columnDefinition = "TEXT")
    private String warning;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Long memberId;

    @Column(nullable = false)
    private boolean trashed;

    @Column
    private LocalDateTime trashedAt;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int uploadedFileCount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summaryJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fullReportJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ConversationReport(
            String category,
            ChatSourceType sourceType,
            String rawText,
            String normalizedJson,
            String participantsJson,
            int messageCount,
            ReportStatus status,
            String analysisMode,
            String warning
    ) {
        this(category, sourceType, rawText, normalizedJson, participantsJson, messageCount, status, analysisMode, warning, null, 1, null);
    }

    public ConversationReport(
            String category,
            ChatSourceType sourceType,
            String rawText,
            String normalizedJson,
            String participantsJson,
            int messageCount,
            ReportStatus status,
            String analysisMode,
            String warning,
            String description,
            int uploadedFileCount
    ) {
        this(category, sourceType, rawText, normalizedJson, participantsJson, messageCount, status, analysisMode, warning, description, uploadedFileCount, null);
    }

    public ConversationReport(
            String category,
            ChatSourceType sourceType,
            String rawText,
            String normalizedJson,
            String participantsJson,
            int messageCount,
            ReportStatus status,
            String analysisMode,
            String warning,
            String description,
            int uploadedFileCount,
            Long memberId
    ) {
        this.category = category;
        this.sourceType = sourceType.name();
        this.rawText = rawText == null ? "" : rawText;
        this.normalizedJson = normalizedJson == null ? "{}" : normalizedJson;
        this.participantsJson = participantsJson;
        this.messageCount = messageCount;
        this.status = status.name();
        this.analysisMode = analysisMode;
        this.warning = warning;
        this.description = description == null ? "" : description;
        this.memberId = memberId;
        this.trashed = false;
        this.trashedAt = null;
        this.title = category + " 분석 리포트";
        this.uploadedFileCount = Math.max(uploadedFileCount, 1);
        this.summaryJson = "{}";
        this.fullReportJson = "{}";
        this.createdAt = LocalDateTime.now();
    }

    public void moveToTrash() {
        this.trashed = true;
        this.trashedAt = LocalDateTime.now();
    }

    public void restoreFromTrash() {
        this.trashed = false;
        this.trashedAt = null;
    }
}
