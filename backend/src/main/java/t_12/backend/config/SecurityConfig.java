package t_12.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import t_12.backend.filter.AuthFilter;

/**
 * Spring Security configuration for the application. Configures CSRF
 * protection, CORS, authorization rules, and session management.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthFilter authFilter;

    /**
     * Constructor for SecurityConfig. Injects the custom authentication filter.
     *
     * @param authFilter the authentication filter to use for securing endpoints
     */
    public SecurityConfig(AuthFilter authFilter) {
        this.authFilter = authFilter;
    }

    /**
     * Configures the security filter chain for HTTP requests.
     *
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // Use custom CORS configuration to allow cross-origin requests from the frontend.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .requestMatchers("/api/wallet/**").authenticated()
                .requestMatchers("/api/staking/**").authenticated()
                .requestMatchers("/api/transactions/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/coin/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/coins/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/chain/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/listings/**").permitAll()
                .requestMatchers("/health").permitAll()
                .anyRequest().authenticated()
                )
                .sessionManagement(session
                        -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings. Allows the
     * frontend to make requests to the backend API from different
     * origins/ports.
     *
     * @return a CorsConfigurationSource configured for the application
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow requests from the frontend application only.
        // In production, this should be set to the actual domain (e.g., "https://yourapp.com").
        config.setAllowedOrigins(List.of("http://localhost:3000"));

        // Allow the HTTP methods used by the API.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow all headers in cross-origin requests.
        config.setAllowedHeaders(List.of("*"));

        // Apply CORS configuration to all endpoints.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
