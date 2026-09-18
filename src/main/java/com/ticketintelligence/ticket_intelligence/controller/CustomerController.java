package com.ticketintelligence.ticket_intelligence.controller;

import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class CustomerController {

    private final UserService userService;

    public CustomerController(UserService userService) {
        this.userService = userService;
    }

    // ============================================================
    // GET ALL CUSTOMERS FOR LOGGED-IN SELLER
    // ============================================================

    @GetMapping
    public ResponseEntity<?> getCustomers(Authentication authentication) {

        try {
            Seller seller = resolveSeller(authentication);

            List<User> customers =
                    userService.getCustomers(seller);

            return ResponseEntity.ok(customers);

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ============================================================
    // GET CUSTOMER BY CUSTOMER ID
    // ============================================================

    @GetMapping("/{customerId}")
    public ResponseEntity<?> getCustomer(
            @PathVariable String customerId,
            Authentication authentication) {

        try {

            Seller seller = resolveSeller(authentication);

            User customer =
                    userService.getCustomerForSeller(
                            customerId,
                            seller
                    );

            return ResponseEntity.ok(customer);

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // ============================================================
    // CREATE CUSTOMER FOR LOGGED-IN SELLER
    // ============================================================

    @PostMapping
    public ResponseEntity<?> createCustomer(
            @RequestBody User customer,
            Authentication authentication) {

        try {

            Seller seller = resolveSeller(authentication);

            User createdCustomer =
                    userService.createCustomerForSeller(
                            seller,
                            customer.getName(),
                            customer.getPhone(),
                            customer.getEmail(),
                            customer.getPassword()
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(createdCustomer);

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ============================================================
    // UPDATE CUSTOMER
    // ============================================================

    @PutMapping("/{customerId}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable String customerId,
            @RequestBody User incoming,
            Authentication authentication) {

        try {

            Seller seller = resolveSeller(authentication);

            User customer =
                    userService.getCustomerForSeller(
                            customerId,
                            seller
                    );

            if (incoming.getName() != null &&
                    !incoming.getName().isBlank()) {

                customer.setName(
                        incoming.getName().trim()
                );
            }

            if (incoming.getPhone() != null &&
                    !incoming.getPhone().isBlank()) {

                customer.setPhone(
                        incoming.getPhone().trim()
                );
            }

            if (incoming.getEmail() != null &&
                    !incoming.getEmail().isBlank()) {

                customer.setEmail(
                        incoming.getEmail().trim().toLowerCase()
                );
            }

            User updated =
                    userService.save(customer);

            return ResponseEntity.ok(updated);

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ============================================================
    // DELETE / DEACTIVATE CUSTOMER
    // ============================================================

    @DeleteMapping("/{customerId}")
    public ResponseEntity<?> deleteCustomer(
            @PathVariable String customerId,
            Authentication authentication) {

        try {

            Seller seller = resolveSeller(authentication);

            User customer =
                    userService.getCustomerForSeller(
                            customerId,
                            seller
                    );

            customer.setActive(false);

            userService.save(customer);

            return ResponseEntity.ok(
                    "Customer deactivated successfully."
            );

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ============================================================
    // RESOLVE SELLER FROM LOGGED-IN ADMIN
    // ============================================================

    private Seller resolveSeller(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "No authenticated administrator."
            );
        }

        String username =
                authentication.getName();

        // --------------------------------------------------------
        // CURRENT LOGIN SYSTEM
        //
        // authentication.getName()
        // = ADMIN-2958
        //
        // Find the logged-in User using customerId,
        // then get that user's Seller.
        // --------------------------------------------------------

        User admin =
                userService.findByCustomerId(username);

        if (admin != null &&
                admin.getSeller() != null) {

            return admin.getSeller();
        }

        // --------------------------------------------------------
        // LEGACY COMPATIBILITY
        //
        // Old format:
        // SELLER-XXXX|phone
        // SELLER-XXXX|email
        // --------------------------------------------------------

        if (username != null &&
                username.contains("|")) {

            String[] parts =
                    username.split("\\|", 2);

            if (parts.length == 2 &&
                    !parts[0].isBlank()) {

                return userService.findSeller(
                        parts[0].trim()
                );
            }
        }

        // --------------------------------------------------------
        // OLDER PHONE / EMAIL LOGIN COMPATIBILITY
        // --------------------------------------------------------

        admin =
                userService.findByPhone(username);

        if (admin == null) {
            admin =
                    userService.findByEmail(username);
        }

        if (admin != null &&
                admin.getSeller() != null) {

            return admin.getSeller();
        }

        throw new IllegalStateException(
                "Could not determine seller from logged-in administrator."
        );
    }
}