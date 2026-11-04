package com.cloudcostai.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Enterprise-grade authenticated credential encryption service using AES-256-GCM.
 * Protects cloud credentials (AWS Access Keys, Azure Client Secrets, GCP Service Account JSONs)
 * stored at rest in MySQL.
 *
 * Format: {keyVersion}:{Base64(12-byte IV + Ciphertext + 16-byte Auth Tag)}
 */
@Service
@Slf4j
public class CredentialEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // 96-bit IV recommended by NIST SP 800-38D
    private static final int GCM_TAG_LENGTH_BITS = 128; // 128-bit authentication tag

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${cloudcost.security.credential-encryption-key}")
    private String rawEncryptionKey;

    @Value("${cloudcost.security.key-version:v1}")
    private String activeKeyVersion;

    private SecretKey secretKey;

    @PostConstruct
    public void validateAndInitializeKey() {
        if (rawEncryptionKey == null || rawEncryptionKey.trim().isEmpty()) {
            throw new IllegalStateException("FATAL: CLOUD_CREDENTIAL_ENCRYPTION_KEY environment variable is missing or empty.");
        }

        byte[] keyBytes = resolveKeyBytes(rawEncryptionKey.trim());
        if (keyBytes.length != 32) {
            throw new IllegalStateException("FATAL: CLOUD_CREDENTIAL_ENCRYPTION_KEY must be exactly 256 bits (32 bytes). Found: " + keyBytes.length + " bytes.");
        }

        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

        // Self-test AES-GCM cipher availability and roundtrip validity
        try {
            String testPlaintext = "CloudCostAI-StartupHealthCheck-2026";
            String encrypted = encrypt(testPlaintext);
            String decrypted = decrypt(encrypted);
            if (!testPlaintext.equals(decrypted)) {
                throw new IllegalStateException("FATAL: Credential encryption self-test failed: plaintext mismatch.");
            }
            log.info("CredentialEncryptionService initialized successfully [Algorithm: AES-256-GCM, Active Key Version: {}]", activeKeyVersion);
        } catch (Exception e) {
            throw new IllegalStateException("FATAL: CredentialEncryptionService initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves raw key representation from Hex, Base64, or direct UTF-8 string bytes.
     */
    private byte[] resolveKeyBytes(String rawKey) {
        if (rawKey.length() == 64 && isHexString(rawKey)) {
            return HexFormat.of().parseHex(rawKey);
        }
        try {
            byte[] base64Decoded = Base64.getDecoder().decode(rawKey);
            if (base64Decoded.length == 32) {
                return base64Decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Not a base64 encoded string, fallback to UTF-8
        }
        return rawKey.getBytes(StandardCharsets.UTF_8);
    }

    private boolean isHexString(String s) {
        for (char c : s.toCharArray()) {
            if (Character.digit(c, 16) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encrypts sensitive cloud credentials using AES-256-GCM with a fresh 12-byte random IV.
     * Output format: {keyVersion}:{Base64(IV + Ciphertext + Tag)}
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertextWithTag = cipher.doFinal(plaintextBytes);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertextWithTag.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertextWithTag);

            String encodedPayload = Base64.getEncoder().encodeToString(byteBuffer.array());
            return activeKeyVersion + ":" + encodedPayload;
        } catch (Exception e) {
            log.error("Failed to encrypt sensitive credential payload: {}", e.getMessage());
            throw new SecurityException("Unable to encrypt credential payload securely", e);
        }
    }

    /**
     * Decrypts and authenticates AES-256-GCM payload.
     * Verifies tag integrity and rejects tampered data.
     */
    public String decrypt(String encryptedPayload) {
        if (encryptedPayload == null || encryptedPayload.trim().isEmpty()) {
            return null;
        }

        try {
            String payloadToDecode = encryptedPayload;
            String payloadVersion = activeKeyVersion;

            if (encryptedPayload.contains(":")) {
                int colonIndex = encryptedPayload.indexOf(":");
                payloadVersion = encryptedPayload.substring(0, colonIndex);
                payloadToDecode = encryptedPayload.substring(colonIndex + 1);
            }

            byte[] decoded = Base64.getDecoder().decode(payloadToDecode);
            if (decoded.length < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
                throw new IllegalArgumentException("Corrupt ciphertext: byte length is too short for GCM IV and Tag.");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] ciphertextWithTag = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertextWithTag);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(ciphertextWithTag);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt and authenticate credential payload: {}", e.getMessage());
            throw new SecurityException("Credential decryption failed: authentication tag mismatch or invalid payload format", e);
        }
    }

    public String getActiveKeyVersion() {
        return activeKeyVersion;
    }
}
