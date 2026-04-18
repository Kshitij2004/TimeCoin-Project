package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RateLimitTestServiceTest {

    private RateLimitTestService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitTestService();
    }

    @Test
    void fireBurst_disabled_doesNothing() {
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "targetUrl", "http://localhost:9999/fake");
        ReflectionTestUtils.setField(service, "burstSize", 10);

        // should return immediately without making any requests
        assertDoesNotThrow(() -> service.fireBurst());
    }

    @Test
    void fireBurst_enabled_handlesConnectionRefused() {
        ReflectionTestUtils.setField(service, "enabled", true);
        // target a port nothing is listening on
        ReflectionTestUtils.setField(service, "targetUrl", "http://localhost:19999/nonexistent");
        ReflectionTestUtils.setField(service, "burstSize", 3);

        // should not throw, all requests count as otherErrors
        assertDoesNotThrow(() -> service.fireBurst());
    }

    @Test
    void fireBurst_enabled_smallBurst_completes() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "targetUrl", "http://localhost:19999/fake");
        ReflectionTestUtils.setField(service, "burstSize", 5);

        assertDoesNotThrow(() -> service.fireBurst());
    }
}