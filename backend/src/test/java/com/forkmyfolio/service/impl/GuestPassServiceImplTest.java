package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.GuestPassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestPassServiceImplTest {

    @Mock
    private GuestPassRepository guestPassRepository;

    @InjectMocks
    private GuestPassServiceImpl guestPassService;

    private User owner;
    private GuestPass guestPass;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");

        guestPass = new GuestPass();
        guestPass.setUuid(UUID.randomUUID());
        guestPass.setToken("valid-token");
        guestPass.setLabel("Test Pass");
        guestPass.setOwner(owner);
        guestPass.setCreatedAt(Instant.now());
        guestPass.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
    }

    @Test
    void createGuestPass_Success() {
        String label = "New Recruiter";
        Instant expiry = Instant.now().plus(48, ChronoUnit.HOURS);

        GuestPass savedPass = new GuestPass();
        savedPass.setLabel(label);
        savedPass.setToken("secure-token-123");

        when(guestPassRepository.save(any(GuestPass.class))).thenReturn(savedPass);

        GuestPass result = guestPassService.createGuestPass(owner, label, expiry);

        assertNotNull(result);
        assertEquals(label, result.getLabel());
        verify(guestPassRepository).save(any(GuestPass.class));
    }

    @Test
    void validateToken_Success() {
        when(guestPassRepository.findByToken("valid-token")).thenReturn(Optional.of(guestPass));

        GuestPass result = guestPassService.validateToken("valid-token");

        assertNotNull(result);
        assertEquals("valid-token", result.getToken());
    }

    @Test
    void validateToken_Expired_ThrowsException() {
        guestPass.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(guestPassRepository.findByToken("expired-token")).thenReturn(Optional.of(guestPass));

        assertThrows(AccessDeniedException.class, () -> guestPassService.validateToken("expired-token"));
    }

    @Test
    void getMyGuestPasses_Success() {
        when(guestPassRepository.findByOwner(owner))
                .thenReturn(Collections.singletonList(guestPass));

        List<GuestPass> result = guestPassService.getMyGuestPasses(owner);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Pass", result.get(0).getLabel());
    }

    @Test
    void revokeGuestPass_Success() {
        when(guestPassRepository.findByUuid(guestPass.getUuid()))
                .thenReturn(Optional.of(guestPass));

        guestPassService.revokeGuestPass(owner, guestPass.getUuid());

        verify(guestPassRepository).delete(guestPass);
    }

    @Test
    void revokeGuestPass_NotFound_ThrowsException() {
        UUID randomUuid = UUID.randomUUID();
        when(guestPassRepository.findByUuid(randomUuid)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> guestPassService.revokeGuestPass(owner, randomUuid));
    }
}
