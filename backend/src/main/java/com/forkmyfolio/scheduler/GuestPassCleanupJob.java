package com.forkmyfolio.scheduler;

import com.forkmyfolio.repository.GuestPassRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class GuestPassCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(GuestPassCleanupJob.class);
    private final GuestPassRepository guestPassRepository;

    /**
     * Purges expired guest passes from the database.
     * Runs every hour (3600000 ms).
     */
    @Scheduled(fixedRate = 3600000) //hourly
    @Transactional
    public void cleanupExpiredPasses() {
        logger.info("Starting scheduled cleanup of expired GuestPass tokens...");
        try {
            guestPassRepository.deleteByExpiresAtBefore(Instant.now());
            logger.info("Successfully purged expired GuestPass tokens.");
        } catch (Exception e) {
            logger.error("Failed to cleanup expired GuestPass tokens: {}", e.getMessage(), e);
        }
    }
}
