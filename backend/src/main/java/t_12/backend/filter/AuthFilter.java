package t_12.backend.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP filter that enforces authorization for protected endpoints. Allows
 * public endpoints (auth routes) to pass through without checking credentials.
 * For all other endpoints, requires a valid Authorization header to be present.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Public endpoints bypass authorization checks.
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // Protected endpoints require an Authorization header.
        if (authHeader == null || authHeader.isBlank()) {
            sendUnauthorized(response, "Missing Authorization header");
            return;
        }

        // The header must follow the format: "Bearer <token>"
        if (!authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Invalid Authorization format");
            return;
        }

        // Strip the "Bearer " prefix to get the raw token string.
        String token = authHeader.substring(7);

        try {
            // This both parses AND validates the token in one call.
            // It will throw an exception if the signature is wrong,
            // the token is malformed, or the token has expired.
            String secretKey = "your-super-secret-key-change-this-in-production";
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Integer userId = Integer.parseInt(claims.getSubject());

            // Store the user ID in the SecurityContext so downstream code
            // (controllers, services) can access it without re-parsing the token.
            // The null arguments are credentials and authorities. They're not needed
            // here since we handle authorization ourselves in the service layer.
            UsernamePasswordAuthenticationToken authentication
                    = new UsernamePasswordAuthenticationToken(userId, null, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Token is valid, let the request through.
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // Any exception here means the token is invalid or expired.
            sendUnauthorized(response, "Invalid or expired token");
        }
    }

    /**
     * Writes a 401 Unauthorized JSON response.
     *
     * @param response the HTTP response to write to
     * @param message the error message to include
     */
    private void sendUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("""
        {
            "status": 401,
            "error": "Unauthorized",
            "message": "%s"
        }
    """, message));
    }
}
