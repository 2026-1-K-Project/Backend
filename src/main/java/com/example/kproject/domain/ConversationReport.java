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
@Table(name = "conversation_reports")
public class ConversationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String participantsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summaryJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fullReportJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ConversationReport(String category, String participantsJson, String summaryJson, String fullReportJson) {
        this.category = category;
        this.participantsJson = participantsJson;
        this.summaryJson = summaryJson;
        this.fullReportJson = fullReportJson;
        this.createdAt = LocalDateTime.now();
    }

    public void updateStoredJson(String summaryJson, String fullReportJson) {
        this.summaryJson = summaryJson;
        this.fullReportJson = fullReportJson;
    }
}
