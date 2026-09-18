package com.ticketintelligence.ticket_intelligence.controller;

import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;

import com.ticketintelligence.ticket_intelligence.repository.UserRepository;
import com.ticketintelligence.ticket_intelligence.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admins")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(
            UserRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<?> getAdmins(
            Authentication authentication) {

        Seller seller =
                getSeller(authentication);

        return ResponseEntity.ok(
                userRepository.findBySellerAndRole(
                        seller,
                        "ROLE_ADMIN"
                )
        );
    }

    @PostMapping
    public ResponseEntity<?> createAdmin(
            @RequestBody CreateAdminRequest request,
            Authentication authentication) {

        try {

            Seller seller =
                    getSeller(authentication);

            if (request.name() == null ||
                    request.name().isBlank()) {

                return ResponseEntity.badRequest()
                        .body("Admin name is required.");
            }

            if (request.phone() == null ||
                    request.phone().isBlank()) {

                return ResponseEntity.badRequest()
                        .body("Admin phone is required.");
            }

            String phone =
                    userService.normalizePhone(
                            request.phone()
                    );

            if (userRepository
                    .existsByPhoneAndSeller(
                            phone,
                            seller
                    )) {

                return ResponseEntity.badRequest()
                        .body(
                                "This phone number is already registered for this seller."
                        );
            }

            User admin = new User();

            admin.setCustomerId(
                    generateAdminId()
            );

            admin.setName(
                    request.name().trim()
            );

            admin.setPhone(phone);

            admin.setEmail(
                    request.email()
            );

            admin.setPassword(
                    passwordEncoder.encode(
                            request.password()
                    )
            );

            admin.setRole("ROLE_ADMIN");
            admin.setSeller(seller);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(
                            userRepository.save(admin)
                    );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @DeleteMapping("/{adminId}")
    public ResponseEntity<?> deactivateAdmin(
            @PathVariable String adminId,
            Authentication authentication) {

        Seller seller =
                getSeller(authentication);

        User admin =
                userRepository
                        .findByCustomerId(adminId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Admin not found."
                                )
                        );

        if (!admin.getSeller()
                .getId()
                .equals(seller.getId())) {

            return ResponseEntity.status(
                    HttpStatus.FORBIDDEN
            ).body(
                    "Admin belongs to another seller."
            );
        }

        /*
         * Keep the record for ticket history.
         */
        admin.setRole("ROLE_DISABLED");

        userRepository.save(admin);

        return ResponseEntity.ok(
                "Admin deactivated successfully."
        );
    }

    private Seller getSeller(
            Authentication authentication) {

        String[] parts =
                authentication.getName()
                        .split("\\|", 2);

        if (parts.length != 2) {

            throw new IllegalArgumentException(
                    "Invalid authenticated user."
            );
        }

        return userService.findSeller(parts[0]);
    }

    private String generateAdminId() {

        return "ADMIN-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 6)
                        .toUpperCase();
    }

    public record CreateAdminRequest(
            String name,
            String phone,
            String email,
            String password
    ) {
    }
}
