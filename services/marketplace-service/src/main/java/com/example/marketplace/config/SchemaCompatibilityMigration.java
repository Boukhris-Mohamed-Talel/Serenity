package com.example.marketplace.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * MySQL may create ENUM columns for @Enumerated fields depending on the existing schema.
 * When enums evolve (e.g., adding new statuses), inserts can fail with "Data truncated" errors.
 * This migrator keeps enum-backed columns compatible by forcing VARCHAR.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaCompatibilityMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateEnumColumnsToVarchar() {
        alterColumnSafely("ALTER TABLE article_orders MODIFY COLUMN status VARCHAR(64) NOT NULL");
        alterColumnSafely("ALTER TABLE article_orders MODIFY COLUMN payment_status VARCHAR(64) NOT NULL");
        alterColumnSafely("ALTER TABLE article_products MODIFY COLUMN category VARCHAR(64) NOT NULL");
        alterColumnSafely("ALTER TABLE article_products MODIFY COLUMN type VARCHAR(64) NOT NULL");
        alterColumnSafely("ALTER TABLE article_products MODIFY COLUMN preview_type VARCHAR(64) NULL");
    }

    private void alterColumnSafely(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.info("Schema compatibility migration applied: {}", sql);
        } catch (Exception ex) {
            // Ignore when table/column does not exist yet or is already compatible.
            log.debug("Schema compatibility migration skipped: {}. Reason: {}", sql, ex.getMessage());
        }
    }
}

