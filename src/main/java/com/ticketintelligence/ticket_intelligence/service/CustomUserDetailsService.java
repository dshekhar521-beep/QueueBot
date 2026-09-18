package com.ticketintelligence.ticket_intelligence.service;

import com.ticketintelligence.ticket_intelligence.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Username cannot be empty");
        }

        username = username.trim();

        User user = null;

        /*
         * Seller/Admin login format:
         *
         * SELLER-2202|PHONE
         * SELLER-2202|EMAIL
         *
         * Example:
         * SELLER-2202|9876543210
         * SELLER-2202|admin@gmail.com
         */
        if (username.contains("|")) {

            String[] parts = username.split("\\|", 2);

            if (parts.length == 2) {

                String sellerId = parts[0].trim();
                String identifier = parts[1].trim();

                if (!sellerId.isBlank() && !identifier.isBlank()) {

                    user = userService.findByLoginIdentifier(
                            sellerId,
                            identifier
                    );
                }
            }
        }

        /*
         * Customer login using Customer ID
         *
         * Example:
         * CUST-1001
         */
        if (user == null &&
                username.toUpperCase().startsWith("CUST-")) {

            user = userService.findByCustomerId(username);
        }

        /*
         * Fallback: login using email
         */
        if (user == null && username.contains("@")) {

            user = userService.findByEmail(username);
        }

        /*
         * Fallback: login using phone
         */
        if (user == null) {

            user = userService.findByPhone(username);
        }

        /*
         * User not found
         */
        if (user == null) {

            throw new UsernameNotFoundException(
                    "User not found: " + username
            );
        }

        /*
         * Check account status
         */
        if (!user.isActive()) {

            throw new UsernameNotFoundException(
                    "User account is inactive: " + username
            );
        }

        /*
         * Check password
         */
        if (user.getPassword() == null ||
                user.getPassword().isBlank()) {

            throw new UsernameNotFoundException(
                    "User password is not configured: " + username
            );
        }

        /*
         * Get and normalize role
         */
        String role = user.getRole();

        if (role == null || role.isBlank()) {
            role = "USER";
        }

        role = role.trim().toUpperCase();

        /*
         * Remove ROLE_ prefix if it already exists.
         *
         * Spring Security's .roles() method
         * automatically adds ROLE_.
         */
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        /*
         * IMPORTANT:
         *
         * The authenticated username is the User's
         * customerId.
         *
         * Example:
         *
         * User:
         * customerId = ADMIN-2202
         * seller = SELLER-2202
         *
         * After authentication:
         *
         * authentication.getName()
         *        ↓
         * ADMIN-2202
         *
         * CustomerController can then find the
         * administrator using customerId and obtain
         * the linked Seller.
         */
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getCustomerId())
                .password(user.getPassword())
                .roles(role)
                .disabled(!user.isActive())
                .build();
    }
}