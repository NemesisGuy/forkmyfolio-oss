package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.QualificationRepository;
import com.forkmyfolio.service.QualificationService;
import com.forkmyfolio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of the {@link QualificationService} interface.
 * This class contains the business logic for managing qualifications.
 * It interacts with the {@link QualificationRepository} and {@link UserService}
 * to perform its operations, adhering to the application's architectural rules.
 */
@Service
@RequiredArgsConstructor
public class QualificationServiceImpl implements QualificationService {

    private final QualificationRepository qualificationRepository;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public List<Qualification> getQualificationsForCurrentUser() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        return getQualificationsForUser(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Qualification> getQualificationsForUser(User user) {
        return qualificationRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Qualification getQualificationByUuidForCurrentUser(UUID qualificationUuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        return qualificationRepository.findByUuidAndUser(qualificationUuid, currentUser)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Qualification not found with UUID: " + qualificationUuid));
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#user.slug")
    public Qualification createQualification(String qualificationName, String institutionName,
            String institutionLogoUrl, String institutionWebsite, String fieldOfStudy,
            com.forkmyfolio.model.enums.QualificationLevel level, Integer startYear, Integer completionYear,
            Boolean stillStudying, String grade, String credentialUrl, boolean visible, User user) {

        Qualification qualification = new Qualification();
        qualification.setUser(user);
        qualification.setQualificationName(qualificationName);
        qualification.setInstitutionName(institutionName);
        qualification.setInstitutionLogoUrl(institutionLogoUrl);
        qualification.setInstitutionWebsite(institutionWebsite);
        qualification.setFieldOfStudy(fieldOfStudy);
        qualification.setLevel(level);
        qualification.setStartYear(startYear);
        qualification.setCompletionYear(completionYear);
        qualification.setStillStudying(stillStudying);
        qualification.setGrade(grade);
        qualification.setCredentialUrl(credentialUrl);
        qualification.setVisible(visible);

        return qualificationRepository.save(qualification);
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#user.slug")
    public Qualification updateQualification(UUID qualificationUuid, String qualificationName, String institutionName,
            String institutionLogoUrl, String institutionWebsite, String fieldOfStudy,
            com.forkmyfolio.model.enums.QualificationLevel level, Integer startYear, Integer completionYear,
            Boolean stillStudying, String grade, String credentialUrl, Boolean visible, User user) {

        Qualification qualificationToUpdate = qualificationRepository.findByUuidAndUser(qualificationUuid, user)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Qualification not found with UUID: " + qualificationUuid));

        qualificationToUpdate.setQualificationName(qualificationName);
        qualificationToUpdate.setInstitutionName(institutionName);
        qualificationToUpdate.setInstitutionLogoUrl(institutionLogoUrl);
        qualificationToUpdate.setInstitutionWebsite(institutionWebsite);
        qualificationToUpdate.setFieldOfStudy(fieldOfStudy);
        qualificationToUpdate.setLevel(level);
        qualificationToUpdate.setStartYear(startYear);
        qualificationToUpdate.setCompletionYear(completionYear);
        qualificationToUpdate.setStillStudying(stillStudying);
        qualificationToUpdate.setGrade(grade);
        qualificationToUpdate.setCredentialUrl(credentialUrl);
        qualificationToUpdate.setVisible(visible);

        return qualificationRepository.save(qualificationToUpdate);
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#user.slug")
    public void deleteQualification(UUID qualificationUuid, User user) {
        Qualification qualificationToDelete = qualificationRepository.findByUuidAndUser(qualificationUuid, user)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Qualification not found with UUID: " + qualificationUuid));
        qualificationRepository.delete(qualificationToDelete);
    }
}