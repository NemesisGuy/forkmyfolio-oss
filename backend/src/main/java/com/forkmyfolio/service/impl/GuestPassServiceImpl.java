package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.GuestPassRepository;
import com.forkmyfolio.service.GuestPassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestPassServiceImpl implements GuestPassService {

    private final GuestPassRepository guestPassRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Override
    @Transactional
    public GuestPass createGuestPass(User owner, String label, Instant expiresAt) {
        String token = generateSecureToken();

        GuestPass guestPass = GuestPass.builder()
                .owner(owner)
                .label(label)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        log.info("Creating guest pass for user {} with label '{}', expires at {}", owner.getEmail(), label, expiresAt);
        return guestPassRepository.save(guestPass);
    }

    @Override
    @Transactional
    public void revokeGuestPass(User owner, UUID uuid) {
        GuestPass guestPass = guestPassRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("GuestPass not found"));

        if (!guestPass.getOwner().getId().equals(owner.getId())) {
            log.error("Access denied: User ID {} tried to revoke pass {} owned by User ID {}", owner.getId(), uuid,
                    guestPass.getOwner().getId());
            // Phase 7.4 (403/404 policy): cross-tenant revocation is concealed
            // as 404, matching the uuid-miss branch above.
            throw new ResourceNotFoundException("GuestPass not found");
        }

        log.info("Revoking guest pass {} (label: '{}') for owner {}", uuid, guestPass.getLabel(), owner.getEmail());
        guestPassRepository.delete(guestPass);
        guestPassRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestPass> getMyGuestPasses(User owner) {
        return guestPassRepository.findByOwner(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public GuestPass validateToken(String token) {
        log.debug("Validating guest token starting with {}", token.substring(0, 4));
        GuestPass guestPass = guestPassRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Invalid guest token provided");
                    return new ResourceNotFoundException("Invalid guest token");
                });

        if (guestPass.isExpired()) {
            log.warn("Guest pass {} has expired (expired at {})", guestPass.getUuid(), guestPass.getExpiresAt());
            throw new AccessDeniedException("Guest pass has expired");
        }

        // Initialize the owner while still in the transaction to prevent
        // LazyInitializationException in the controller
        org.hibernate.Hibernate.initialize(guestPass.getOwner());

        return guestPass;
    }

    private String generateSecureToken() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
