package com.forkmyfolio.aop;

import com.forkmyfolio.model.enums.VisitorStatType;
import com.forkmyfolio.service.impl.VisitorStatsService;
import com.forkmyfolio.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class VisitorTrackingAspect {

    private static final Logger log = LoggerFactory.getLogger(VisitorTrackingAspect.class);
    private final VisitorStatsService visitorStatsService;
    private final SecurityUtils securityUtils;

    @AfterReturning(pointcut = "@annotation(trackVisitorAnnotation)")
    public void track(JoinPoint joinPoint, TrackVisitor trackVisitorAnnotation) {
        if (!securityUtils.isUserAnonymous()) {
            return; // Don't track logged-in users
        }

        VisitorStatType statType = trackVisitorAnnotation.value();
        log.debug("AOP: Anonymous user triggered visitor tracking for type: {}", statType);

        switch (statType) {
            case TOTAL_VISITS -> visitorStatsService.incrementTotalVisits();
            case PROJECTS_SECTION_VIEW -> visitorStatsService.incrementProjectsSectionView();
            case SKILLS_SECTION_VIEW -> visitorStatsService.incrementSkillsSectionView();
            case EXPERIENCE_SECTION_VIEW -> visitorStatsService.incrementExperienceSectionView();
            case QUALIFICATIONS_SECTION_VIEW -> visitorStatsService.incrementQualificationsSectionView();
            case CONTACT_MESSAGE_SUBMISSION -> visitorStatsService.incrementContactMessageSubmission();
            case PDF_DOWNLOAD -> visitorStatsService.incrementPdfDownload();
            case TESTIMONIALS_SECTION_VIEW -> visitorStatsService.incrementTestimonialsSectionView();
            case PROJECT_VIEW -> findUuidArgument(joinPoint)
                    .ifPresent(uuid -> visitorStatsService.incrementProjectView(uuid.toString()));
            case VCARD_DOWNLOAD -> visitorStatsService.incrementVcardDownload();
            default -> log.warn("Unhandled VisitorStatType in VisitorTrackingAspect: {}", statType);
        }
    }

    /**
     * Finds a UUID argument from the intercepted method call.
     * This is used to get the project ID for tracking individual project views.
     *
     * @param joinPoint The join point representing the method call.
     * @return An Optional containing the UUID if found.
     */
    private java.util.Optional<UUID> findUuidArgument(JoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getArgs())
                .filter(UUID.class::isInstance)
                .map(UUID.class::cast)
                .findFirst();
    }
}