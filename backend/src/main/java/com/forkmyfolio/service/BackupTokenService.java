package com.forkmyfolio.service;

/**
 * Service responsible for re-verifying a user's password for sensitive
 * backup operations.
 *
 * <p>Business rules implemented here (moved out of the controller to keep
 * controllers thin per NAG-01):
 * <ol>
 *   <li>The supplied password is validated against the currently
 *       authenticated user's stored credential hash. Blank or missing
 *       passwords fail closed (Phase 0.6C: the legacy ADMIN environment-value
 *       / blank-password fallback was removed as an authentication bypass).</li>
 * </ol>
 * Token creation is deliberately NOT part of this service: the backup-token
 * lifecycle belongs to {@code JwtTokenProvider} (a security infrastructure
 * component) and the orchestrating controller, mirroring how
 * {@code BackupController}/{@code AdminBackupController} validate an existing
 * backup token. The plaintext password is never stored or logged.
 */
public interface BackupTokenService {

    /**
     * Verifies the supplied password for the authenticated user.
     *
     * @param email       the authenticated user's email (may be null when no
     *                    authentication is present; not used for verification)
     * @param rawPassword the plaintext password from the request (null or
     *                    blank is always rejected)
     * @return {@code true} when the caller may receive a backup token
     */
    boolean verifyPasswordForBackup(String email, String rawPassword);
}
