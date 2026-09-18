package com.ticketintelligence.ticket_intelligence.repository;

import com.ticketintelligence.ticket_intelligence.entity.Order;
import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // =========================================================
    // SELLER ORDERS
    // =========================================================

    List<Order> findBySeller(Seller seller);

    List<Order> findBySellerOrderByIdDesc(Seller seller);


    // =========================================================
    // CUSTOMER ORDERS
    // =========================================================

    List<Order> findByCustomer(User customer);

    List<Order> findByCustomerOrderByIdDesc(User customer);

    List<Order> findByCustomerAndSeller(
            User customer,
            Seller seller
    );

    List<Order> findBySellerAndCustomer(
            Seller seller,
            User customer
    );


    // =========================================================
    // ORDER ID
    // =========================================================

    Optional<Order> findByOrderId(String orderId);

    Optional<Order> findByOrderIdAndSeller(
            String orderId,
            Seller seller
    );


    // =========================================================
    // DATABASE ID + SELLER
    // =========================================================

    Optional<Order> findByIdAndSeller(
            Long id,
            Seller seller
    );


    // =========================================================
    // CUSTOMER PORTAL
    // =========================================================

    @Query("""
            SELECT o
            FROM Order o
            JOIN FETCH o.product p
            WHERE o.customer = :customer
              AND o.seller = :seller
            ORDER BY o.orderDate DESC
            """)
    List<Order> findCustomerOrdersForSeller(
            @Param("customer") User customer,
            @Param("seller") Seller seller
    );

}