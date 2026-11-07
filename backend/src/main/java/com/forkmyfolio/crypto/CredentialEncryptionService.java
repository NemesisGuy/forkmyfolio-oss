package com.forkmyfolio.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for analytics provider credentials at rest (DEF-1).
 *
 * <p>Wire format: {@code Base64( iv[12] || ciphertext||tag )}. A fresh random IV
 * is used for every encryption. The key comes from the {@code ANALYTICS_ENCRYPTION_KEY}
 * environment variable (Base64-encoded 32 bytes). There is deliberately no
 * default key: when the key is absent, {@link #isAvailable()} is false and any
 * attempt to encrypt fails fast — the safe failure mode is "no stored secrets",
 * not "secrets protected by a well-known key".</p>
 */
@Service
public class CredentialEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptionService(@Value("${app.analytics.encryption-key:}") String base64Key) {
        SecretKeySpec spec = null;
        if (base64Key != null && !base64Key.isBlank()) {
            byte[] key = Base64.getDecoder().decode(base64Key.trim());
            if (key.length != 32) {
                throw new IllegalStateException(
                        "ANALYTICS_ENCRYPTION_KEY must decode to exactly 32 bytes (AES-256); got " + key.length);
            }
            spec = new SecretKeySpec(key, "AES");
        }
        this.keySpec = spec;
    }

    /** True when an encryption key is configured; secret-bearing config requires this. */
    public boolean isAvailable() {
        return keySpec != null;
    }

    /** Encrypts plaintext to the Base64(iv || ciphertext||tag) wire format. */
    public String encrypt(String plaintext) {
        if (keySpec == null) {
            throw new IllegalStateException(
                    "ANALYTICS_ENCRYPTION_KEY is not configured; refusing to store analytics credentials.");
        }
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Cannot encrypt a blank credential.");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv).put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt credential", e);
        }
    }

    /** Decrypts the Base64(iv || ciphertext||tag) wire format back to plaintext. */
    public String decrypt(String encoded) {
        if (keySpec == null) {
            throw new IllegalStateException(
                    "ANALYTICS_ENCRYPTION_KEY is not configured; stored analytics credentials cannot be decrypted.");
        }
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException("Cannot decrypt a blank credential.");
        }
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            if (all.length <= IV_BYTES) {
                throw new IllegalArgumentException("Ciphertext too short.");
            }
            byte[] iv = new byte[IV_BYTES];
            byte[] cipherText = new byte[all.length - IV_BYTES];
            System.arraycopy(all, 0, iv, 0, IV_BYTES);
            System.arraycopy(all, IV_BYTES, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt credential", e);
        }
    }
}
