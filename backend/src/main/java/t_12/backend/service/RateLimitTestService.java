package t_12.backend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Fires rapid burst requests against a configurable endpoint to validate
 * that rate limiting correctly blocks excessive traffic. Disabled by default
 * and should never run in production.
 *
 * Logs a summary after each burst showing how many requests succeeded (200)
 * vs were rejected (429) vs failed for other reasons.
 */
@Service
public class RateLimitTestService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitTestService.class);

    private final HttpClient httpClient;

    @Value("${ratelimit.test.enabled:false}")
    private boolean enabled;

    @Value("${ratelimit.test.target-url:http://localhost:8080/api/coin}")
    private String targetUrl;

    @Value("${ratelimit.test.burst-size:100}")
    private int burstSize;

    public RateLimitTestService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Fires a burst of requests on the configured interval. Each burst
     * sends burstSize requests as fast as possible and logs the results.
     */
    @Scheduled(fixedDelayString = "${ratelimit.test.delay-between-bursts-ms:60000}")
    public void fireBurst() {
        if (!enabled) {
            return;
        }

        log.info("Starting rate limit test burst: {} requests to {}", burstSize, targetUrl);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger rateLimited = new AtomicInteger(0);
        AtomicInteger otherErrors = new AtomicInteger(0);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        for (int i = 0; i < burstSize; i++) {
            try {
                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                if (status == 200) {
                    success.incrementAndGet();
                } else if (status == 429) {
                    rateLimited.incrementAndGet();
                } else {
                    otherErrors.incrementAndGet();
                    log.debug("Request {} returned unexpected status: {}", i + 1, status);
                }
            } catch (Exception ex) {
                otherErrors.incrementAndGet();
                log.debug("Request {} failed: {}", i + 1, ex.getMessage());
            }
        }

        log.info("Rate limit test complete: {} succeeded, {} rate-limited (429), {} errors out of {} total",
                success.get(), rateLimited.get(), otherErrors.get(), burstSize);

        if (rateLimited.get() == 0) {
            log.warn("No requests were rate-limited. Is rate limiting configured?");
        }
    }
}