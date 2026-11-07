package com.forkmyfolio.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Phase 7.4 (§6): a single structured "key feature" bullet that belongs to a
 * Project. Previously this kind of content was embedded as Markdown bullet
 * lines inside the project description; this entity makes the list explicit,
 * ordered (displayOrder = index in the submitted list), and independently
 * renderable (UI, PDF export, Markdown export, backups).
 */
@Entity
@Table(name = "key_features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "uuid")
public class KeyFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, updatable = false, unique = true)
    private UUID uuid;

    /**
     * The feature text itself (single line/short bullet).
     */
    @Column(nullable = false, length = 255)
    private String value;

    /**
     * Ordering within the owning project's key-features list.
     * Lower numbers appear first; the service re-assigns this from the list
     * index on every save.
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * The owning project.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
