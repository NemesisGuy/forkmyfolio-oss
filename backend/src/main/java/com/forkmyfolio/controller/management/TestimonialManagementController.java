package com.forkmyfolio.controller.management;

import com.forkmyfolio.dto.create.CreateTestimonialRequest;
import com.forkmyfolio.dto.response.TestimonialDto;
import com.forkmyfolio.dto.update.UpdateTestimonialRequest;
import com.forkmyfolio.mapper.TestimonialMapper;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.TestimonialService;
import com.forkmyfolio.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/me/testimonials")
@Tag(name = "Testimonial Management (Me)", description = "Endpoints for the authenticated user to manage their own testimonials.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
public class TestimonialManagementController {

    private final TestimonialService testimonialService;
    private final UserService userService;
    private final TestimonialMapper testimonialMapper;

    @GetMapping
    @Operation(summary = "Get all of my testimonials")
    public ResponseEntity<List<TestimonialDto>> getMyTestimonials() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        List<Testimonial> testimonials = testimonialService.getTestimonialsForUser(currentUser);
        List<TestimonialDto> testimonialDtos = testimonials.stream()
                .map(testimonialMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(testimonialDtos);
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get one of my testimonials by its UUID")
    public ResponseEntity<TestimonialDto> getMyTestimonialByUuid(@PathVariable UUID uuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Testimonial testimonial = testimonialService.findTestimonialByUuidAndUser(uuid, currentUser);
        return ResponseEntity.ok(testimonialMapper.toDto(testimonial));
    }

    @PostMapping
    @Operation(summary = "Create a new testimonial for myself")
    public ResponseEntity<TestimonialDto> createMyTestimonial(@Valid @RequestBody CreateTestimonialRequest createRequest) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Testimonial newTestimonial = testimonialMapper.toEntity(createRequest, currentUser);
        Testimonial createdTestimonial = testimonialService.createTestimonial(newTestimonial);
        return new ResponseEntity<>(testimonialMapper.toDto(createdTestimonial), HttpStatus.CREATED);
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update one of my testimonials")
    public ResponseEntity<TestimonialDto> updateMyTestimonial(@PathVariable UUID uuid, @Valid @RequestBody UpdateTestimonialRequest updateRequest) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Testimonial testimonialToUpdate = testimonialService.findTestimonialByUuidAndUser(uuid, currentUser);
        // Optional-field update policy is applied by the mapper translation layer, not here.
        testimonialMapper.applyUpdateFromRequest(updateRequest, testimonialToUpdate);
        Testimonial savedTestimonial = testimonialService.save(testimonialToUpdate);
        return ResponseEntity.ok(testimonialMapper.toDto(savedTestimonial));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete one of my testimonials")
    public ResponseEntity<Void> deleteMyTestimonial(@PathVariable UUID uuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        testimonialService.deleteTestimonial(uuid, currentUser);
        return ResponseEntity.noContent().build();
    }
}