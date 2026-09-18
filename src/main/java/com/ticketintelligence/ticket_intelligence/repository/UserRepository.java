package com.ticketintelligence.ticket_intelligence.repository;

import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT u
            FROM User u
            JOIN u.sellers s
            WHERE s = :seller
            AND u.phone = :phone
            """)
    Optional<User> findBySellerAndPhone(
            @Param("seller") Seller seller,
            @Param("phone") String phone
    );

    @Query("""
            SELECT u
            FROM User u
            JOIN u.sellers s
            WHERE s = :seller
            AND LOWER(u.email) = LOWER(:email)
            """)
    Optional<User> findBySellerAndEmailIgnoreCase(
            @Param("seller") Seller seller,
            @Param("email") String email
    );

    @Query("""
            SELECT u
            FROM User u
            JOIN u.sellers s
            WHERE s = :seller
            AND u.customerId = :customerId
            """)
    Optional<User> findBySellerAndCustomerId(
            @Param("seller") Seller seller,
            @Param("customerId") String customerId
    );

    @Query("""
            SELECT u
            FROM User u
            JOIN u.sellers s
            WHERE s = :seller
            """)
    List<User> findBySeller(
            @Param("seller") Seller seller
    );

    @Query("""
            SELECT u
            FROM User u
            JOIN u.sellers s
            WHERE s = :seller
            AND u.role = :role
            """)
    List<User> findBySellerAndRole(
            @Param("seller") Seller seller,
            @Param("role") String role
    );

    @Query("""
            SELECT u
            FROM User u
            JOIN u.sellers s
            WHERE s = :seller
            AND u.role = :role
            """)
    List<User> findByRoleAndSeller(
            @Param("role") String role,
            @Param("seller") Seller seller
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

    // ============================================================
    // SELLER-SPECIFIC EXISTS
    // ============================================================

    @Query("""
            SELECT COUNT(u) > 0
            FROM User u
            JOIN u.sellers s
            WHERE u.phone = :phone
            AND s = :seller
            """)
    boolean existsByPhoneAndSeller(
            @Param("phone") String phone,
            @Param("seller") Seller seller
    );
}