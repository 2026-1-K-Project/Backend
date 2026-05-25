package com.example.kproject.repository;

import com.example.kproject.domain.ConversationReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationReportRepository extends JpaRepository<ConversationReport, Long> {
}
