package com.forkmyfolio.config.startup;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @deprecated This seeder has been replaced by the more efficient {@link SkillDataLoader}.
 * This component is now a no-op and can be safely removed from the project.
 * Its functionality has been consolidated to prevent redundant database checks during startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Deprecated
public class SkillSeeder {

    @PostConstruct
    public void seedSkills() {
        log.warn("SkillSeeder is deprecated and no longer runs. Its functionality is handled by SkillDataLoader. Please remove this class.");
    }
}