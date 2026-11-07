package com.forkmyfolio.controller.guest;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.aop.TrackVisitor;
import com.forkmyfolio.dto.create.CreateContactMessageRequest;
import com.forkmyfolio.model.enums.VisitorStatType;
import com.forkmyfolio.mapper.ContactMessageMapper;
import com.forkmyfolio.service.ContactMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolios/{slug}/contact-messages")
@Tag(name = "Contact Messages", description = "Endpoints for submitting contact messages to a specific user.")
@RequiredArgsConstructor
@Slf4j
public class ContactMessageController {

        private final ContactMessageService contactMessageService;
        private final ContactMessageMapper contactMessageMapper;

        @PostMapping
        @Operation(summary = "Submit a new contact message to a user", description = "Allows visitors to submit a contact message to the user identified by the slug.", responses = {
                        @ApiResponse(responseCode = "201", description = "Message submitted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseWrapper.class)))
        })
        @ResponseStatus(HttpStatus.CREATED)
        @TrackVisitor(VisitorStatType.CONTACT_MESSAGE_SUBMISSION)
        public ApiResponseWrapper<Map<String, String>> submitContactMessage(
                        @Parameter(description = "The unique, URL-friendly slug of the user to contact.", example = "jane-doe") @PathVariable String slug,
                        @Valid @RequestBody CreateContactMessageRequest createRequest) {

                log.info("Received contact message for user with slug '{}' from: {}", slug, createRequest.getEmail());
                contactMessageService.saveMessage(slug, contactMessageMapper.toEntity(createRequest));
                log.info("Successfully saved contact message for user with slug '{}'.", slug);

                Map<String, String> response = Map.of("message", "Contact message submitted successfully.");
                return new ApiResponseWrapper<>(response);
        }
}