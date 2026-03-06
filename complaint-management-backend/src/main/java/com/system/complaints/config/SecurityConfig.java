package com.system.complaints.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    // Comes from application.properties or environment variable
    @Value("${CORS_ALLOWED_ORIGINS}")
    private String corsAllowedOrigins;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Provide the AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Main SecurityFilterChain with Form Login (HTTP Session).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 1) Disable CSRF for simplicity (only if suitable for your use case!)
                .csrf(csrf -> csrf.disable())

                // 2) Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3) Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Permit the main index and static resources
                        .requestMatchers("/", "/index.html", "/static/**").permitAll()
                        // Permit all OPTIONS requests (for CORS preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Permit GET on the custom login page
                        .requestMatchers("/login").permitAll()
                        // Protect admin endpoints
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        // Protect user endpoints
                        .requestMatchers("/user/**").hasAnyAuthority("USER", "ADMIN")
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // 4) Form Login config
                .formLogin(form -> form
                        .loginPage("/login")                  // The GET page for login
                        .loginProcessingUrl("/perform_login") // The POST endpoint for credentials
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // 5) Logout config
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.getWriter().write("Logged out successfully");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )

                // 6) Session Management
                .sessionManagement(session -> session
                        .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                        .invalidSessionUrl("/login?session=expired")
                        .sessionConcurrency(concurrency -> concurrency
                                // -1 allows unlimited concurrent sessions for the same user.
                                .maximumSessions(-1)
                                // If set to true, further logins beyond the max would be rejected.
                                // If set to false, the oldest session will be invalidated to allow a new one.
                                .maxSessionsPreventsLogin(false)
                        )
                )

                // Optional: allow HTTP Basic for debugging
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }



    /**
     * Basic CORS configuration to allow your React app (or other front-ends) to talk to this API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Split your allowed origins from your application.properties
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true); // crucial if you're sending session cookies
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply it to all endpoints
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Use plain-text passwords (NoOp) for testing only.
     * In production, use BCrypt or another secure encoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
