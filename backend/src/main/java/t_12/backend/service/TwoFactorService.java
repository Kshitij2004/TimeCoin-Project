package t_12.backend.service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * Implements TOTP (Time-based One-Time Password) per RFC 6238.
 * Uses HMAC-SHA1, 30-second windows, 6-digit codes.
 * No third-party dependencies — pure Java crypto.
 */
@Service
public class TwoFactorService {

    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    // Allow 1 step before/after to account for clock drift
    private static final int ALLOWED_WINDOW = 1;

    /**
     * Generates a random Base32-encoded TOTP secret for a new user.
     *
     * @return a 20-byte random secret encoded as Base32
     */
    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return base32Encode(buffer);
    }

    /**
     * Builds a TOTP URI for QR code generation.
     * Compatible with Google Authenticator and similar apps.
     *
     * @param secret   the Base32-encoded secret
     * @param username the account username to display in the app
     * @param issuer   the app name (e.g. "CrypMart")
     * @return otpauth:// URI string
     */
    public String buildOtpAuthUri(String secret, String username, String issuer) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                encode(issuer), encode(username), secret, encode(issuer)
        );
    }

    /**
     * Verifies a 6-digit TOTP code against the user's secret.
     * Accepts codes from the current window and one step before/after.
     *
     * @param secret      the Base32-encoded secret stored for the user
     * @param code        the 6-digit code entered by the user
     * @return true if the code is valid
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }

        long currentStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;

        for (int i = -ALLOWED_WINDOW; i <= ALLOWED_WINDOW; i++) {
            String expected = generateCode(secret, currentStep + i);
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
            byte[] data = longToBytes(timeStep);

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            // Dynamic truncation per RFC 4226
            int offset = hash[hash.length - 1] & 0x0F;
            int binary =
                    ((hash[offset]     & 0x7F) << 24) |
                            ((hash[offset + 1] & 0xFF) << 16) |
                            ((hash[offset + 2] & 0xFF) << 8)  |
                            (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return null;
        }
    }

    private byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    // ── Base32 encoding/decoding (RFC 4648) ────────────────────────────────

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_CHARS.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String input) {
        input = input.toUpperCase().replaceAll("=", "");
        byte[] result = new byte[input.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : input.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }

    private String encode(String value) {
        return value.replace(" ", "%20").replace(":", "%3A");
    }
}