package com.ticketintelligence.ticket_intelligence.repository;

import com.ticketintelligence.ticket_intelligence.entity.Product;
import com.ticketintelligence.ticket_intelligence.entity.Seller;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySeller(Seller seller);

    List<Product> findBySellerAndStatus(
            Seller seller,
            String status
    );

    Optional<Product> findByProductIdAndSeller(
            String productId,
            Seller seller
    );

    boolean existsByProductId(String productId);

    boolean existsBySkuAndSeller(
            String sku,
            Seller seller
    );
}