package t_12.backend.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import t_12.backend.service.RateLimiterService;
import t_12.backend.service.RateLimiterService.Policy;

/**
 * Enforces per-endpoint rate limits on DOS-sensitive routes. Runs before
 * AuthFilter so that unauthenticated floods (login brute force, register spam)
 * are rejected without burning JWT parsing cost.
 *
 * Agents are architecturally exempt: they invoke services directly via the
 * scheduler rather than hitting HTTP endpoints, so this filter never sees them.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        ConsumptionProbe probe = rateLimiterService.tryConsume(rule.policy, rule.clientId);
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        sendTooManyRequests(response, retryAfterSeconds);
    }

    /**
     * Returns a RateLimitRule if this request is covered by a rate limit
     * policy, or null if it should pass through unthrottled.
     */
    private RateLimitRule resolveRule(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Auth endpoints — keyed by client IP (no authenticated user yet)
        if ("POST".equalsIgnoreCase(method)) {
            if ("/api/auth/login".equals(path)) {
                return new RateLimitRule(Policy.LOGIN, clientIp(request));
            }
            if ("/api/auth/register".equals(path)) {
                return new RateLimitRule(Policy.REGISTER, clientIp(request));
            }
            if ("/api/auth/refresh".equals(path)) {
                return new RateLimitRule(Policy.REFRESH, clientIp(request));
            }
            if (path.startsWith("/api/auth/2fa/")) {
                return new RateLimitRule(Policy.TFA, clientIp(request));
            }
        }

        // Coin read endpoints — keyed by IP (DOS target)
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/coin")) {
            return new RateLimitRule(Policy.COIN_READ, clientIp(request));
        }

        // Protected transaction endpoints — keyed by user ID when available,
        // fall back to IP if unauthenticated (request will 401 downstream anyway)
        if ("POST".equalsIgnoreCase(method)) {
            if ("/api/coin/buy".equals(path)
                    || "/api/coin/sell".equals(path)
                    || path.startsWith("/api/transactions/")
                    || (path.startsWith("/api/listings/") && path.endsWith("/purchase"))) {
                return new RateLimitRule(Policy.TRANSACTION, clientKey(request));
            }

            if ("/api/mining/mine".equals(path)) {
                return new RateLimitRule(Policy.MINE, clientKey(request));
            }
        }

        return null;
    }

    /**
     * Returns the authenticated user ID from the JWT sub claim if present,
     * otherwise falls back to client IP. Parsed without full validation since
     * AuthFilter will reject bad tokens downstream — we only need a stable key.
     */
    private String clientKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            String userId = extractUserIdUnsafe(token);
            if (userId != null) {
                return "user:" + userId;
            }
        }
        return "ip:" + clientIp(request);
    }

    /**
     * Extracts the subject claim from a JWT without signature verification.
     * Safe because the result is used only as a rate limit bucket key —
     * forging a user ID here just means the attacker shares THAT user's bucket,
     * which doesn't increase their effective rate.
     */
    private String extractUserIdUnsafe(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            int subIdx = payloadJson.indexOf("\"sub\"");
            if (subIdx < 0) return null;
            int colon = payloadJson.indexOf(':', subIdx);
            int quoteStart = payloadJson.indexOf('"', colon + 1);
            int quoteEnd = payloadJson.indexOf('"', quoteStart + 1);
            if (quoteStart < 0 || quoteEnd < 0) return null;
            return payloadJson.substring(quoteStart + 1, quoteEnd);
        } catch (Exception e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendTooManyRequests(HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType("application/json");
        response.getWriter().write(String.format("""
                {
                    "status": 429,
                    "error": "Too Many Requests",
                    "message": "Rate limit exceeded. Retry after %d seconds.",
                    "retryAfterSeconds": %d
                }
                """, retryAfterSeconds, retryAfterSeconds));
    }

    private record RateLimitRule(Policy policy, String clientId) {}
}