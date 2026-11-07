package com.forkmyfolio.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks the read status of an announcement for a specific user.
 */
@Entity
@Table(name = "announcement_reads", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "announcement_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class AnnouncementRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime readAt;

    public AnnouncementRead(User user, Announcement announcement) {
        this.user = user;
        this.announcement = announcement;
    }
}
