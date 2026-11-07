package com.forkmyfolio.service;

import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.model.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GuestPassService {
    GuestPass createGuestPass(User owner, String label, Instant expiresAt);

    void revokeGuestPass(User owner, UUID uuid);

    List<GuestPass> getMyGuestPasses(User owner);

    GuestPass validateToken(String token);
}
