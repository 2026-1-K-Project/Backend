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
@Table(name = "analysis_results")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reportId;

    @Column(nullable = false)
    private String analysisType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resultJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public AnalysisResult(Long reportId, AnalysisType analysisType, String resultJson) {
        this.reportId = reportId;
        this.analysisType = analysisType.name();
        this.resultJson = resultJson;
        this.createdAt = LocalDateTime.now();
    }
}
