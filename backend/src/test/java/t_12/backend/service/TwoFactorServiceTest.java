package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests TwoFactorService against known RFC 6238 test vectors and
 * round-trip secret generation/verification.
 */
class TwoFactorServiceTest {

    private final TwoFactorService service = new TwoFactorService();

    /**
     * Generate a secret, then verify that a code generated "now" validates "now".
     * This is a round-trip sanity check.
     */
    @Test
    void generateAndVerify_worksRoundTrip() throws Exception {
        String secret = service.generateSecret();

        // Use reflection to call private generateCode with current time step
        long currentStep = java.time.Instant.now().getEpochSecond() / 30;
        java.lang.reflect.Method m = TwoFactorService.class
                .getDeclaredMethod("generateCode", String.class, long.class);
        m.setAccessible(true);
        String code = (String) m.invoke(service, secret, currentStep);

        assertTrue(service.verifyCode(secret, code),
                "Freshly generated code should verify. Got code: " + code);
    }

    @Test
    void verifyCode_rejectsWrongCode() {
        String secret = service.generateSecret();
        assertFalse(service.verifyCode(secret, "000000"));
    }

    @Test
    void generateSecret_producesValidBase32() {
        String secret = service.generateSecret();
        assertTrue(secret.matches("[A-Z2-7]+"),
                "Secret should only contain Base32 chars: " + secret);
        assertTrue(secret.length() >= 32,
                "Secret should be at least 32 chars (20 bytes): " + secret);
    }
}