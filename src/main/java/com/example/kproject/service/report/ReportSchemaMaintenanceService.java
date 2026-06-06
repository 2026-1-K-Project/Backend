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
        try {
            jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS description TEXT");
            jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS uploaded_file_count INTEGER NOT NULL DEFAULT 1");
            jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS member_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS trashed BOOLEAN NOT NULL DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS trashed_at TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS title VARCHAR(255)");
            jdbcTemplate.execute("UPDATE reports SET title = COALESCE(title, category || ' 분석 리포트')");
        } catch (Exception exception) {
            log.debug("Skipping reports upload metadata column maintenance: {}", exception.getMessage());
        }
    }
}
