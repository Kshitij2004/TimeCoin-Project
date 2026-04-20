package t_12.backend.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

/**
 * In-memory rate limit bucket registry. Buckets are created on first access
 * and reused thereafter. Keys are of the form "{policy}:{clientId}" where
 * policy is one of LOGIN, REGISTER, REFRESH, TFA, TRANSACTION, COIN_READ,
 * MINE and clientId is either an IP address or a user ID depending on policy.
 *
 * Limits are configurable via application.properties.
 */
@Service
public class RateLimiterService {

    /** Bucket policy categories. Each has its own capacity/refill config. */
    public enum Policy {
        LOGIN, REGISTER, REFRESH, TFA, TRANSACTION, COIN_READ, MINE
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final int loginCapacity;
    private final int registerCapacity;
    private final int refreshCapacity;
    private final int tfaCapacity;
    private final int transactionCapacity;
    private final int coinReadCapacity;
    private final int mineCapacity;

    public RateLimiterService(
            @Value("${ratelimit.login.per-minute:10}") int loginCapacity,
            @Value("${ratelimit.register.per-minute:5}") int registerCapacity,
            @Value("${ratelimit.refresh.per-minute:20}") int refreshCapacity,
            @Value("${ratelimit.tfa.per-minute:10}") int tfaCapacity,
            @Value("${ratelimit.transaction.per-minute:30}") int transactionCapacity,
            @Value("${ratelimit.coin-read.per-minute:60}") int coinReadCapacity,
            @Value("${ratelimit.mine.per-minute:60}") int mineCapacity) {
        this.loginCapacity = loginCapacity;
        this.registerCapacity = registerCapacity;
        this.refreshCapacity = refreshCapacity;
        this.tfaCapacity = tfaCapacity;
        this.transactionCapacity = transactionCapacity;
        this.coinReadCapacity = coinReadCapacity;
        this.mineCapacity = mineCapacity;
    }

    /**
     * Attempts to consume one token from the bucket identified by policy + clientId.
     * Returns a ConsumptionProbe describing whether the request was allowed and,
     * if not, how long until the next token is available.
     */
    public ConsumptionProbe tryConsume(Policy policy, String clientId) {
        String key = policy.name() + ":" + clientId;
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(policy));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket newBucket(Policy policy) {
        int capacity = capacityFor(policy);
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private int capacityFor(Policy policy) {
        return switch (policy) {
            case LOGIN -> loginCapacity;
            case REGISTER -> registerCapacity;
            case REFRESH -> refreshCapacity;
            case TFA -> tfaCapacity;
            case TRANSACTION -> transactionCapacity;
            case COIN_READ -> coinReadCapacity;
            case MINE -> mineCapacity;
        };
    }

    /**
     * For testing only — clears all buckets so each test starts fresh.
     */
    void resetForTests() {
        buckets.clear();
    }
}