package t_12.backend.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

    /**
     * Filters incoming HTTP requests to enforce authorization requirements.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain to proceed with
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Public endpoints (auth routes) bypass authorization checks.
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // Protected endpoints require a valid Authorization header.
        if (authHeader == null || authHeader.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "status": 401,
                    "error": "Unauthorized",
                    "message": "Missing Authorization header"
                }
            """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
