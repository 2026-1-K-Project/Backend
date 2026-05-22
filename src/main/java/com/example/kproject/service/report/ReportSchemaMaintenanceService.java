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
            jdbcTemplate.execute("ALTER TABLE reports MODIFY raw_text LONGTEXT NOT NULL");
            jdbcTemplate.execute("ALTER TABLE reports MODIFY normalized_json LONGTEXT NOT NULL");
            jdbcTemplate.execute("ALTER TABLE reports MODIFY participants_json LONGTEXT NOT NULL");
            jdbcTemplate.execute("ALTER TABLE reports MODIFY summary_json LONGTEXT NOT NULL");
            jdbcTemplate.execute("ALTER TABLE reports MODIFY full_report_json LONGTEXT NOT NULL");
        } catch (Exception exception) {
            log.debug("Skipping reports text column maintenance: {}", exception.getMessage());
        }
    }
}
