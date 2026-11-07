package com.forkmyfolio.model;

import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a user in the ForkMyFolio system.
 * Implements Spring Security's UserDetails for authentication.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "portfolioProfile", "projects", "userSkills", "experiences", "qualifications", "testimonials",
        "contactMessages", "userSettings", "refreshTokens" })
@EqualsAndHashCode(of = "uuid")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User implements UserDetails {

    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Universally unique identifier for the user.
     */
    @Column(name = "uuid", nullable = false, updatable = false, unique = true)
    private UUID uuid;

    /**
     * The unique, URL-friendly identifier for the user's public portfolio.
     */
    @Column(name = "slug", unique = true, nullable = false, length = 50)
    private String slug;

    /**
     * User's email address. Must be unique.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * User's first name.
     */
    @Column(nullable = false)
    private String firstName;

    /**
     * User's last name.
     */
    @Column(nullable = false)
    private String lastName;

    /**
     * Hashed password for the user. Can be null for OAuth2 users.
     */
    private String password;

    /**
     * URL to the user's profile image.
     */
    private String profileImageUrl;

    /**
     * Flag indicating if the user's account is active.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Timestamp of when the user last changed their password.
     * If this is null, it indicates the user has never changed their initial
     * password.
     */
    private Instant passwordLastChangedAt;
    /**
     * Flag indicating if the user's email has been verified.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * The authentication provider used to register this user (e.g., LOCAL, GOOGLE).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    /**
     * The unique identifier for the user from the external provider.
     */
    private String providerId;

    /**
     * Timestamp of when the user accepted the Terms of Service and Privacy Policy.
     * Important for POPIA compliance. Can be null for users created before this
     * feature.
     */
    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    /**
     * The version of the terms the user accepted (e.g., a version number or date
     * string 'YYYY-MM-DD').
     * This provides an auditable record of which policy version was agreed to.
     */
    @Column(name = "terms_version")
    private String termsVersion;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private PortfolioProfile portfolioProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private Set<Project> projects = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private Set<UserSkill> userSkills = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private Set<Experience> experiences = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private Set<Qualification> qualifications = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private Set<Testimonial> testimonials = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private Set<ContactMessage> contactMessages = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private Set<UserSetting> userSettings = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    /**
     * Roles assigned to the user (e.g., ADMIN, USER).
     */
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Timestamp of when the user account was created.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of when the user account was last updated.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    // UserDetails methods

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.active;
    }
}