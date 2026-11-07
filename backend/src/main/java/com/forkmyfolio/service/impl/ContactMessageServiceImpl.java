package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.ContactMessageRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.ContactMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service implementation for contact messages.
 * Adheres to NAG-01 (DBR): Purely domain-based.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactMessageServiceImpl implements ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ContactMessage saveMessage(String userSlug, ContactMessage message) {
        User targetUser = userRepository.findBySlugAndActiveTrue(userSlug)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with slug: " + userSlug));

        // Sanitize potentially dangerous fields to prevent XSS
        message.setMessage(Encode.forHtml(message.getMessage()));
        message.setName(Encode.forHtml(message.getName()));

        message.setUser(targetUser);
        return contactMessageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactMessage> findAll() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public void deleteByUuid(UUID uuid) {
        if (!contactMessageRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException("ContactMessage not found with UUID: " + uuid);
        }
        contactMessageRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactMessage> getMessagesForUser(User user) {
        return contactMessageRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional
    public void deleteMessageAsOwner(UUID messageUuid, User owner) {
        ContactMessage message = contactMessageRepository.findByUuid(messageUuid)
                .orElseThrow(() -> new ResourceNotFoundException("ContactMessage not found with UUID: " + messageUuid));

        if (!message.getUser().getId().equals(owner.getId())) {
            // Phase 7.4 (403/404 policy): cross-tenant deletion is concealed
            // as 404, matching the uuid-miss branch above.
            throw new ResourceNotFoundException("ContactMessage not found with UUID: " + messageUuid);
        }
        contactMessageRepository.delete(message);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadMessageCount(User user) {
        return contactMessageRepository.countByUserAndIsReadFalseAndIsArchivedFalse(user);
    }

    @Override
    @Transactional
    public ContactMessage updateMessage(UUID messageUuid, ContactMessage updateData, User owner) {
        ContactMessage message = contactMessageRepository.findByUuid(messageUuid)
                .orElseThrow(() -> new ResourceNotFoundException("ContactMessage not found with UUID: " + messageUuid));

        if (!message.getUser().getId().equals(owner.getId())) {
            // Phase 7.4 (403/404 policy): cross-tenant modification is
            // concealed as 404, matching the uuid-miss branch above.
            throw new ResourceNotFoundException("ContactMessage not found with UUID: " + messageUuid);
        }

        // Partially update fields based on non-null domain data
        if (updateData.getPriority() != null) {
            message.setPriority(updateData.getPriority());
        }
        if (updateData.isRead() != message.isRead()) { // Primitive check
            message.setRead(updateData.isRead());
        }
        if (updateData.isReplied() != message.isReplied()) {
            message.setReplied(updateData.isReplied());
        }
        if (updateData.isArchived() != message.isArchived()) {
            message.setArchived(updateData.isArchived());
        }

        return contactMessageRepository.save(message);
    }
}
