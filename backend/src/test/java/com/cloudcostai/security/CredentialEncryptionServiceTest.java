package com.cloudcostai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialEncryptionServiceTest {

    private CredentialEncryptionService encryptionService;

    // Exactly 32 bytes (256 bits)
    private static final String VALID_KEY = "0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        encryptionService = new CredentialEncryptionService();
        ReflectionTestUtils.setField(encryptionService, "rawEncryptionKey", VALID_KEY);
        ReflectionTestUtils.setField(encryptionService, "activeKeyVersion", "v1");
        encryptionService.validateAndInitializeKey();
    }

    @Test
    @DisplayName("Should successfully encrypt and decrypt credential payload")
    void shouldEncryptAndDecryptSuccessfully() {
        String secretJson = "{\"clientId\":\"azure-app-123\",\"clientSecret\":\"s3cr3t!Valu3\"}";

        String encrypted = encryptionService.encrypt(secretJson);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).startsWith("v1:");
        assertThat(encrypted).isNotEqualTo(secretJson);

        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(secretJson);
    }

    @Test
    @DisplayName("Should produce different ciphertexts for the same plaintext due to random IVs")
    void shouldProduceDifferentCiphertextsDueToRandomIv() {
        String plaintext = "my-aws-secret-access-key-12345";

        String ciphertext1 = encryptionService.encrypt(plaintext);
        String ciphertext2 = encryptionService.encrypt(plaintext);

        assertThat(ciphertext1).isNotEqualTo(ciphertext2);
        assertThat(encryptionService.decrypt(ciphertext1)).isEqualTo(plaintext);
        assertThat(encryptionService.decrypt(ciphertext2)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("Should reject tampered ciphertext with SecurityException")
    void shouldRejectTamperedCiphertext() {
        String plaintext = "confidential-gcp-service-account-key";
        String encrypted = encryptionService.encrypt(plaintext);

        // Tamper with one character in the Base64 portion
        String[] parts = encrypted.split(":");
        String tamperedBase64 = parts[1].substring(0, parts[1].length() - 4) + "AAAA";
        String tamperedPayload = parts[0] + ":" + tamperedBase64;

        assertThatThrownBy(() -> encryptionService.decrypt(tamperedPayload))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("decryption failed");
    }

    @Test
    @DisplayName("Should fail startup initialization when key length is invalid")
    void shouldFailStartupWhenKeyLengthIsInvalid() {
        CredentialEncryptionService invalidService = new CredentialEncryptionService();
        ReflectionTestUtils.setField(invalidService, "rawEncryptionKey", "too-short-key");
        ReflectionTestUtils.setField(invalidService, "activeKeyVersion", "v1");

        assertThatThrownBy(invalidService::validateAndInitializeKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be exactly 256 bits");
    }

    @Test
    @DisplayName("Should return null when encrypting or decrypting null/empty string")
    void shouldHandleNullAndEmptyGracefully() {
        assertThat(encryptionService.encrypt(null)).isNull();
        assertThat(encryptionService.encrypt("   ")).isNull();
        assertThat(encryptionService.decrypt(null)).isNull();
        assertThat(encryptionService.decrypt("   ")).isNull();
    }
}
