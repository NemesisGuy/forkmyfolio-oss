package com.forkmyfolio.service;

import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.User;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing contact messages.
 * Adheres to NAG-01 (DBR): Purely domain-based.
 */
public interface ContactMessageService {

    ContactMessage saveMessage(String userSlug, ContactMessage message);

    List<ContactMessage> findAll();

    void deleteByUuid(UUID uuid);

    List<ContactMessage> getMessagesForUser(User user);

    void deleteMessageAsOwner(UUID messageUuid, User owner);

    long getUnreadMessageCount(User user);

    /**
     * Updates a message's attributes.
     *
     * @param messageUuid The UUID of the message to update.
     * @param updateData  Entity containing update fields.
     * @param owner       The user who owns the message.
     * @return The updated message entity.
     */
    ContactMessage updateMessage(UUID messageUuid, ContactMessage updateData, User owner);
}