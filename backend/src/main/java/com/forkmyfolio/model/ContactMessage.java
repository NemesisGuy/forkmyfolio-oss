package com.forkmyfolio.model;

import com.forkmyfolio.model.enums.MessagePriority;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a contact message submitted through the platform.
 */
@Entity
@Table(name = "contact_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ContactMessage {

    /**
     * Unique identifier for the contact message.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing unique identifier for the contact message.
     * Automatically generated.
     */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, updatable = false, unique = true)
    private UUID uuid;

    /**
     * Name of the person who sent the message.
     * Cannot be blank.
     */
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String name;

    /**
     * Email address of the sender.
     * Must be a valid email format and cannot be blank.
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Column(nullable = false)
    private String email;

    /**
     * The content of the message.
     * Cannot be blank and has a maximum length.
     * Stored as TEXT for potentially longer messages.
     */
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 10, max = 5000, message = "Message must be between 10 and 5000 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * The user who owns the portfolio to which this message was sent.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Timestamp of when the message was created/received.
     * Automatically set by Hibernate.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Whether the message has been read by the portfolio owner. Defaults to false.
     */
    @NotNull
    @Column(nullable = false)
    private boolean isRead = false;

    /**
     * The priority level of the message. Defaults to MEDIUM.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessagePriority priority = MessagePriority.MEDIUM;

    /**
     * Flag to indicate if the message has been replied to. Defaults to false.
     */
    @NotNull
    @Column(nullable = false)
    private boolean isReplied = false;

    /**
     * Soft delete flag to archive the message instead of permanent deletion.
     * Defaults to false.
     */
    @NotNull
    @Column(nullable = false)
    private boolean isArchived = false;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}