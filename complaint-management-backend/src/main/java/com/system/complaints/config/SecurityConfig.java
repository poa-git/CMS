package com.system.complaints.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    @Value("${CORS_ALLOWED_ORIGINS}")
    private String corsAllowedOrigins;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * This bean makes Spring respect X-Forwarded-Proto, X-Forwarded-Host, etc.
     * headers sent by Railway's reverse proxy, so redirects use https:// instead of http://
     */
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
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

                // 3) Trust forwarded headers from Railway's proxy (fixes http -> https redirect issue)
                .requestCache(cache -> cache.requestCache(new org.springframework.security.web.savedrequest.HttpSessionRequestCache()))

                // 4) Configure authorization rules
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

                // 5) Form Login config
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // 6) Logout config
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.getWriter().write("Logged out successfully");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )

                // 7) Session Management
                .sessionManagement(session -> session
                        .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                        .invalidSessionUrl("/login?session=expired")
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(-1)
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
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
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