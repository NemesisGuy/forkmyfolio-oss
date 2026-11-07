package com.forkmyfolio.service;

import com.forkmyfolio.model.Qualification;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing Qualification entities.
 * Defines the business logic for qualification-related operations,
 * ensuring that all actions are performed on behalf of the currently
 * authenticated user.
 */
public interface QualificationService {

        /**
         * Retrieves all qualifications for the currently authenticated user.
         *
         * @return A list of {@link Qualification} entities.
         */
        List<Qualification> getQualificationsForCurrentUser();

        /**
         * Retrieves all qualifications for a specific user.
         *
         * @param user The user whose qualifications to retrieve.
         * @return A list of {@link Qualification} entities.
         */
        List<Qualification> getQualificationsForUser(com.forkmyfolio.model.User user);

        /**
         * Retrieves a single qualification by its UUID, ensuring it belongs to the
         * current user.
         *
         * @param qualificationUuid The UUID of the qualification to retrieve.
         * @return The found {@link Qualification} entity.
         * @throws com.forkmyfolio.exception.ResourceNotFoundException if the
         *                                                             qualification
         *                                                             does not exist or
         *                                                             does not belong
         *                                                             to the user.
         */
        Qualification getQualificationByUuidForCurrentUser(UUID qualificationUuid);

        /**
         * Creates a new qualification and associates it with the specified user.
         *
         * @param qualificationName  The name of the qualification.
         * @param institutionName    The name of the institution.
         * @param institutionLogoUrl URL for the institution's logo.
         * @param institutionWebsite URL for the institution's website.
         * @param fieldOfStudy       The field of study.
         * @param level              The academic level.
         * @param startYear          The start year.
         * @param completionYear     The completion year.
         * @param stillStudying      Flag for ongoing studies.
         * @param grade              The grade achieved.
         * @param credentialUrl      URL for a verifiable credential.
         * @param visible            Visibility on the public portfolio.
         * @param user               The user as the owner.
         * @return The newly created and persisted {@link Qualification} entity.
         */
        Qualification createQualification(String qualificationName, String institutionName, String institutionLogoUrl,
                        String institutionWebsite, String fieldOfStudy,
                        com.forkmyfolio.model.enums.QualificationLevel level, Integer startYear, Integer completionYear,
                        Boolean stillStudying, String grade, String credentialUrl, boolean visible,
                        com.forkmyfolio.model.User user);

        /**
         * Updates an existing qualification.
         *
         * @param qualificationUuid The UUID of the qualification to update.
         * @param user              The user for ownership verification.
         * @return The updated and persisted {@link Qualification} entity.
         */
        Qualification updateQualification(UUID qualificationUuid, String qualificationName, String institutionName,
                        String institutionLogoUrl, String institutionWebsite, String fieldOfStudy,
                        com.forkmyfolio.model.enums.QualificationLevel level, Integer startYear, Integer completionYear,
                        Boolean stillStudying, String grade, String credentialUrl, Boolean visible,
                        com.forkmyfolio.model.User user);

        /**
         * Deletes a qualification.
         *
         * @param qualificationUuid The UUID of the qualification to delete.
         * @param user              The user for ownership verification.
         */
        void deleteQualification(UUID qualificationUuid, com.forkmyfolio.model.User user);
}