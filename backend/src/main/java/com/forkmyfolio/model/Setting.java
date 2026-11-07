package com.forkmyfolio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Represents a global, system-wide setting.
 * These settings are unique by name and serve as application-level
 * configurations
 * or as default values for user-configurable settings.
 * For user-specific overrides, see the {@link UserSetting} entity.
 */
@Entity
@Table(name = "settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "`value`", nullable = false, length = 255)
    private String value;

    @Column(length = 512)
    private String description;

    public Setting(String name, String value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}