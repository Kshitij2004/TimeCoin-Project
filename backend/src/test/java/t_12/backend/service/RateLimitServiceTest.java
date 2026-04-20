package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.bucket4j.ConsumptionProbe;
import t_12.backend.service.RateLimiterService.Policy;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        // use small limits so tests don't need to loop thousands of times
        rateLimiterService = new RateLimiterService(
                3,  // login
                2,  // register
                5,  // refresh
                3,  // tfa
                4,  // transaction
                5,  // coin-read
                5   // mine
        );
    }

    @Test
    void underLimit_allowsAllRequests() {
        for (int i = 0; i < 3; i++) {
            ConsumptionProbe probe = rateLimiterService.tryConsume(Policy.LOGIN, "1.2.3.4");
            assertTrue(probe.isConsumed(), "request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void overLimit_rejectsRequest() {
        // exhaust bucket
        for (int i = 0; i < 3; i++) {
            rateLimiterService.tryConsume(Policy.LOGIN, "1.2.3.4");
        }

        ConsumptionProbe probe = rateLimiterService.tryConsume(Policy.LOGIN, "1.2.3.4");
        assertFalse(probe.isConsumed(), "4th request should be rejected");
        assertTrue(probe.getNanosToWaitForRefill() > 0,
                "rejected probe should include wait time for retry");
    }

    @Test
    void differentClients_haveIndependentBuckets() {
        // client A exhausts their bucket
        for (int i = 0; i < 3; i++) {
            rateLimiterService.tryConsume(Policy.LOGIN, "1.1.1.1");
        }
        ConsumptionProbe clientABlocked = rateLimiterService.tryConsume(Policy.LOGIN, "1.1.1.1");
        assertFalse(clientABlocked.isConsumed());

        // client B should still have full capacity
        ConsumptionProbe clientBAllowed = rateLimiterService.tryConsume(Policy.LOGIN, "2.2.2.2");
        assertTrue(clientBAllowed.isConsumed());
    }

    @Test
    void differentPolicies_haveIndependentBuckets() {
        // exhaust register for this client
        for (int i = 0; i < 2; i++) {
            rateLimiterService.tryConsume(Policy.REGISTER, "1.2.3.4");
        }
        assertFalse(rateLimiterService.tryConsume(Policy.REGISTER, "1.2.3.4").isConsumed());

        // same client on login should still have capacity
        assertTrue(rateLimiterService.tryConsume(Policy.LOGIN, "1.2.3.4").isConsumed());
    }

    @Test
    void transactionPolicy_respectsConfiguredCapacity() {
        for (int i = 0; i < 4; i++) {
            assertTrue(rateLimiterService.tryConsume(Policy.TRANSACTION, "user:42").isConsumed());
        }
        assertFalse(rateLimiterService.tryConsume(Policy.TRANSACTION, "user:42").isConsumed());
    }
}