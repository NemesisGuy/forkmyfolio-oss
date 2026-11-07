package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.TestimonialRepository;
import com.forkmyfolio.service.TestimonialService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestimonialServiceImpl implements TestimonialService {

    private final TestimonialRepository testimonialRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Testimonial> getTestimonialsForUser(User user) {
        return testimonialRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Testimonial findTestimonialByUuidAndUser(UUID uuid, User user) {
        Testimonial testimonial = testimonialRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial not found with UUID: " + uuid));

        if (!testimonial.getUser().getId().equals(user.getId())) {
            // Phase 7.4 (403/404 policy): cross-tenant access is concealed
            // as 404, matching the uuid-miss branch above.
            throw new ResourceNotFoundException("Testimonial not found with UUID: " + uuid);
        }
        return testimonial;
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#testimonial.user.slug")
    public Testimonial createTestimonial(Testimonial testimonial) {
        return testimonialRepository.save(testimonial);
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#testimonial.user.slug")
    public Testimonial save(Testimonial testimonial) {
        // This method is used for updates. The ownership check happens before this is
        // called.
        return testimonialRepository.save(testimonial);
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#currentUser.slug")
    public void deleteTestimonial(UUID uuid, User currentUser) {
        // findTestimonialByUuidAndUser performs both the lookup and the ownership
        // check.
        Testimonial testimonialToDelete = findTestimonialByUuidAndUser(uuid, currentUser);
        testimonialRepository.delete(testimonialToDelete);
    }
}
