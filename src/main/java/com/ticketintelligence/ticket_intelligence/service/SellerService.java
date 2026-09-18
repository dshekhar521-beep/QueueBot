package com.ticketintelligence.ticket_intelligence.service;

import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.repository.SellerRepository;
import com.ticketintelligence.ticket_intelligence.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SellerService {

    private final SellerRepository sellerRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;


    public SellerService(
            SellerRepository sellerRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {

        this.sellerRepository =
                sellerRepository;

        this.userRepository =
                userRepository;

        this.passwordEncoder =
                passwordEncoder;
    }


    public SellerRegistrationResult registerSeller(
            String businessName,
            String ownerName,
            String phone,
            String email,
            String password
    ) {

        if (
                sellerRepository.existsByPhone(
                        phone
                )
        ) {

            throw new RuntimeException(
                    "Seller phone number already exists."
            );
        }


        if (
                sellerRepository
                        .existsByEmailIgnoreCase(
                                email
                        )
        ) {

            throw new RuntimeException(
                    "Seller email already exists."
            );
        }


        String sellerId =
                generateSellerId();


        Seller seller =
                new Seller();

        seller.setSellerId(
                sellerId
        );

        seller.setBusinessName(
                businessName
        );

        seller.setOwnerName(
                ownerName
        );

        seller.setPhone(
                phone
        );

        seller.setEmail(
                email
        );

        seller.setActive(
                true
        );


        seller =
                sellerRepository.save(
                        seller
                );


        String adminId =
                generateAdminId();


        User admin =
                new User(
                        adminId,
                        ownerName,
                        phone,
                        email,
                        passwordEncoder.encode(
                                password
                        ),
                        "ROLE_ADMIN",
                        seller
                );


        admin.setAccountVerified(
                true
        );

        admin.setActive(
                true
        );


        userRepository.save(
                admin
        );


        return new SellerRegistrationResult(
                seller,
                adminId
        );
    }


    public Seller findBySellerId(
            String sellerId
    ) {

        return sellerRepository
                .findBySellerId(
                        sellerId
                )
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Seller not found."
                                )
                );
    }


    private String generateSellerId() {

        String id;

        do {

            id =
                    "SELLER-"
                            + String.format(
                            "%04d",
                            (int) (
                                    Math.random()
                                            * 10000
                            )
                    );

        } while (
                sellerRepository.existsBySellerId(
                        id
                )
        );

        return id;
    }


    private String generateAdminId() {

        String id;

        do {

            id =
                    "ADMIN-"
                            + String.format(
                            "%04d",
                            (int) (
                                    Math.random()
                                            * 10000
                            )
                    );

        } while (
                userRepository.existsByCustomerId(
                        id
                )
        );

        return id;
    }


    public record SellerRegistrationResult(
            Seller seller,
            String adminId
    ) {}
}