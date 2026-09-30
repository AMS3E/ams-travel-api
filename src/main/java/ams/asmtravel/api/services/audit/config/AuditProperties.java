package ams.asmtravel.api.services.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.audit.*}.
 *
 * @param enabled   whether catalogue writes are logged at all; off only for load tests and imports
 * @param retention how long entries are kept, and when the trim runs
 */
@ConfigurationProperties(prefix = "app.audit")
public record AuditProperties(Boolean enabled, Retention retention) {

    public AuditProperties {
        enabled = enabled == null || enabled;
        retention = retention == null ? new Retention(null, null) : retention;
    }

    /**
     * @param days entries older than this are dropped nightly; 0 keeps everything
     * @param cron when the trim runs
     */
    public record Retention(Integer days, String cron) {

        public Retention {
            days = days == null ? 365 : days;
            cron = cron == null ? "0 30 3 * * *" : cron;
        }
    }
}
