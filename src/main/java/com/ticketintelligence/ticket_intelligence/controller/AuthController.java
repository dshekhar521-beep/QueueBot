package com.ticketintelligence.ticket_intelligence.controller;

import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.service.OtpService;
import com.ticketintelligence.ticket_intelligence.service.SellerService;
import com.ticketintelligence.ticket_intelligence.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final SellerService sellerService;
    private final OtpService otpService;

    public AuthController(
            UserService userService,
            SellerService sellerService,
            OtpService otpService
    ) {
        this.userService = userService;
        this.sellerService = sellerService;
        this.otpService = otpService;
    }

    // ============================================================
    // CUSTOMER SEND OTP
    // NEW CUSTOMERS ARE ALLOWED
    // ============================================================

    @PostMapping("/customer/send-otp")
    public ResponseEntity<?> sendCustomerOtp(
            @RequestBody OtpRequest request
    ) {

        try {

            String identifier = firstNonBlank(
                    request.phone(),
                    request.email()
            );

            if (identifier == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Phone or Email is required."
                        )
                );
            }

            /*
             * A customer may register before buying anything.
             * Therefore OTP generation must NOT require an
             * existing customer record or a seller link.
             */
            String otpIdentifier =
                    "customer:" + identifier.trim();

            String otp =
                    otpService.generateOtp(otpIdentifier);

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("success", true);
            response.put(
                    "message",
                    "OTP generated successfully."
            );
            response.put("otp", otp);
            response.put("expiresInMinutes", 5);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", safeMessage(e)
                    )
            );
        }
    }

    // ============================================================
    // CUSTOMER REGISTER
    // ============================================================
    //
    // Uses the EXISTING seller-created customer record.
    // It does NOT create a second User row.
    // ============================================================

    @PostMapping("/customer/register")
    public ResponseEntity<?> registerCustomer(
            @RequestBody CustomerRegisterRequest request
    ) {

        try {

            if (isBlank(request.phone()) &&
                    isBlank(request.email())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Phone or Email is required."
                        )
                );
            }

            if (isBlank(request.password())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Password is required."
                        )
                );
            }

            if (request.password().length() < 6) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Password must contain at least 6 characters."
                        )
                );
            }

            if (isBlank(request.otp())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "OTP is required."
                        )
                );
            }

            String identifier =
                    firstNonBlank(
                            request.phone(),
                            request.email()
                    );

            String otpIdentifier =
                    "customer:" + identifier.trim();

            boolean otpValid =
                    otpService.verifyOtp(
                            otpIdentifier,
                            request.otp().trim()
                    );

            if (!otpValid) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Invalid or expired OTP."
                        )
                );
            }

            // ----------------------------------------------------
            // IMPORTANT FIX
            // This updates the existing customer and preserves
            // seller_id.
            // ----------------------------------------------------

            User customer =
                    userService.registerCustomer(
                            request.name(),
                            request.phone(),
                            request.email(),
                            request.password(),
                            request.sellerId()
                    );

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("success", true);

            response.put(
                    "message",
                    "Customer account created successfully."
            );

            response.put(
                    "customerId",
                    customer.getCustomerId()
            );

            response.put(
                    "name",
                    customer.getName()
            );

            response.put(
                    "phone",
                    customer.getPhone()
            );

            response.put(
                    "email",
                    customer.getEmail()
            );

            if (customer.getSeller() != null) {

                response.put(
                        "sellerId",
                        customer.getSeller().getSellerId()
                );

                response.put(
                        "businessName",
                        customer.getSeller().getBusinessName()
                );
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", safeMessage(e)
                    )
            );
        }
    }

    // ============================================================
    // SELLER SEND OTP
    // ============================================================

    @PostMapping("/seller/send-otp")
    public ResponseEntity<?> sendSellerOtp(
            @RequestBody SellerOtpRequest request
    ) {

        try {

            if (isBlank(request.phone())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Phone number is required."
                        )
                );
            }

            String phone =
                    request.phone().trim();

            String otpIdentifier =
                    "seller:" + phone;

            String otp =
                    otpService.generateOtp(
                            otpIdentifier
                    );

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("success", true);

            response.put(
                    "message",
                    "Seller OTP generated successfully."
            );

            response.put("otp", otp);

            response.put(
                    "expiresInMinutes",
                    5
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", safeMessage(e)
                    )
            );
        }
    }

    // ============================================================
    // SELLER REGISTER
    // ============================================================

    @PostMapping("/seller/register")
    public ResponseEntity<?> registerSeller(
            @RequestBody SellerRegisterRequest request
    ) {

        try {

            if (isBlank(request.businessName())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Business name is required."
                        )
                );
            }

            if (isBlank(request.ownerName())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Owner name is required."
                        )
                );
            }

            if (isBlank(request.phone())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Phone number is required."
                        )
                );
            }

            if (isBlank(request.email())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Email is required."
                        )
                );
            }

            if (isBlank(request.password())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Password is required."
                        )
                );
            }

            if (request.password().length() < 6) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Password must contain at least 6 characters."
                        )
                );
            }

            if (isBlank(request.otp())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Seller OTP is required."
                        )
                );
            }

            String phone =
                    request.phone().trim();

            boolean otpValid =
                    otpService.verifyOtp(
                            "seller:" + phone,
                            request.otp().trim()
                    );

            if (!otpValid) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Invalid or expired seller OTP."
                        )
                );
            }

            // ----------------------------------------------------
            // IMPORTANT FIX:
            // registerSeller() returns SellerRegistrationResult,
            // not Seller directly.
            // ----------------------------------------------------

            SellerService.SellerRegistrationResult
                    registrationResult =
                    sellerService.registerSeller(
                            request.businessName(),
                            request.ownerName(),
                            phone,
                            request.email(),
                            request.password()
                    );

            Seller seller =
                    registrationResult.seller();

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    "Seller account created successfully."
            );

            response.put(
                    "sellerId",
                    seller.getSellerId()
            );

            response.put(
                    "businessName",
                    seller.getBusinessName()
            );

            response.put(
                    "phone",
                    seller.getPhone()
            );

            response.put(
                    "email",
                    seller.getEmail()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", safeMessage(e)
                    )
            );
        }
    }

    // ============================================================
    // ADMIN CREATE CUSTOMER
    // ============================================================

    @PostMapping("/admin/customers")
    public ResponseEntity<?> createCustomerForAdmin(
            @RequestBody AdminCustomerRequest request,
            Authentication authentication
    ) {

        try {

            Seller seller =
                    resolveSellerFromAuthentication(
                            authentication
                    );

            User customer =
                    userService.createCustomerForSeller(
                            seller,
                            request.name(),
                            request.phone(),
                            request.email(),
                            null
                    );

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    "Customer created successfully."
            );

            response.put(
                    "customerId",
                    customer.getCustomerId()
            );

            response.put(
                    "sellerId",
                    seller.getSellerId()
            );

            response.put(
                    "businessName",
                    seller.getBusinessName()
            );

            response.put(
                    "name",
                    customer.getName()
            );

            response.put(
                    "phone",
                    customer.getPhone()
            );

            response.put(
                    "email",
                    customer.getEmail()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", safeMessage(e)
                    )
            );
        }
    }

    // ============================================================
    // CURRENT USER
    // ============================================================

    // ============================================================
    // CURRENT USER
    // ============================================================

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication
    ) {

        try {

            if (authentication == null ||
                    !authentication.isAuthenticated()) {

                return ResponseEntity.status(
                        HttpStatus.UNAUTHORIZED
                ).body(
                        Map.of(
                                "success", false,
                                "message", "Not authenticated."
                        )
                );
            }

            String username =
                    authentication.getName();

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("success", true);
            response.put("username", username);


            // ----------------------------------------------------
            // NEW CUSTOMER SESSION
            //
            // Customer logs in with:
            // PHONE/EMAIL + PASSWORD
            //
            // CustomUserDetailsService stores the generated
            // Customer ID as the authenticated username:
            //
            // CUST-XXXX
            // ----------------------------------------------------

            if (username != null &&
                    username.trim()
                            .toUpperCase()
                            .startsWith("CUST-")) {

                User customer =
                        userService.findByCustomerId(
                                username.trim()
                        );


                if (customer == null) {

                    return ResponseEntity.status(
                            HttpStatus.UNAUTHORIZED
                    ).body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    "Customer account could not be resolved."
                            )
                    );
                }


                response.put(
                        "customerId",
                        customer.getCustomerId()
                );

                response.put(
                        "name",
                        customer.getName()
                );

                response.put(
                        "phone",
                        customer.getPhone()
                );

                response.put(
                        "email",
                        customer.getEmail()
                );

                response.put(
                        "role",
                        customer.getRole()
                );


                // A newly registered customer may not have
                // a seller yet.

                if (customer.getSeller() != null) {

                    response.put(
                            "sellerId",
                            customer.getSeller()
                                    .getSellerId()
                    );

                    response.put(
                            "businessName",
                            customer.getSeller()
                                    .getBusinessName()
                    );

                } else {

                    response.put(
                            "sellerId",
                            null
                    );

                    response.put(
                            "businessName",
                            null
                    );
                }


                response.put(
                        "authorities",
                        authentication
                                .getAuthorities()
                                .stream()
                                .map(
                                        GrantedAuthority::getAuthority
                                )
                                .toList()
                );


                return ResponseEntity.ok(
                        response
                );
            }


            // ----------------------------------------------------
            // SELLER / ADMIN SESSION
            //
            // Expected authenticated username:
            //
            // SELLER-XXXX|PHONE
            // SELLER-XXXX|EMAIL
            //
            // This keeps seller/admin login compatible.
            // ----------------------------------------------------

            if (username != null &&
                    username.contains("|")) {

                String[] parts =
                        username.split(
                                "\\|",
                                2
                        );


                if (parts.length == 2) {

                    String accountId =
                            parts[0].trim();

                    String identifier =
                            parts[1].trim();


                    // ------------------------------------------------
                    // OLD CUSTOMER COMPATIBILITY
                    // CUST-XXXX|PHONE/EMAIL
                    // ------------------------------------------------

                    if (accountId
                            .toUpperCase()
                            .startsWith("CUST-")) {

                        User customer =
                                userService
                                        .findByLoginIdentifier(
                                                accountId,
                                                identifier
                                        );


                        if (customer == null) {

                            return ResponseEntity.status(
                                    HttpStatus.UNAUTHORIZED
                            ).body(
                                    Map.of(
                                            "success",
                                            false,
                                            "message",
                                            "Customer account could not be resolved."
                                    )
                            );
                        }


                        response.put(
                                "customerId",
                                customer.getCustomerId()
                        );

                        response.put(
                                "name",
                                customer.getName()
                        );

                        response.put(
                                "phone",
                                customer.getPhone()
                        );

                        response.put(
                                "email",
                                customer.getEmail()
                        );

                        response.put(
                                "role",
                                customer.getRole()
                        );


                        if (customer.getSeller() != null) {

                            response.put(
                                    "sellerId",
                                    customer.getSeller()
                                            .getSellerId()
                            );

                            response.put(
                                    "businessName",
                                    customer.getSeller()
                                            .getBusinessName()
                            );

                        } else {

                            response.put(
                                    "sellerId",
                                    null
                            );

                            response.put(
                                    "businessName",
                                    null
                            );
                        }


                        response.put(
                                "authorities",
                                authentication
                                        .getAuthorities()
                                        .stream()
                                        .map(
                                                GrantedAuthority::getAuthority
                                        )
                                        .toList()
                        );


                        return ResponseEntity.ok(
                                response
                        );
                    }


                    // ------------------------------------------------
                    // SELLER / ADMIN
                    // ------------------------------------------------

                    if (accountId
                            .toUpperCase()
                            .startsWith("SELLER-")) {

                        Seller seller =
                                userService.findSeller(
                                        accountId
                                );


                        if (seller == null) {

                            return ResponseEntity.status(
                                    HttpStatus.UNAUTHORIZED
                            ).body(
                                    Map.of(
                                            "success",
                                            false,
                                            "message",
                                            "Seller account could not be resolved."
                                    )
                            );
                        }


                        User user =
                                userService.findByLoginIdentifier(
                                        accountId,
                                        identifier
                                );


                        response.put(
                                "sellerId",
                                seller.getSellerId()
                        );

                        response.put(
                                "businessName",
                                seller.getBusinessName()
                        );


                        if (user != null) {

                            response.put(
                                    "customerId",
                                    user.getCustomerId()
                            );

                            response.put(
                                    "name",
                                    user.getName()
                            );

                            response.put(
                                    "phone",
                                    user.getPhone()
                            );

                            response.put(
                                    "email",
                                    user.getEmail()
                            );

                            response.put(
                                    "role",
                                    user.getRole()
                            );
                        }


                        response.put(
                                "authorities",
                                authentication
                                        .getAuthorities()
                                        .stream()
                                        .map(
                                                GrantedAuthority::getAuthority
                                        )
                                        .toList()
                        );


                        return ResponseEntity.ok(
                                response
                        );
                    }
                }
            }


            // ----------------------------------------------------
            // FALLBACK
            //
            // Handles an older session where the principal is
            // directly stored as phone/email.
            // ----------------------------------------------------

            User fallbackUser =
                    userService.findByCustomerId(
                            username
                    );


            if (fallbackUser == null) {

                fallbackUser =
                        userService.findByPhone(
                                username
                        );
            }


            if (fallbackUser == null) {

                fallbackUser =
                        userService.findByEmail(
                                username
                        );
            }


            if (fallbackUser != null) {

                response.put(
                        "customerId",
                        fallbackUser.getCustomerId()
                );

                response.put(
                        "name",
                        fallbackUser.getName()
                );

                response.put(
                        "phone",
                        fallbackUser.getPhone()
                );

                response.put(
                        "email",
                        fallbackUser.getEmail()
                );

                response.put(
                        "role",
                        fallbackUser.getRole()
                );


                if (fallbackUser.getSeller() != null) {

                    response.put(
                            "sellerId",
                            fallbackUser.getSeller()
                                    .getSellerId()
                    );

                    response.put(
                            "businessName",
                            fallbackUser.getSeller()
                                    .getBusinessName()
                    );

                } else {

                    response.put(
                            "sellerId",
                            null
                    );

                    response.put(
                            "businessName",
                            null
                    );
                }
            }


            response.put(
                    "authorities",
                    authentication
                            .getAuthorities()
                            .stream()
                            .map(
                                    GrantedAuthority::getAuthority
                            )
                            .toList()
            );


            return ResponseEntity.ok(
                    response
            );


        } catch (Exception e) {

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(
                    Map.of(
                            "success",
                            false,
                            "message",
                            safeMessage(e)
                    )
            );
        }
    }

    // ============================================================
    // LEGACY REGISTER
    // ============================================================

    @PostMapping("/register")
    public ResponseEntity<?> legacyRegister(
            @RequestBody LegacyRegisterRequest request
    ) {

        try {

            if (isBlank(request.name()) ||
                    isBlank(request.phone()) ||
                    isBlank(request.password())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Name, phone and password are required."
                        )
                );
            }

            if (isBlank(request.sellerId())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Seller ID is required for this endpoint."
                        )
                );
            }

            Seller seller =
                    userService.findSeller(
                            request.sellerId()
                    );

            User user =
                    userService.createCustomerForSeller(
                            seller,
                            request.name(),
                            request.phone(),
                            request.email(),
                            request.password()
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "success",
                            true,
                            "message",
                            "Registration successful.",
                            "customerId",
                            user.getCustomerId(),
                            "sellerId",
                            seller.getSellerId()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", safeMessage(e)
                    )
            );
        }
    }

    // ============================================================
    // FIND EXISTING CUSTOMER
    // ============================================================

    private User findExistingCustomer(
            String identifier
    ) {

        if (identifier == null ||
                identifier.isBlank()) {

            return null;
        }

        String value =
                identifier.trim();

        User user =
                userService.findByPhone(value);

        if (user != null) {
            return user;
        }

        return userService.findByEmail(value);
    }

    // ============================================================
    // RESOLVE SELLER FROM ADMIN AUTHENTICATION
    // ============================================================

    private Seller resolveSellerFromAuthentication(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "Administrator authentication required."
            );
        }

        String username =
                authentication.getName();

        // Expected:
        // SELLER-XXXX|phone
        // SELLER-XXXX|email

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

        // Fallback
        User user =
                userService.findByPhone(username);

        if (user == null) {
            user =
                    userService.findByEmail(username);
        }

        if (user != null &&
                user.getSeller() != null) {

            return user.getSeller();
        }

        throw new IllegalStateException(
                "Could not determine seller from authenticated administrator."
        );
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private boolean isBlank(String value) {

        return value == null ||
                value.trim().isEmpty();
    }

    private String firstNonBlank(
            String first,
            String second
    ) {

        if (!isBlank(first)) {
            return first.trim();
        }

        if (!isBlank(second)) {
            return second.trim();
        }

        return null;
    }

    private String safeMessage(Exception e) {

        if (e == null ||
                e.getMessage() == null ||
                e.getMessage().isBlank()) {

            return "Something went wrong.";
        }

        return e.getMessage();
    }

    // ============================================================
    // REQUEST RECORDS
    // ============================================================

    public record OtpRequest(
            String phone,
            String email
    ) {}

    public record CustomerRegisterRequest(
            String name,
            String phone,
            String email,
            String otp,
            String password,
            String sellerId
    ) {}

    public record SellerOtpRequest(
            String phone
    ) {}

    public record SellerRegisterRequest(
            String businessName,
            String ownerName,
            String phone,
            String email,
            String otp,
            String password
    ) {}

    public record AdminCustomerRequest(
            String name,
            String phone,
            String email
    ) {}

    public record LegacyRegisterRequest(
            String name,
            String phone,
            String email,
            String password,
            String sellerId
    ) {}
}
