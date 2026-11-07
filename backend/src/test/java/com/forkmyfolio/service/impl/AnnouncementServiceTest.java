package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.Announcement;
import com.forkmyfolio.model.AnnouncementRead;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.AnnouncementReadRepository;
import com.forkmyfolio.repository.AnnouncementRepository;
import com.forkmyfolio.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementReadRepository announcementReadRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    private User testUser;
    private Announcement testAnnouncement;
    private UUID announcementUuid;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        announcementUuid = UUID.randomUUID();
        testAnnouncement = new Announcement();
        testAnnouncement.setUuid(announcementUuid);
        testAnnouncement.setId(10L);
    }

    @Test
    void markAsRead_whenNotAlreadyRead_shouldSaveReadRecord() {
        when(announcementRepository.findByUuid(announcementUuid)).thenReturn(Optional.of(testAnnouncement));
        when(announcementReadRepository.existsByUserAndAnnouncement(testUser, testAnnouncement)).thenReturn(false);

        announcementService.markAsRead(announcementUuid, testUser);

        verify(announcementReadRepository, times(1)).save(any(AnnouncementRead.class));
    }

    @Test
    void markAsRead_whenAlreadyRead_shouldNotSaveAgain() {
        when(announcementRepository.findByUuid(announcementUuid)).thenReturn(Optional.of(testAnnouncement));
        when(announcementReadRepository.existsByUserAndAnnouncement(testUser, testAnnouncement)).thenReturn(true);

        announcementService.markAsRead(announcementUuid, testUser);

        verify(announcementReadRepository, never()).save(any(AnnouncementRead.class));
    }

    @Test
    void getReadAnnouncementUuids_shouldReturnUuids() {
        AnnouncementRead read = new AnnouncementRead(testUser, testAnnouncement);
        when(announcementReadRepository.findByUser(testUser)).thenReturn(Collections.singletonList(read));

        Set<UUID> result = announcementService.getReadAnnouncementUuids(testUser);

        assertEquals(1, result.size());
        assertTrue(result.contains(announcementUuid));
    }

    @Test
    void markAllAsRead_shouldSaveRecordsForAll() {
        when(announcementRepository.findAll()).thenReturn(Collections.singletonList(testAnnouncement));
        when(announcementReadRepository.existsByUserAndAnnouncement(testUser, testAnnouncement)).thenReturn(false);

        announcementService.markAllAsRead(testUser);

        verify(announcementReadRepository, times(1)).save(any(AnnouncementRead.class));
    }
}
