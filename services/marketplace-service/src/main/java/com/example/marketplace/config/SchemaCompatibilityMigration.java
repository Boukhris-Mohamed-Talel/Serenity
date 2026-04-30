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

    @PostConstruct
    public void migrateReviewHelpfulVotesTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS article_review_helpful_votes ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "review_id BIGINT NOT NULL,"
                            + "user_id BIGINT NOT NULL,"
                            + "created_at DATETIME NOT NULL,"
                            + "UNIQUE KEY uk_article_review_helpful (review_id, user_id),"
                            + "CONSTRAINT fk_article_review_helpful_review FOREIGN KEY (review_id) "
                            + "REFERENCES article_product_reviews (id) ON DELETE CASCADE"
                            + ")");
            log.info("Schema compatibility: article_review_helpful_votes table ensured");
        } catch (Exception ex) {
            log.debug("Review helpful votes table migration skipped: {}", ex.getMessage());
        }
    }

    @PostConstruct
    public void migrateProductStockQuantity() {
        alterColumnSafely("ALTER TABLE article_products ADD COLUMN stock_quantity INT NULL");
        try {
            jdbcTemplate.update("UPDATE article_products SET stock_quantity = NULL WHERE UPPER(TRIM(type)) = 'DIGITAL'");
            jdbcTemplate.update(
                    "UPDATE article_products SET stock_quantity = 0 WHERE stock_quantity IS NULL AND UPPER(TRIM(type)) = 'PHYSICAL'");
        } catch (Exception ex) {
            log.debug("Stock quantity backfill skipped: {}", ex.getMessage());
        }
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

