package com.example.todoapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SampleTodoService samples) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/error",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .anyRequest().hasRole("USER"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    String path = request.getServletPath();
                    if (path.isEmpty()) path = request.getRequestURI();
                    if (path.startsWith("/api/") || path.startsWith("/mcp")) {
                        response.sendError(401);
                    } else {
                        new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login")
                                .commence(request, response, exception);
                    }
                }))
                .formLogin(login -> login.loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            samples.initialize(((LoginUser) authentication.getPrincipal()).getId());
                            response.sendRedirect(request.getContextPath() + "/todos");
                        }).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
                .build();
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
