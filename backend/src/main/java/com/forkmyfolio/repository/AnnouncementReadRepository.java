package com.forkmyfolio.repository;

import com.forkmyfolio.model.Announcement;
import com.forkmyfolio.model.AnnouncementRead;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    Optional<AnnouncementRead> findByUserAndAnnouncement(User user, Announcement announcement);

    List<AnnouncementRead> findByUser(User user);

    boolean existsByUserAndAnnouncement(User user, Announcement announcement);

    /** Bulk delete of read receipts (FK to users has no ON DELETE CASCADE). */
    @Modifying
    @Query("DELETE FROM AnnouncementRead r WHERE r.user = :user")
    int deleteByUser(@Param("user") User user);
}
