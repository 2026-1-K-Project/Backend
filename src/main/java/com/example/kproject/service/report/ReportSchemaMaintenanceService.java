package com.example.kproject.service.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportSchemaMaintenanceService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReportSchemaMaintenanceService.class);

    private final JdbcTemplate jdbcTemplate;

    public ReportSchemaMaintenanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        widenReportTextColumns();
        addUploadMetadataColumns();
    }

    private void widenReportTextColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE reports ALTER COLUMN raw_text TYPE TEXT");
            jdbcTemplate.execute("ALTER TABLE reports ALTER COLUMN normalized_json TYPE TEXT");
            jdbcTemplate.execute("ALTER TABLE reports ALTER COLUMN participants_json TYPE TEXT");
            jdbcTemplate.execute("ALTER TABLE reports ALTER COLUMN summary_json TYPE TEXT");
            jdbcTemplate.execute("ALTER TABLE reports ALTER COLUMN full_report_json TYPE TEXT");
        } catch (Exception exception) {
            log.debug("Skipping reports text column maintenance: {}", exception.getMessage());
        }
    }

    private void addUploadMetadataColumns() {
        executeMaintenance("ALTER TABLE reports ADD COLUMN IF NOT EXISTS description TEXT");
        executeMaintenance("ALTER TABLE reports ADD COLUMN IF NOT EXISTS uploaded_file_count INTEGER NOT NULL DEFAULT 1");
        executeMaintenance("ALTER TABLE reports ADD COLUMN IF NOT EXISTS member_id BIGINT");
        executeMaintenance("ALTER TABLE reports ADD COLUMN IF NOT EXISTS trashed BOOLEAN NOT NULL DEFAULT FALSE");
        executeMaintenance("ALTER TABLE reports ADD COLUMN IF NOT EXISTS trashed_at TIMESTAMP");
        executeMaintenance("ALTER TABLE reports ADD COLUMN IF NOT EXISTS title VARCHAR(255)");
        executeMaintenance("UPDATE reports SET title = COALESCE(NULLIF(title, ''), COALESCE(category, '대화') || ' 분석 리포트')");
        executeMaintenance("UPDATE reports SET status = COALESCE(NULLIF(status, ''), 'COMPLETED')");
        executeMaintenance("UPDATE reports SET analysis_mode = COALESCE(NULLIF(analysis_mode, ''), 'FLEXIBLE')");
        executeMaintenance("UPDATE reports SET uploaded_file_count = 1 WHERE uploaded_file_count IS NULL OR uploaded_file_count < 1");
        executeMaintenance("UPDATE reports SET trashed = FALSE WHERE trashed IS NULL");
    }

    private void executeMaintenance(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception exception) {
            log.warn("Skipping report schema maintenance SQL [{}]: {}", sql, exception.getMessage());
        }
    }
}
