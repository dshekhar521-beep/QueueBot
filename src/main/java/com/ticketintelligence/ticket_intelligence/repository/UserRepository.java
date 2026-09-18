package com.ticketintelligence.ticket_intelligence.repository;

import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ============================================================
    // GLOBAL LOOKUPS
    // ============================================================

    Optional<User> findByCustomerId(String customerId);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmailIgnoreCase(String email);

    // ============================================================
    // SELLER-SPECIFIC LOOKUPS
    // ============================================================

    Optional<User> findBySellerAndPhone(
            Seller seller,
            String phone
    );

    Optional<User> findBySellerAndEmailIgnoreCase(
            Seller seller,
            String email
    );

    Optional<User> findBySellerAndCustomerId(
            Seller seller,
            String customerId
    );

    List<User> findBySeller(
            Seller seller
    );

    List<User> findBySellerAndRole(
            Seller seller,
            String role
    );

    List<User> findByRoleAndSeller(
            String role,
            Seller seller
    );

    // ============================================================
    // ROLE LOOKUPS
    // ============================================================

    List<User> findByRole(String role);

    List<User> findBySellerIsNullAndRole(
            String role
    );

    // ============================================================
    // EXISTS
    // ============================================================

    boolean existsByCustomerId(
            String customerId
    );

    boolean existsByPhone(
            String phone
    );

    boolean existsByEmailIgnoreCase(
            String email
    );

    // FIX:
    // Used by AdminController
    boolean existsByPhoneAndSeller(
            String phone,
            Seller seller
    );
}