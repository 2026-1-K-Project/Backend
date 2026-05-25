package com.example.kproject.repository;

import com.example.kproject.domain.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    Optional<AnalysisResult> findByReportIdAndAnalysisType(Long reportId, String analysisType);
}
