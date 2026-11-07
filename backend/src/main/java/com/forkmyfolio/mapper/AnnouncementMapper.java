package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.response.AnnouncementDto;
import com.forkmyfolio.model.Announcement;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementMapper {

    public AnnouncementDto toDto(Announcement announcement) {
        return toDto(announcement, false);
    }

    public AnnouncementDto toDto(Announcement announcement, boolean isRead) {
        if (announcement == null) {
            return null;
        }
        AnnouncementDto dto = new AnnouncementDto();
        dto.setUuid(announcement.getUuid());
        dto.setTitle(announcement.getTitle());
        dto.setMessage(announcement.getMessage());
        dto.setTargetRole(announcement.getTargetRole());
        dto.setCreatedAt(announcement.getCreatedAt());
        dto.setRead(isRead);
        if (announcement.getSender() != null) {
            dto.setSenderName(announcement.getSender().getFirstName() + " " + announcement.getSender().getLastName());
        }
        return dto;
    }

    public java.util.List<AnnouncementDto> toDtoList(java.util.List<Announcement> announcements,
            java.util.Set<java.util.UUID> readUuids) {
        return announcements.stream()
                .map(a -> toDto(a, readUuids.contains(a.getUuid())))
                .collect(java.util.stream.Collectors.toList());
    }

    public Announcement toEntity(com.forkmyfolio.dto.create.CreateAnnouncementRequest request) {
        if (request == null) {
            return null;
        }
        return Announcement.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .targetRole(request.getTargetRole())
                .build();
    }
}
