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
}
