package com.example.sana.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@org.springframework.context.annotation.Configuration
@EnableWebSecurity
public class Configuration {

    // New passwords are saved as BCrypt ({bcrypt}...).
    // Passwords saved before security was added have no {id} prefix: they are checked as plain text once
    // and MyUserDetailsService saves them again as BCrypt after that login.
    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        DelegatingPasswordEncoder encoder = (DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
        encoder.setDefaultPasswordEncoderForMatches(NoOpPasswordEncoder.getInstance());
        return encoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf
                        .disable()
                )

                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .authorizeHttpRequests(auth -> auth
                                // The React frontend files (built into static/) load before the user logs in
                                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/assets/**", "/*.png", "/*.webp", "/*.svg").permitAll()
                                // Anyone can create an account
                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()

                                // n8n is not a user: N8nCallbackFilter checks its own X-Sana-Callback-Key
                                .requestMatchers(HttpMethod.POST, "/api/integrations/n8n/ai/callback").permitAll()

                                // Payment addition: Moyasar notifications do not use user login.
                                // BillingService verifies the callback token and retrieves the invoice from Moyasar.
                                .requestMatchers(HttpMethod.POST, "/api/billing/callbacks/*/*").permitAll()

                                // Lists of every user's data and the old generic CRUD are for admins only
                                .requestMatchers("/api/v1/user/**").hasAuthority("ADMIN")
                                .requestMatchers("/api/v1/ai-interaction/**", "/api/v1/change-proposal/**", "/api/v1/change-proposal-item/**", "/api/v1/tool-run/**").hasAuthority("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/v1/project/get", "/api/v1/project-output/get", "/api/v1/node/get", "/api/v1/edge/get",
                                        "/api/v1/record/get", "/api/v1/project-understanding/get", "/api/v1/tech/architecture/get",
                                        "/api/v1/character-profile/get", "/api/v1/world/get", "/api/v1/location/get", "/api/v1/world-rule/get", "/api/v1/story-event/get").hasAuthority("ADMIN")

                                // Everything else needs a login; controllers check that the data belongs to the user
                                .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
                )

                // Answer 401 with JSON and no WWW-Authenticate header, so the browser shows the frontend's login screen
                // instead of its own password popup
                .httpBasic(basic -> basic
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Wrong email or password, or not logged in\"}");
                        })
                )

                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"You are not allowed to do this\"}");
                        })
                )

                .build();
    }
}
