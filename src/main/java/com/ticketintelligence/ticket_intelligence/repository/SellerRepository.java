package com.ticketintelligence.ticket_intelligence.repository;

import com.ticketintelligence.ticket_intelligence.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository
        extends JpaRepository<Seller, Long> {

    Optional<Seller> findBySellerId(
            String sellerId
    );


    boolean existsBySellerId(
            String sellerId
    );


    boolean existsByPhone(
            String phone
    );


    boolean existsByEmailIgnoreCase(
            String email
    );
}