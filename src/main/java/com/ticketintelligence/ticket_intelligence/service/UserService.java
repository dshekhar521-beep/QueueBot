package com.ticketintelligence.ticket_intelligence.service;

import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.repository.SellerRepository;
import com.ticketintelligence.ticket_intelligence.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            SellerRepository sellerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.sellerRepository = sellerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================================================
    // SAVE
    // ============================================================

    public User save(User user) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null."
            );
        }

        /*
         * seller is intentionally NOT required.
         *
         * A customer can exist before purchase.
         */
        return userRepository.save(user);
    }

    // ============================================================
    // FIND SELLER
    // ============================================================

    public Seller findSeller(String sellerId) {

        if (sellerId == null || sellerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Seller ID is required."
            );
        }

        return sellerRepository
                .findBySellerId(sellerId.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Seller not found: " + sellerId
                        )
                );
    }

    // ============================================================
    // CREATE USER
    // ============================================================

    public User createUser(
            String customerId,
            String name,
            String phone,
            String email,
            String password,
            String role,
            Seller seller
    ) {

        User user = new User(
                customerId,
                name,
                normalizePhone(phone),
                normalizeEmail(email),
                password,
                role,
                seller
        );

        user.setActive(true);

        return userRepository.save(user);
    }

    // ============================================================
    // CUSTOMER REGISTRATION
    //
    // Customer can register WITHOUT a seller.
    // ============================================================

    public User registerCustomer(
            String name,
            String phone,
            String email,
            String password,
            String sellerId
    ) {

        String normalizedPhone =
                normalizePhone(phone);

        String normalizedEmail =
                normalizeEmail(email);

        if ((normalizedPhone == null ||
                normalizedPhone.isBlank()) &&
                (normalizedEmail == null ||
                        normalizedEmail.isBlank())) {

            throw new IllegalArgumentException(
                    "Phone or Email is required."
            );
        }

        if (password == null ||
                password.isBlank()) {

            throw new IllegalArgumentException(
                    "Password is required."
            );
        }

        // --------------------------------------------------------
        // Find existing customer using phone
        // --------------------------------------------------------

        User existingCustomer = null;

        if (normalizedPhone != null &&
                !normalizedPhone.isBlank()) {

            existingCustomer =
                    userRepository
                            .findByPhone(
                                    normalizedPhone
                            )
                            .orElse(null);
        }

        // --------------------------------------------------------
        // Try email
        // --------------------------------------------------------

        if (existingCustomer == null &&
                normalizedEmail != null &&
                !normalizedEmail.isBlank()) {

            existingCustomer =
                    userRepository
                            .findByEmailIgnoreCase(
                                    normalizedEmail
                            )
                            .orElse(null);
        }

        // --------------------------------------------------------
        // Existing user
        // --------------------------------------------------------

        if (existingCustomer != null) {

            if (!"ROLE_USER".equalsIgnoreCase(
                    existingCustomer.getRole()) &&
                    !"USER".equalsIgnoreCase(
                            existingCustomer.getRole())) {

                throw new IllegalArgumentException(
                        "This Phone or Email is already associated " +
                                "with another account type."
                );
            }

            // Optional seller validation for legacy requests
            if (sellerId != null &&
                    !sellerId.isBlank()) {

                Seller requestedSeller =
                        findSeller(sellerId);

                if (existingCustomer.getSeller() != null &&
                        !existingCustomer.getSeller()
                                .getId()
                                .equals(
                                        requestedSeller.getId()
                                )) {

                    throw new IllegalArgumentException(
                            "This customer belongs to another seller."
                    );
                }
            }

            if (name != null &&
                    !name.isBlank()) {

                existingCustomer.setName(
                        name.trim()
                );

            } else if (existingCustomer.getName() == null ||
                    existingCustomer.getName().isBlank()) {

                existingCustomer.setName(
                        "QueueBot Customer"
                );
            }

            if (normalizedPhone != null &&
                    !normalizedPhone.isBlank()) {

                existingCustomer.setPhone(
                        normalizedPhone
                );
            }

            if (normalizedEmail != null &&
                    !normalizedEmail.isBlank()) {

                existingCustomer.setEmail(
                        normalizedEmail
                );
            }

            existingCustomer.setPassword(
                    passwordEncoder.encode(password)
            );

            existingCustomer.setRole(
                    "ROLE_USER"
            );

            existingCustomer.setActive(true);

            existingCustomer.setAccountVerified(true);

            /*
             * IMPORTANT:
             * Do NOT set seller here.
             *
             * Existing seller relationship stays unchanged.
             * For a brand-new user it remains NULL.
             */

            return userRepository.save(
                    existingCustomer
            );
        }

        // --------------------------------------------------------
        // NEW GLOBAL CUSTOMER
        //
        // No seller required.
        // --------------------------------------------------------

        String customerId =
                generateCustomerId();

        String finalName =
                name != null &&
                        !name.isBlank()
                        ? name.trim()
                        : "QueueBot Customer";

        User customer =
                new User(
                        customerId,
                        finalName,
                        normalizedPhone,
                        normalizedEmail,
                        passwordEncoder.encode(password),
                        "ROLE_USER",
                        null
                );

        customer.setActive(true);
        customer.setAccountVerified(true);
        customer.setSeller(null);

        return userRepository.save(customer);
    }

    // ============================================================
    // ADMIN CREATES / LINKS CUSTOMER TO SELLER
    // ============================================================

    public User createCustomerForSeller(
            Seller seller,
            String name,
            String phone,
            String email,
            String password
    ) {

        if (seller == null) {
            throw new IllegalArgumentException(
                    "Seller is required."
            );
        }

        String normalizedPhone =
                normalizePhone(phone);

        String normalizedEmail =
                normalizeEmail(email);

        if ((normalizedPhone == null ||
                normalizedPhone.isBlank()) &&
                (normalizedEmail == null ||
                        normalizedEmail.isBlank())) {

            throw new IllegalArgumentException(
                    "Phone or Email is required."
            );
        }

        User existing =
                findExistingCustomer(
                        normalizedPhone,
                        normalizedEmail
                );

        if (existing != null) {

            if (!"ROLE_USER".equalsIgnoreCase(
                    existing.getRole()) &&
                    !"USER".equalsIgnoreCase(
                            existing.getRole())) {

                throw new IllegalArgumentException(
                        "Phone or Email already belongs to an admin account."
                );
            }

            /*
             * Global customer can now become associated
             * with this seller.
             */

            if (existing.getSeller() != null &&
                    !existing.getSeller()
                            .getId()
                            .equals(seller.getId())) {

                throw new IllegalArgumentException(
                        "This customer is already linked to another seller."
                );
            }

            existing.setSeller(seller);

            if (name != null &&
                    !name.isBlank()) {

                existing.setName(
                        name.trim()
                );
            }

            if (normalizedPhone != null &&
                    !normalizedPhone.isBlank()) {

                existing.setPhone(
                        normalizedPhone
                );
            }

            if (normalizedEmail != null &&
                    !normalizedEmail.isBlank()) {

                existing.setEmail(
                        normalizedEmail
                );
            }

            existing.setRole("ROLE_USER");
            existing.setActive(true);

            return userRepository.save(existing);
        }

        // --------------------------------------------------------
        // Brand-new seller customer
        // --------------------------------------------------------

        String customerId =
                generateCustomerId();

        String finalPassword =
                password != null &&
                        !password.isBlank()
                        ? passwordEncoder.encode(password)
                        : passwordEncoder.encode(
                        UUID.randomUUID().toString()
                );

        User customer =
                new User(
                        customerId,
                        name != null &&
                                !name.isBlank()
                                ? name.trim()
                                : "Customer",
                        normalizedPhone,
                        normalizedEmail,
                        finalPassword,
                        "ROLE_USER",
                        seller
                );

        customer.setSeller(seller);
        customer.setActive(true);
        customer.setAccountVerified(false);

        return userRepository.save(customer);
    }

    // ============================================================
    // COMPATIBILITY OVERLOAD
    // ============================================================

    public User createCustomerForSeller(
            String name,
            String phone,
            String email,
            Seller seller
    ) {

        return createCustomerForSeller(
                seller,
                name,
                phone,
                email,
                null
        );
    }

    // ============================================================
    // CREATE OR FIND
    // ============================================================

    public User createOrFindCustomerForSeller(
            Seller seller,
            String name,
            String phone,
            String email
    ) {

        if (seller == null) {
            throw new IllegalArgumentException(
                    "Seller is required."
            );
        }

        User existing =
                findExistingCustomer(
                        normalizePhone(phone),
                        normalizeEmail(email)
                );

        if (existing != null) {

            if (existing.getSeller() == null) {

                existing.setSeller(seller);

                return userRepository.save(existing);
            }

            if (existing.getSeller()
                    .getId()
                    .equals(seller.getId())) {

                return existing;
            }

            throw new IllegalArgumentException(
                    "Customer belongs to another seller."
            );
        }

        return createCustomerForSeller(
                seller,
                name,
                phone,
                email,
                null
        );
    }

    // ============================================================
    // CUSTOMER LOOKUPS
    // ============================================================

    public User findByCustomerId(
            String customerId
    ) {

        if (customerId == null ||
                customerId.isBlank()) {

            return null;
        }

        return userRepository
                .findByCustomerId(
                        customerId.trim()
                )
                .orElse(null);
    }

    public Optional<User> findByCustomerIdOptional(
            String customerId
    ) {

        if (customerId == null ||
                customerId.isBlank()) {

            return Optional.empty();
        }

        return userRepository.findByCustomerId(
                customerId.trim()
        );
    }

    public User findByPhone(
            String phone
    ) {

        String normalized =
                normalizePhone(phone);

        if (normalized == null ||
                normalized.isBlank()) {

            return null;
        }

        return userRepository
                .findByPhone(normalized)
                .orElse(null);
    }

    public Optional<User> findByPhoneOptional(
            String phone
    ) {

        String normalized =
                normalizePhone(phone);

        if (normalized == null ||
                normalized.isBlank()) {

            return Optional.empty();
        }

        return userRepository.findByPhone(
                normalized
        );
    }

    public User findByEmail(
            String email
    ) {

        String normalized =
                normalizeEmail(email);

        if (normalized == null ||
                normalized.isBlank()) {

            return null;
        }

        return userRepository
                .findByEmailIgnoreCase(normalized)
                .orElse(null);
    }

    public Optional<User> findByEmailOptional(
            String email
    ) {

        String normalized =
                normalizeEmail(email);

        if (normalized == null ||
                normalized.isBlank()) {

            return Optional.empty();
        }

        return userRepository
                .findByEmailIgnoreCase(normalized);
    }

    // ============================================================
    // SELLER LOOKUPS
    // ============================================================

    public Optional<User> findBySellerAndPhone(
            Seller seller,
            String phone
    ) {

        return userRepository
                .findBySellerAndPhone(
                        seller,
                        normalizePhone(phone)
                );
    }

    public Optional<User> findBySellerAndEmail(
            Seller seller,
            String email
    ) {

        return userRepository
                .findBySellerAndEmailIgnoreCase(
                        seller,
                        normalizeEmail(email)
                );
    }

    public Optional<User> findBySellerAndCustomerId(
            Seller seller,
            String customerId
    ) {

        return userRepository
                .findBySellerAndCustomerId(
                        seller,
                        customerId
                );
    }

    // ============================================================
    // LOGIN
    //
    // ADMIN:
    // SELLER-XXXX|phone
    // SELLER-XXXX|email
    //
    // CUSTOMER:
    // CUST-XXXXXX|phone
    // CUST-XXXXXX|email
    // ============================================================

    public User findByLoginIdentifier(
            String firstIdentifier,
            String secondIdentifier
    ) {

        if (firstIdentifier == null ||
                firstIdentifier.isBlank() ||
                secondIdentifier == null ||
                secondIdentifier.isBlank()) {

            return null;
        }

        String first =
                firstIdentifier.trim();

        String second =
                secondIdentifier.trim();

        // --------------------------------------------------------
        // CUSTOMER LOGIN
        // --------------------------------------------------------

        if (first.toUpperCase().startsWith("CUST-")) {

            User customer =
                    findByCustomerId(first);

            if (customer == null) {
                return null;
            }

            if (!"ROLE_USER".equalsIgnoreCase(
                    customer.getRole()) &&
                    !"USER".equalsIgnoreCase(
                            customer.getRole())) {

                return null;
            }

            if (matchesCustomerIdentifier(
                    customer,
                    second
            )) {
                return customer;
            }

            return null;
        }

        // --------------------------------------------------------
        // SELLER / ADMIN LOGIN
        // --------------------------------------------------------

        if (first.toUpperCase().startsWith("SELLER-")) {

            Seller seller =
                    sellerRepository
                            .findBySellerId(first)
                            .orElse(null);

            if (seller == null) {
                return null;
            }

            Optional<User> byPhone =
                    userRepository
                            .findBySellerAndPhone(
                                    seller,
                                    normalizePhone(second)
                            );

            if (byPhone.isPresent()) {
                return byPhone.get();
            }

            Optional<User> byEmail =
                    userRepository
                            .findBySellerAndEmailIgnoreCase(
                                    seller,
                                    normalizeEmail(second)
                            );

            return byEmail.orElse(null);
        }

        return null;
    }

    // ============================================================
    // CUSTOMER LIST
    // ============================================================

    public List<User> getCustomers(
            Seller seller
    ) {

        if (seller == null) {
            return List.of();
        }

        return userRepository
                .findBySellerAndRole(
                        seller,
                        "ROLE_USER"
                );
    }

    // ============================================================
    // ADMIN LIST
    // ============================================================

    public List<User> getAdmins(
            Seller seller
    ) {

        if (seller == null) {
            return List.of();
        }

        return userRepository
                .findBySellerAndRole(
                        seller,
                        "ROLE_ADMIN"
                );
    }

    public List<User> findByRoleAndSeller(
            String role,
            Seller seller
    ) {

        if (seller == null ||
                role == null ||
                role.isBlank()) {

            return List.of();
        }

        return userRepository
                .findBySellerAndRole(
                        seller,
                        role
                );
    }

    public List<User> findByRole(
            String role
    ) {

        if (role == null ||
                role.isBlank()) {

            return List.of();
        }

        return userRepository.findByRole(role);
    }

    // ============================================================
    // COMPLETE CUSTOMER REGISTRATION
    // ============================================================

    public User completeCustomerRegistration(
            User customer,
            String password
    ) {

        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer not found."
            );
        }

        customer.setPassword(
                passwordEncoder.encode(password)
        );

        customer.setRole("ROLE_USER");
        customer.setActive(true);
        customer.setAccountVerified(true);

        return userRepository.save(customer);
    }

    // ============================================================
    // BELONGS TO SELLER
    // ============================================================

    public boolean belongsToSeller(
            User customer,
            Seller seller
    ) {

        if (customer == null ||
                seller == null ||
                customer.getSeller() == null) {

            return false;
        }

        return customer.getSeller()
                .getId()
                .equals(seller.getId());
    }

    // ============================================================
    // GET CUSTOMER FOR SELLER
    // ============================================================

    public User getCustomerForSeller(
            String customerId,
            Seller seller
    ) {

        if (customerId == null ||
                customerId.isBlank()) {

            throw new IllegalArgumentException(
                    "Customer ID is required."
            );
        }

        if (seller == null) {
            throw new IllegalArgumentException(
                    "Seller is required."
            );
        }

        return userRepository
                .findBySellerAndCustomerId(
                        seller,
                        customerId.trim()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found for this seller."
                        )
                );
    }

    // ============================================================
    // GENERATE CUSTOMER ID
    // ============================================================

    public String generateCustomerId() {

        String customerId;

        do {

            customerId =
                    "CUST-" +
                            String.format(
                                    "%06d",
                                    (int) (
                                            Math.random()
                                                    * 1_000_000
                                    )
                            );

        } while (
                userRepository
                        .findByCustomerId(customerId)
                        .isPresent()
        );

        return customerId;
    }

    // ============================================================
    // FIND EXISTING CUSTOMER
    // ============================================================

    private User findExistingCustomer(
            String phone,
            String email
    ) {

        User existing = null;

        if (phone != null &&
                !phone.isBlank()) {

            existing =
                    userRepository
                            .findByPhone(phone)
                            .orElse(null);
        }

        if (existing == null &&
                email != null &&
                !email.isBlank()) {

            existing =
                    userRepository
                            .findByEmailIgnoreCase(email)
                            .orElse(null);
        }

        return existing;
    }

    // ============================================================
    // CUSTOMER IDENTIFIER MATCH
    // ============================================================

    private boolean matchesCustomerIdentifier(
            User customer,
            String identifier
    ) {

        String normalizedIdentifier =
                identifier.trim();

        String customerPhone =
                normalizePhone(
                        customer.getPhone()
                );

        String customerEmail =
                normalizeEmail(
                        customer.getEmail()
                );

        String normalizedPhone =
                normalizePhone(
                        normalizedIdentifier
                );

        String normalizedEmail =
                normalizeEmail(
                        normalizedIdentifier
                );

        if (customerPhone != null &&
                customerPhone.equals(
                        normalizedPhone
                )) {

            return true;
        }

        return customerEmail != null &&
                customerEmail.equalsIgnoreCase(
                        normalizedEmail
                );
    }

    // ============================================================
    // NORMALIZE PHONE
    // ============================================================

    public String normalizePhone(
            String phone
    ) {

        if (phone == null) {
            return null;
        }

        String value =
                phone.trim();

        if (value.isBlank()) {
            return null;
        }

        return value;
    }

    // ============================================================
    // NORMALIZE EMAIL
    // ============================================================

    public String normalizeEmail(
            String email
    ) {

        if (email == null) {
            return null;
        }

        String value =
                email.trim();

        if (value.isBlank()) {
            return null;
        }

        return value.toLowerCase();
    }

    // ============================================================
    // PASSWORD
    // ============================================================

    public String encodePassword(
            String password
    ) {

        if (password == null ||
                password.isBlank()) {

            throw new IllegalArgumentException(
                    "Password cannot be empty."
            );
        }

        return passwordEncoder.encode(password);
    }

    public boolean passwordMatches(
            String rawPassword,
            String encodedPassword
    ) {

        if (rawPassword == null ||
                encodedPassword == null) {

            return false;
        }

        return passwordEncoder.matches(
                rawPassword,
                encodedPassword
        );
    }
}