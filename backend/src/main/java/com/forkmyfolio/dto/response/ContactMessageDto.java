package com.forkmyfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for representing ContactMessage information in API
 * responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageDto {

    /**
     * Unique identifier for the contact message.
     */
    private UUID uuid;

    /**
     * Name of the person who sent the message.
     */
    private String name;

    /**
     * Email address of the sender.
     */
    private String email;

    /**
     * The content of the message.
     */
    private String message;

    /**
     * Timestamp of when the message was created/received.
     */
    private LocalDateTime createdAt;

    /**
     * Whether the message has been read by the portfolio owner.
     */
    private boolean isRead;

    /**
     * The priority level of the message (e.g., LOW, MEDIUM, HIGH).
     */
    private String priority;

    /**
     * Flag to indicate if the message has been replied to.
     */
    private boolean isReplied;

    /**
     * Soft delete flag to archive the message.
     */
    /**
     * Soft delete flag to archive the message.
     */
    private boolean isArchived;

    /**
     * The user who received this message.
     */
    private UserDto user;
}