package com.forkmyfolio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolio_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "uuid")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class PortfolioProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Section-level Toggle: Controls the visibility of the profile summary section
     * on an otherwise public portfolio page.
     */
    @Column(nullable = false)
    private boolean visible = true;

    /**
     * Master-level Toggle: Controls the visibility of the entire public-facing
     * portfolio.
     * If false, the public slug (e.g., /portfolio/john-doe) will be inaccessible.
     * Defaults to false for security.
     */
    @Column(name = "is_public", nullable = false)
    private boolean publiclyVisible = false;

    /**
     * Phase 10 (Workstream 2): user-controlled search-engine indexing
     * preference. Independent of {@link #publiclyVisible} — a publicly
     * accessible portfolio can opt out of search indexing while remaining
     * reachable through its direct URL. Defaults to false (conservative
     * opt-in) so no existing portfolio becomes indexable unexpectedly.
     * Always subordinate to the deployment-wide NUXT_PUBLIC_INDEXABLE switch.
     */
    @Column(name = "search_indexable", nullable = false)
    private boolean searchIndexable = false;

    @Column(length = 100)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Email
    private String publicEmail;

    @URL
    private String websiteUrl;

    @URL
    private String linkedinUrl;

    @URL
    private String githubUrl;

    @URL
    private String resumeUrl; // URL to the downloadable PDF resume

    @URL
    private String resumeImageUrl; // URL to a preview image of the resume (e.g., a PNG/JPG)

    @Column(length = 50)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String coverLetterTemplate;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}