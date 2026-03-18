package com.trustai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class CompanySchemaInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CompanySchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public CompanySchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureCompanyTable();
        ensureCompanyColumns();
        seedDefaultCompanies();
        bindLegacyUsersToDefaultCompany();
    }

    private void ensureCompanyTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS company (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              company_code VARCHAR(64) NOT NULL,
              company_name VARCHAR(128) NOT NULL,
              status TINYINT DEFAULT 1,
              create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
        log.info("Schema check complete: company");
    }

    private void ensureCompanyColumns() {
        ensureColumn("sys_user", "company_id", "BIGINT");
        ensureColumn("sys_user", "nickname", "VARCHAR(50)");
        ensureColumn("sys_user", "avatar", "VARCHAR(255)");
        ensureColumn("sys_user", "device_id", "VARCHAR(128)");
        ensureColumn("sys_user", "organization_type", "VARCHAR(64)");
        ensureColumn("sys_user", "login_type", "VARCHAR(32)");
        ensureColumn("sys_user", "wechat_open_id", "VARCHAR(128)");
        ensureColumn("sys_user", "account_type", "VARCHAR(20)");
        ensureColumn("sys_user", "account_status", "VARCHAR(20)");
        ensureColumn("sys_user", "approved_by", "BIGINT");
        ensureColumn("sys_user", "reject_reason", "VARCHAR(255)");
        ensureColumn("sys_user", "approved_at", "TIMESTAMP");
        ensureColumn("role", "company_id", "BIGINT");
        ensureColumn("data_asset", "company_id", "BIGINT");
        ensureColumn("risk_event", "company_id", "BIGINT");
        ensureColumn("security_event", "company_id", "BIGINT");
        ensureColumn("approval_request", "company_id", "BIGINT");
        ensureColumn("approval_request", "process_instance_id", "VARCHAR(64)");
        ensureColumn("approval_request", "task_id", "VARCHAR(64)");
        ensureColumn("subject_request", "company_id", "BIGINT");
        ensureColumn("compliance_policy", "company_id", "BIGINT");
        ensureColumn("client_report", "company_id", "BIGINT");
        ensureColumn("client_scan_queue", "company_id", "BIGINT");
    }

    private void ensureColumn(String table, String column, String type) {
        try {
            Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                table,
                column
            );
            if (exists != null && exists > 0) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        } catch (Exception ex) {
            log.debug("Skip migration for {}.{}: {}", table, column, ex.getMessage());
        }
    }

    private void seedDefaultCompanies() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM company", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
            INSERT INTO company (company_code, company_name, status, create_time, update_time)
            VALUES
            ('aegis-default', 'Aegis 默认公司', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            ('aegis-sandbox', 'Aegis 沙箱公司', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);
    }

    private void bindLegacyUsersToDefaultCompany() {
        try {
            jdbcTemplate.update("UPDATE sys_user SET company_id = 1 WHERE company_id IS NULL");
            jdbcTemplate.update("UPDATE sys_user SET account_type = 'demo' WHERE account_type IS NULL OR account_type = ''");
            jdbcTemplate.update("UPDATE sys_user SET account_status = CASE WHEN status = 0 THEN 'disabled' ELSE 'active' END WHERE account_status IS NULL OR account_status = ''");
            jdbcTemplate.update("UPDATE role SET company_id = 1 WHERE company_id IS NULL");
            jdbcTemplate.update("UPDATE approval_request SET company_id = 1 WHERE company_id IS NULL");
            jdbcTemplate.update("UPDATE subject_request SET company_id = 1 WHERE company_id IS NULL");
            jdbcTemplate.update("UPDATE compliance_policy SET company_id = 1 WHERE company_id IS NULL");
            jdbcTemplate.update("UPDATE client_report SET company_id = 1 WHERE company_id IS NULL");
            jdbcTemplate.update("UPDATE client_scan_queue SET company_id = 1 WHERE company_id IS NULL");
        } catch (Exception ex) {
            log.debug("Skip binding legacy users to default company: {}", ex.getMessage());
        }
    }
}
