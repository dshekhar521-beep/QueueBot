package com.ticketintelligence.ticket_intelligence.controller;

import com.ticketintelligence.ticket_intelligence.entity.Order;
import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.service.OrderService;
import com.ticketintelligence.ticket_intelligence.service.SellerService;
import com.ticketintelligence.ticket_intelligence.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final SellerService sellerService;
    private final UserService userService;


    public OrderController(
            OrderService orderService,
            SellerService sellerService,
            UserService userService
    ) {
        this.orderService = orderService;
        this.sellerService = sellerService;
        this.userService = userService;
    }


    // =========================================================
    // CREATE ORDER
    // =========================================================

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequest request,
            Authentication authentication
    ) {

        try {

            Seller seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            Order order =
                    orderService.createOrder(

                            request.orderId(),

                            request.customerId(),

                            request.productId(),

                            request.quantity(),

                            request.amount(),

                            request.orderDate(),

                            request.deliveryDate(),

                            request.trackingNumber(),

                            request.paymentStatus(),

                            request.status(),

                            seller
                    );


            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(order);


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Could not create order: "
                                            + (
                                            e.getMessage() != null
                                                    ? e.getMessage()
                                                    : "Unknown error"
                                    )
                            )
                    );
        }
    }


    // =========================================================
    // GET SELLER ORDERS
    // =========================================================

    @GetMapping
    public ResponseEntity<?> getOrders(
            Authentication authentication
    ) {

        try {

            Seller seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            List<Order> orders =
                    orderService.getOrdersForSeller(
                            seller.getSellerId()
                    );


            return ResponseEntity.ok(
                    orders
            );


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Could not load orders."
                            )
                    );
        }
    }


    // =========================================================
    // GET ONE ORDER
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(
            @PathVariable Long id,
            Authentication authentication
    ) {

        try {

            Seller seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            Order order =
                    orderService.getOrderById(
                            id,
                            seller
                    );


            return ResponseEntity.ok(
                    order
            );


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(
                            HttpStatus.NOT_FOUND
                    )
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );

        } catch (Exception e) {

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Could not load order."
                            )
                    );
        }
    }


    // =========================================================
    // DELETE ORDER
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(
            @PathVariable Long id,
            Authentication authentication
    ) {

        try {

            Seller seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            orderService.deleteOrder(
                    id,
                    seller
            );


            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Order deleted successfully."
                    )
            );


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(
                            HttpStatus.NOT_FOUND
                    )
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );

        } catch (Exception e) {

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Could not delete order."
                            )
                    );
        }
    }


    // =========================================================
    // AUTHENTICATED SELLER
    // =========================================================

    private Seller getAuthenticatedSeller(
            Authentication authentication
    ) {

        if (
                authentication == null ||
                !authentication.isAuthenticated()
        ) {

            throw new IllegalArgumentException(
                    "Please login first."
            );
        }


        String username =
                authentication.getName();


        if (
                username == null ||
                username.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Invalid authenticated seller session."
            );
        }


        username =
                username.trim();


        // ---------------------------------------------------------
        // CURRENT LOGIN FORMAT
        // Spring Security stores customerId as username.
        // ---------------------------------------------------------

        User user =
                userService.findByCustomerId(
                        username
                );


        if (
                user != null &&
                user.getSeller() != null
        ) {

            return user.getSeller();
        }


        // ---------------------------------------------------------
        // OLD LOGIN FORMAT
        // SELLER_ID|PHONE_OR_EMAIL
        // ---------------------------------------------------------

        int separator =
                username.indexOf("|");


        if (separator > 0) {

            String sellerId =
                    username
                            .substring(
                                    0,
                                    separator
                            )
                            .trim();


            return sellerService
                    .findBySellerId(
                            sellerId
                    );
        }


        // ---------------------------------------------------------
        // DIRECT SELLER ID FALLBACK
        // ---------------------------------------------------------

        try {

            return sellerService
                    .findBySellerId(
                            username
                    );

        } catch (Exception ignored) {

            // Continue to final error below.
        }


        throw new IllegalArgumentException(
                "Invalid authenticated seller session."
        );
    }


    // =========================================================
    // REQUEST DTO
    // =========================================================

    public record OrderRequest(

            String orderId,

            String customerId,

            String productId,

            Integer quantity,

            BigDecimal amount,

            LocalDate orderDate,

            LocalDate deliveryDate,

            String trackingNumber,

            String paymentStatus,

            String status

    ) {
    }
}