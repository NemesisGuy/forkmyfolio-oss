package com.forkmyfolio.repository;

import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    boolean existsByUser(User user);

    Optional<ContactMessage> findByUuid(UUID uuid);

    List<ContactMessage> findByUserOrderByCreatedAtDesc(User user);

    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    /**
     * Bulk delete of messages addressed to a user. The User entity also cascades
     * this collection, but the hard-delete path removes them explicitly first so
     * no orphaned rows can survive an interrupted cascade.
     */
    @Modifying
    @Query("DELETE FROM ContactMessage m WHERE m.user = :user")
    int deleteByUser(@Param("user") User user);

    /**
     * Counts the number of messages for a specific user that are marked as unread
     * and are not archived.
     *
     * @param user The user whose unread messages are to be counted.
     * @return The total count of unread, non-archived messages.
     */
    long countByUserAndIsReadFalseAndIsArchivedFalse(User user);
}