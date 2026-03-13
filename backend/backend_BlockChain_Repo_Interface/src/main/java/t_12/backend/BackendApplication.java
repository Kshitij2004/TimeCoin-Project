package t_12.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Spring Boot backend service.
 */
@SpringBootApplication
public class BackendApplication {

    /**
     * Starts the Spring application context and embedded web server.
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
