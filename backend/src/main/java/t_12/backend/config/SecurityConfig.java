package t_12.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// @Configuration tells Spring this class contains setup/configuration "beans."
// (which--what the hell is a bean?)
// @EnableWebSecurity tells Spring Security to use our custom config
// instead of its default behavior (the auto-generated password etc).
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // A SecurityFilterChain is the object that defines the security rules.
    // Every incoming HTTP request passes through this chain before
    // reaching any controller.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF protection. CSRF is important for browser-based apps
            // but REST APIs use tokens instead, so it would just block our POST requests.
            .csrf(csrf -> csrf.disable())

            // Define which endpoints are allowed and which require authentication.
            .authorizeHttpRequests(auth -> auth
                // Allow /api/auth/register freely — no login needed to sign up.
                .requestMatchers("/api/auth/register").permitAll()
                // Allow existing endpoints through for now so we don't break anything.
                .requestMatchers("/api/wallet/**").permitAll()
                .requestMatchers("/api/coin/**").permitAll()
                .requestMatchers("/health").permitAll()
                // Everything else requires authentication (for future sprints).
                .anyRequest().authenticated()
            )

            // Use stateless sessions — the server won't store session data.
            // REST APIs typically authenticate per-request via tokens, not sessions.
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}