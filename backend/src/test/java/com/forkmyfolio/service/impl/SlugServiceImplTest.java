package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SlugServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SlugServiceImpl slugService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void testGenerateUniqueSlug_NotExists() {
        when(userRepository.existsBySlug("john-doe")).thenReturn(false);
        String slug = slugService.generateUniqueSlug("John Doe");
        assertEquals("john-doe", slug);
    }

    @Test
    void testGenerateUniqueSlug_Exists() {
        when(userRepository.existsBySlug("john-doe")).thenReturn(true);
        when(userRepository.existsBySlug("john-doe-2")).thenReturn(false);
        String slug = slugService.generateUniqueSlug("John Doe");
        assertEquals("john-doe-2", slug);
    }

    @Test
    void testGenerateUniqueSlug_Reserved() {
        // 'admin' is in the reserved list
        when(userRepository.existsBySlug("admin-2")).thenReturn(false);
        String slug = slugService.generateUniqueSlug("Admin");
        assertEquals("admin-2", slug);
    }

    @Test
    void testIsValidSlug() {
        assertTrue(slugService.isValidSlug("john-doe"));
        assertFalse(slugService.isValidSlug("admin")); // Reserved
        assertFalse(slugService.isValidSlug("John Doe")); // Not sanitized (caps/space)
        assertFalse(slugService.isValidSlug("null")); // Reserved
        assertFalse(slugService.isValidSlug("")); // Empty
        assertFalse(slugService.isValidSlug(null)); // Null
    }

    @Test
    void testIsSlugAvailable_NewUser() {
        when(userRepository.findBySlug("john-doe")).thenReturn(Optional.empty());
        assertTrue(slugService.isSlugAvailable("john-doe", UUID.randomUUID()));
    }

    @Test
    void testIsSlugAvailable_SameUser() {
        User user = new User();
        user.setUuid(userId);
        user.setSlug("john-doe");

        when(userRepository.findBySlug("john-doe")).thenReturn(Optional.of(user));
        assertTrue(slugService.isSlugAvailable("john-doe", userId));
    }

    @Test
    void testIsSlugAvailable_TakenByOther() {
        User otherUser = new User();
        otherUser.setUuid(UUID.randomUUID());
        otherUser.setSlug("john-doe");

        when(userRepository.findBySlug("john-doe")).thenReturn(Optional.of(otherUser));
        assertFalse(slugService.isSlugAvailable("john-doe", userId));
    }

    @Test
    void testIsSlugAvailable_Reserved() {
        assertFalse(slugService.isSlugAvailable("admin", userId));
    }
}
