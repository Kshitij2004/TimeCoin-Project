package t_12.backend.service;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * Implements TOTP (Time-based One-Time Password) per RFC 6238.
 * HMAC-SHA1, 30-second windows, 6-digit codes.
 */
@Service
public class TwoFactorService {

    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int ALLOWED_WINDOW = 1; // +/- 1 step for clock drift

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /**
     * Generates a random Base32 secret (20 bytes → 32 chars).
     */
    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return base32Encode(buffer);
    }

    /**
     * Builds an otpauth:// URI for QR code display.
     */
    public String buildOtpAuthUri(String secret, String username, String issuer) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                urlEncode(issuer), urlEncode(username), secret, urlEncode(issuer)
        );
    }

    /**
     * Verifies a 6-digit TOTP code against the stored secret.
     * Accepts codes from the current window and +/- 1 step for drift.
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }

        long currentStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;

        for (int offset = -ALLOWED_WINDOW; offset <= ALLOWED_WINDOW; offset++) {
            String expected = generateCode(secret, currentStep + offset);
            if (expected != null && expected.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a TOTP code for a given time step.
     */
    private String generateCode(String secret, long timeStep) {
        try {
            byte[] key = base32Decode(secret);

            // Convert time step to 8-byte big-endian
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(timeBytes);

            // Dynamic truncation per RFC 4226
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    |  (hash[offset + 3] & 0xFF);

            int otp = binary % 1_000_000;
            return String.format("%06d", otp);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return null;
        }
    }

    // ── Base32 ─────────────────────────────────────────────────────────────

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                sb.append(BASE32_ALPHABET.charAt(index));
                bitsLeft -= 5;
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(BASE32_ALPHABET.charAt(index));
        }

        return sb.toString();
    }

    private byte[] base32Decode(String input) {
        // Normalize: uppercase, strip padding and whitespace
        String cleaned = input.toUpperCase().replaceAll("[=\\s]", "");

        int outputLength = (cleaned.length() * 5) / 8;
        byte[] output = new byte[outputLength];

        int buffer = 0;
        int bitsLeft = 0;
        int outputIndex = 0;

        for (int i = 0; i < cleaned.length(); i++) {
            int value = BASE32_ALPHABET.indexOf(cleaned.charAt(i));
            if (value < 0) {
                continue; // skip any non-base32 chars
            }
            buffer = (buffer << 5) | value;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                output[outputIndex++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }

        return output;
    }

    private String urlEncode(String value) {
        return value.replace(" ", "%20").replace(":", "%3A");
    }
}