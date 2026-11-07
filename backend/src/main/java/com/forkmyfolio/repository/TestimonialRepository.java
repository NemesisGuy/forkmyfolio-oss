package com.forkmyfolio.repository;

import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {

    // Find all testimonials for a specific user, ordered by creation date
    List<Testimonial> findByUserOrderByCreatedAtDesc(User user);

    Optional<Testimonial> findByUuid(UUID uuid);

    void deleteByUser(User user);

    List<Testimonial> findByUserAndVisibleTrue(User user);

    List<Testimonial> findByUser(User user);

    long countByUser(User user);

}