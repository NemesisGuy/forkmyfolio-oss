package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.Announcement;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.AnnouncementRepository;
import com.forkmyfolio.service.AnnouncementService;
import com.forkmyfolio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.forkmyfolio.model.AnnouncementRead;
import com.forkmyfolio.repository.AnnouncementReadRepository;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final UserService userService;

    @Override
    @Transactional
    public Announcement createAnnouncement(Announcement announcement, UUID adminId) {
        User admin = userService.getUserByUuid(adminId);

        // Finalize entity state
        announcement.setSender(admin);

        return announcementRepository.save(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsForRole(Role role) {
        return announcementRepository.findAllForRole(role);
    }

    @Override
    @Transactional
    public void markAsRead(UUID announcementUuid, User user) {
        Announcement announcement = announcementRepository.findByUuid(announcementUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found."));

        if (!announcementReadRepository.existsByUserAndAnnouncement(user, announcement)) {
            announcementReadRepository.save(new AnnouncementRead(user, announcement));
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        List<Announcement> announcements = announcementRepository.findAll();
        for (Announcement announcement : announcements) {
            if (!announcementReadRepository.existsByUserAndAnnouncement(user, announcement)) {
                announcementReadRepository.save(new AnnouncementRead(user, announcement));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getReadAnnouncementUuids(User user) {
        return announcementReadRepository.findByUser(user).stream()
                .map(read -> read.getAnnouncement().getUuid())
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void deleteAnnouncement(UUID uuid) {
        Announcement announcement = announcementRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found."));
        announcementRepository.delete(announcement);
    }
}
