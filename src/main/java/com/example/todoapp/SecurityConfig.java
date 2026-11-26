package com.example.todoapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Keep the existing public app accessible, with CSRF protection enabled.
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
    }
}
