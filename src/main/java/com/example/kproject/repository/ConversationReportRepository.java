package com.example.kproject.repository;

import com.example.kproject.domain.ConversationReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationReportRepository extends JpaRepository<ConversationReport, Long> {

    List<ConversationReport> findByTrashedFalseOrderByCreatedAtDesc();

    List<ConversationReport> findByTrashedTrueOrderByTrashedAtDesc();

    List<ConversationReport> findByMemberIdIsNullAndTrashedFalseOrderByCreatedAtDesc();

    List<ConversationReport> findByMemberIdIsNullAndTrashedTrueOrderByTrashedAtDesc();

    List<ConversationReport> findByMemberIdAndTrashedFalseOrderByCreatedAtDesc(Long memberId);

    List<ConversationReport> findByMemberIdAndTrashedTrueOrderByTrashedAtDesc(Long memberId);
}
