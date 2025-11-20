package com.bookvexe.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Apply CORS configuration globally to all endpoints (/**)
        registry.addMapping("/**")
            // 1. Specify the exact origin of your front-end application
            // This replaces the unsafe wildcard '*' and resolves the error.
            .allowedOrigins("http://localhost:5173")

            // 2. Allow all necessary HTTP methods
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")

            // 3. Crucially, allow credentials for SockJS/WebSocket and safer use of cookies/Auth headers
            // This is mandatory when allowedOrigins is not '*'
            .allowCredentials(true)

            // 4. Optionally specify allowed request headers
            .allowedHeaders("*")

            // 5. Optionally specify response headers to expose to the browser
            .exposedHeaders("Authorization");
    }
}