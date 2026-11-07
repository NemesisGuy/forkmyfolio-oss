package com.forkmyfolio.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    /**
     * Custom Flyway migration strategy that automatically repairs the schema
     * history table
     * before running new migrations. This is useful for clearing out failed
     * migration entries
     * caused by intermittent issues or incorrect SQL syntax in previous releases.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
