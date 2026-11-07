package com.forkmyfolio.service;

import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for business logic related to Testimonials.
 * This service operates solely on domain models (e.g., Testimonial) and is DTO-agnostic.
 */
public interface TestimonialService {

    /**
     * Retrieves all testimonials belonging to a specific user.
     *
     * @param user The user whose testimonials are to be retrieved.
     * @return A list of {@link Testimonial} entities for the specified user.
     */
    List<Testimonial> getTestimonialsForUser(User user);

    /**
     * Retrieves a single testimonial by its UUID, ensuring it belongs to the specified user.
     *
     * @param uuid The UUID of the testimonial.
     * @param user The user who must own the testimonial.
     * @return The {@link Testimonial} entity if found and owned by the user.
     * @throws com.forkmyfolio.exception.ResourceNotFoundException if the testimonial is not found.
     * @throws AccessDeniedException                               if the user does not own the testimonial.
     */
    Testimonial findTestimonialByUuidAndUser(UUID uuid, User user);

    /**
     * Creates and persists a new testimonial.
     *
     * @param testimonial The pre-constructed testimonial entity to save. The owner must be set.
     * @return The persisted {@link Testimonial} entity.
     */
    Testimonial createTestimonial(Testimonial testimonial);

    /**
     * Saves an updated testimonial entity.
     *
     * @param testimonial The testimonial entity with updated fields to be saved.
     * @return The updated and persisted {@link Testimonial} entity.
     */
    Testimonial save(Testimonial testimonial);

    /**
     * Deletes a testimonial by its public UUID.
     *
     * @param uuid        The UUID of the testimonial to delete.
     * @param currentUser The user performing the action.
     * @throws AccessDeniedException if the user is not authorized.
     */
    void deleteTestimonial(UUID uuid, User currentUser);
}