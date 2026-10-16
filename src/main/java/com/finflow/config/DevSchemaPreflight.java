package com.finflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevSchemaPreflight {
    private static final Logger log = LoggerFactory.getLogger(DevSchemaPreflight.class);

    public DevSchemaPreflight(JdbcTemplate jdbcTemplate) {
        try {
            Long duplicateWebhookRows = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(group_count - 1), 0) FROM (SELECT COUNT(*) AS group_count FROM payment_webhook_events WHERE event_id IS NOT NULL GROUP BY provider, event_id HAVING COUNT(*) > 1) duplicates",
                    Long.class);
            Long nullRoleRows = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE role IS NULL", Long.class);
            log.warn("Dev schema preflight: duplicate webhook rows={}, null user roles={}", duplicateWebhookRows, nullRoleRows);
        } catch (Exception exception) {
            log.warn("Dev schema preflight could not inspect existing rows: {}", exception.getClass().getSimpleName());
        }
    }
}
