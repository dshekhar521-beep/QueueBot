package com.ticketintelligence.ticket_intelligence.repository;

import com.ticketintelligence.ticket_intelligence.entity.Feedback;
import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository
        extends JpaRepository<Feedback, Long> {

    List<Feedback> findBySellerOrderByCreatedAtDesc(
            Seller seller
    );

    List<Feedback> findByCustomerOrderByCreatedAtDesc(
            User customer
    );

    Optional<Feedback> findByTicket(Ticket ticket);

    boolean existsByTicket(Ticket ticket);
}