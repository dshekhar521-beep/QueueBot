package com.ticketintelligence.ticket_intelligence.config;

import com.ticketintelligence.ticket_intelligence.service.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService
            customUserDetailsService;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService
    ) {
        this.customUserDetailsService =
                customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            PasswordEncoder passwordEncoder
    ) throws Exception {

        http

                // -------------------------------------------------
                // CSRF
                // -------------------------------------------------

                .csrf(csrf ->
                        csrf.disable()
                )

                // -------------------------------------------------
                // USER DETAILS
                // -------------------------------------------------

                .userDetailsService(
                        customUserDetailsService
                )

                // -------------------------------------------------
                // AUTHORIZATION
                // -------------------------------------------------

                .authorizeHttpRequests(
                        auth -> auth

                                // Public pages
                                .requestMatchers(
                                        "/",
                                        "/index.html",
                                        "/login.html",
                                        "/register.html",
                                        "/user-dashboard.html",
                                        "/queuebot.png",
                                        "/css/**",
                                        "/js/**",
                                        "/images/**",
                                        "/uploads/**"
                                )
                                .permitAll()

                                // Public customer registration
                                .requestMatchers(
                                        "/auth/customer/send-otp",
                                        "/auth/customer/register"
                                )
                                .permitAll()

                                // Public seller registration
                                .requestMatchers(
                                        "/auth/seller/send-otp",
                                        "/auth/seller/register"
                                )
                                .permitAll()

                                // Form login
                                .requestMatchers(
                                        "/auth/login"
                                )
                                .permitAll()

                                // Logout
                                .requestMatchers(
                                        "/auth/logout"
                                )
                                .permitAll()

                                // Admin-only APIs
                                .requestMatchers(
                                        "/auth/admin/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "SUPER_ADMIN"
                                )

                                .requestMatchers(
                                        "/api/customers/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "SUPER_ADMIN"
                                )

                                .requestMatchers(
                                        "/api/admins/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "SUPER_ADMIN"
                                )

                                .requestMatchers(
                                        "/api/super-admin/**"
                                )
                                .hasRole(
                                        "SUPER_ADMIN"
                                )

                                // Customer portal
                                .requestMatchers(
                                        "/api/customer/**"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "ADMIN",
                                        "SUPER_ADMIN"
                                )

                                // Customer ticket creation
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/tickets"
                                )
                                .hasRole("USER")

                                // Everything else under API
                                .requestMatchers(
                                        "/api/**"
                                )
                                .authenticated()

                                // Anything else
                                .anyRequest()
                                .authenticated()
                )

                // -------------------------------------------------
                // FORM LOGIN
                // -------------------------------------------------

                .formLogin(
                        form -> form

                                .loginPage(
                                        "/login.html"
                                )

                                .loginProcessingUrl(
                                        "/auth/login"
                                )

                                .usernameParameter(
                                        "username"
                                )

                                .passwordParameter(
                                        "password"
                                )

                                .successHandler(
                                        (
                                                request,
                                                response,
                                                authentication
                                        ) -> {

                                            boolean isCustomer =
                                                    hasRole(
                                                            authentication,
                                                            "ROLE_USER"
                                                    );

                                            if (isCustomer) {

                                                response.sendRedirect(
                                                        "/user-dashboard.html"
                                                );

                                            } else {

                                                response.sendRedirect(
                                                        "/index.html"
                                                );
                                            }
                                        }
                                )

                                .failureUrl(
                                        "/login.html?error=true"
                                )

                                .permitAll()
                )

                // -------------------------------------------------
                // LOGOUT
                // -------------------------------------------------

                .logout(
                        logout -> logout

                                .logoutUrl(
                                        "/auth/logout"
                                )

                                .logoutSuccessUrl(
                                        "/login.html?logout=true"
                                )

                                .invalidateHttpSession(
                                        true
                                )

                                .deleteCookies(
                                        "JSESSIONID"
                                )

                                .permitAll()
                );

        return http.build();
    }

    private boolean hasRole(
            Authentication authentication,
            String role
    ) {

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(
                        authority ->
                                role.equals(
                                        authority.getAuthority()
                                )
                );
    }
}