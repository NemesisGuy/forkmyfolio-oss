package com.forkmyfolio.service;

import com.forkmyfolio.model.Announcement;
import com.forkmyfolio.model.enums.Role;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.forkmyfolio.model.User;

public interface AnnouncementService {
    Announcement createAnnouncement(Announcement announcement, UUID adminId);

    List<Announcement> getAllAnnouncements();

    List<Announcement> getAnnouncementsForRole(Role role);

    void markAsRead(UUID announcementUuid, User user);

    void markAllAsRead(User user);

    Set<UUID> getReadAnnouncementUuids(User user);

    void deleteAnnouncement(UUID uuid);
}
