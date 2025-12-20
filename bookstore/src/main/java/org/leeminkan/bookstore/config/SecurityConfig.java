package org.leeminkan.bookstore.config;

import org.leeminkan.bookstore.security.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // Inject the filter bean we just created
    @Autowired
    private AuthTokenFilter authTokenFilter;

    // Add this method to define the PasswordEncoder Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for REST APIs (for now)
                .authorizeHttpRequests(authorize -> authorize// ADD THIS LINE: Allow POST to the registration endpoint
                        // MODIFIED LINE: Permit access to all /auth/** endpoints (POST and GET)
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll() // Allow to GET requests
                        .anyRequest().authenticated() // All other requests must be authenticated
                )
                // FINAL CRITICAL STEP: Add our custom JWT filter to the chain
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class); // Inject before built-in auth

        return http.build();
    }
}