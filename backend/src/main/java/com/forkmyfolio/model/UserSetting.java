package com.forkmyfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Represents a setting that is specific to an individual user.
 * This allows a user to override the global default settings for their
 * own public portfolio (e.g., choosing to hide the 'Skills' section).
 * The combination of a user and a setting name must be unique.
 */
@Entity
@Table(name = "user_settings", uniqueConstraints = {
        // Ensures a user can only have one entry for each setting name.
        @UniqueConstraint(columnNames = { "user_id", "name" })
})
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "A user-specific override for a global application setting.")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class UserSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "The internal database identifier.", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @UuidGenerator
    @Column(name = "uuid", nullable = false, updatable = false, unique = true)
    @Schema(description = "The external unique identifier (UUID) for API usage.", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID uuid;

    /**
     * The user to whom this setting belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @Schema(hidden = true) // Hide from API docs as it's an internal relationship
    private User user;

    /**
     * The name of the setting (e.g., "SHOW_SKILLS").
     */
    @NotBlank
    @Column(nullable = false)
    @Schema(description = "The unique name of the setting being overridden.", example = "SHOW_SKILLS")
    private String name;

    /**
     * The user-specific value for the setting (e.g., "false").
     */
    @NotBlank
    @Column(name = "`value`", nullable = false, length = 255)
    @Schema(description = "The user's chosen value for this setting.", example = "false")
    private String value;

    public UserSetting(User user, String name, String value) {
        this.user = user;
        this.name = name;
        this.value = value;
    }
}