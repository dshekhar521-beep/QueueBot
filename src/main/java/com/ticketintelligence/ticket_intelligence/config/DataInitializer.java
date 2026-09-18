package com.ticketintelligence.ticket_intelligence.config;

import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.repository.SellerRepository;
import com.ticketintelligence.ticket_intelligence.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeData(
            SellerRepository sellerRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            // ---------------------------------------------------------
            // 1. Find or create demo seller
            // ---------------------------------------------------------
            Seller seller = sellerRepository
                    .findBySellerId("SELLER-001")
                    .orElseGet(() -> {

                        Seller newSeller = new Seller(
                                "SELLER-001",
                                "QueueBot Demo Store",
                                "9999999998",
                                "seller@ticketintelligence.com",
                                "Kolkata, India"
                        );

                        newSeller.setOwnerName("QueueBot Admin");
                        newSeller.setActive(true);

                        return sellerRepository.save(newSeller);
                    });

            // ---------------------------------------------------------
            // 2. Find existing admin by ADMIN-001 first
            //    If not found, try the admin email.
            //    Only create a new admin if neither exists.
            // ---------------------------------------------------------
            User admin = userRepository
                    .findByCustomerId("ADMIN-001")
                    .orElseGet(() ->
                            userRepository
                                    .findByEmailIgnoreCase("admin@ticketintelligence.com")
                                    .orElseGet(User::new)
                    );

            // ---------------------------------------------------------
            // 3. Update / normalize the demo admin
            // ---------------------------------------------------------
            admin.setCustomerId("ADMIN-001");
            admin.setName("System Administrator");
            admin.setPhone("9999999998");
            admin.setEmail("admin@ticketintelligence.com");
            admin.setRole("ADMIN");
            admin.setSeller(seller);
            admin.setAccountVerified(true);
            admin.setActive(true);

            // Always ensure the known demo password is usable.
            admin.setPassword(passwordEncoder.encode("admin123"));

            // ---------------------------------------------------------
            // 4. Save
            //    Existing ADMIN-001 => UPDATE
            //    Missing ADMIN-001 => INSERT
            // ---------------------------------------------------------
            userRepository.save(admin);

            System.out.println("========================================");
            System.out.println(" QueueBot demo data initialized");
            System.out.println(" Seller ID : SELLER-001");
            System.out.println(" Admin ID  : ADMIN-001");
            System.out.println(" Login     : 9999999998");
            System.out.println(" Password  : admin123");
            System.out.println("========================================");
        };
    }
}