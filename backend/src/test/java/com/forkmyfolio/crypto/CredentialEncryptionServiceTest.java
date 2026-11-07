package com.forkmyfolio.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 8.5 (DEF-1) — AES-256-GCM credential encryption at rest.
 *
 * <p>These are the guarantees that make storing provider API keys acceptable:
 * ciphertexts are non-deterministic (random IV), tampering is detected
 * (GCM auth tag), and a missing/mis-sized key fails fast instead of falling
 * back to an insecure default.</p>
 */
class CredentialEncryptionServiceTest {

    private static final String KEY_32_BYTES = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String KEY_16_BYTES = Base64.getEncoder().encodeToString(new byte[16]);

    @Test
    void roundtrip_recoversTheOriginalPlaintext() {
        CredentialEncryptionService service = new CredentialEncryptionService(KEY_32_BYTES);
        String secret = "umami_live_key_0123456789abcdef";

        String ciphertext = service.encrypt(secret);

        assertThat(ciphertext).isNotEqualTo(secret);
        assertThat(service.decrypt(ciphertext)).isEqualTo(secret);
    }

    @Test
    void encryptionIsNonDeterministic_freshIvPerCall() {
        CredentialEncryptionService service = new CredentialEncryptionService(KEY_32_BYTES);

        String first = service.encrypt("same-plaintext");
        String second = service.encrypt("same-plaintext");

        assertThat(first).isNotEqualTo(second);
        assertThat(service.decrypt(first)).isEqualTo("same-plaintext");
        assertThat(service.decrypt(second)).isEqualTo("same-plaintext");
    }

    @Test
    void tamperedCiphertext_isRejected() {
        CredentialEncryptionService service = new CredentialEncryptionService(KEY_32_BYTES);
        String ciphertext = service.encrypt("sensitive-value");

        byte[] raw = Base64.getDecoder().decode(ciphertext);
        raw[raw.length - 1] ^= 0x01; // flip one bit of the auth tag/ciphertext
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ciphertextFromADifferentKey_isRejected() {
        CredentialEncryptionService serviceA = new CredentialEncryptionService(KEY_32_BYTES);
        String otherKey = Base64.getEncoder().encodeToString(new byte[]{
                9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 9, 8, 7, 6, 5, 4,
                3, 2, 1, 0, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 9, 8});
        CredentialEncryptionService serviceB = new CredentialEncryptionService(otherKey);

        String ciphertext = serviceA.encrypt("cross-key-secret");

        assertThatThrownBy(() -> serviceB.decrypt(ciphertext))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingKey_isSafeFailure_notAnInsecureDefault() {
        CredentialEncryptionService service = new CredentialEncryptionService("");

        assertThat(service.isAvailable()).isFalse();
        assertThatThrownBy(() -> service.encrypt("secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ANALYTICS_ENCRYPTION_KEY");
    }

    @Test
    void wrongKeyLength_failsFastAtConstruction() {
        assertThatThrownBy(() -> new CredentialEncryptionService(KEY_16_BYTES))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void blankPlaintext_isRejected() {
        CredentialEncryptionService service = new CredentialEncryptionService(KEY_32_BYTES);

        assertThatThrownBy(() -> service.encrypt("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
