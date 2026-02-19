package com.cs506.TimeCoin; 

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstraps the Sping Boot application.
 * When this runs, it starts an embedded web server (Tomcat) on port 8080 by default.
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}