package com.ticketintelligence.ticket_intelligence.repository;

import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.Ticket;
import com.ticketintelligence.ticket_intelligence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // =========================================================
    // BASIC LOOKUPS
    // =========================================================

    Optional<Ticket> findByTicketId(
            String ticketId
    );


    // =========================================================
    // SELLER TICKETS
    // =========================================================

    List<Ticket> findBySeller(
            Seller seller
    );

    List<Ticket> findBySellerOrderByPriorityScoreDesc(
            Seller seller
    );


    // =========================================================
    // CUSTOMER TICKETS
    // =========================================================

    List<Ticket> findByCreatedBy(
            User user
    );

    List<Ticket> findByCreatedByOrderByCreatedAtDesc(
            User user
    );


    // =========================================================
    // SECURE TICKET LOOKUPS
    // =========================================================

    Optional<Ticket> findByIdAndSeller(
            Long id,
            Seller seller
    );

    Optional<Ticket> findByIdAndCreatedBy(
            Long id,
            User user
    );


    // =========================================================
    // CUSTOMER + SELLER
    // =========================================================

    List<Ticket> findByCreatedByAndSeller(
            User user,
            Seller seller
    );


    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.createdBy = :customer
              AND t.seller = :seller
            ORDER BY t.createdAt DESC
            """)
    List<Ticket> findCustomerTicketsForSeller(
            @Param("customer") User customer,
            @Param("seller") Seller seller
    );


    // =========================================================
    // SEARCH / FILTER
    // =========================================================

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE
                (:priority IS NULL
                 OR :priority = ''
                 OR LOWER(t.priority) = LOWER(:priority))
            AND
                (:status IS NULL
                 OR :status = ''
                 OR LOWER(t.status) = LOWER(:status))
            AND
                (:category IS NULL
                 OR :category = ''
                 OR LOWER(t.category) = LOWER(:category))
            ORDER BY t.priorityScore DESC
            """)
    List<Ticket> searchTickets(
            @Param("priority") String priority,
            @Param("status") String status,
            @Param("category") String category
    );


    // =========================================================
    // PRIORITY QUEUE
    // =========================================================

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.seller = :seller
            ORDER BY t.priorityScore DESC,
                     t.createdAt ASC
            """)
    List<Ticket> findSellerPriorityQueue(
            @Param("seller") Seller seller
    );
}