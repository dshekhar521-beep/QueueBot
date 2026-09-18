package com.ticketintelligence.ticket_intelligence.service;

import com.ticketintelligence.ticket_intelligence.entity.Order;
import com.ticketintelligence.ticket_intelligence.entity.Product;
import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.repository.OrderRepository;
import com.ticketintelligence.ticket_intelligence.repository.ProductRepository;
import com.ticketintelligence.ticket_intelligence.repository.SellerRepository;
import com.ticketintelligence.ticket_intelligence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;


    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            SellerRepository sellerRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.sellerRepository = sellerRepository;
    }


    // =========================================================
    // CREATE ORDER
    // =========================================================

    @Transactional
    public Order createOrder(
            String orderId,
            String customerId,
            String productId,
            Integer quantity,
            BigDecimal amount,
            LocalDate orderDate,
            LocalDate deliveryDate,
            String trackingNumber,
            String paymentStatus,
            String status,
            Seller seller
    ) {

        if (seller == null) {
            throw new IllegalArgumentException(
                    "Seller account could not be identified."
            );
        }


        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException(
                    "Order ID is required."
            );
        }


        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer ID is required."
            );
        }


        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException(
                    "Product ID is required."
            );
        }


        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException(
                    "Quantity must be at least 1."
            );
        }


        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Amount must be zero or greater."
            );
        }


        /*
         * IMPORTANT:
         * Find the real User entity.
         *
         * We do NOT create a new User object from the
         * customerId sent by the browser.
         */
        User customer =
                userRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Customer not found: " + customerId
                                )
                        );


        /*
         * BUSINESS RULE:
         *
         * A customer may have registered before buying anything,
         * so seller can be NULL at this point. The first order
         * created by a seller establishes the customer-seller link.
         * If the customer is already linked to another seller,
         * the order is rejected.
         */
        if (customer.getSeller() == null) {

            customer.setSeller(seller);
            customer = userRepository.save(customer);

        } else if (!customer.belongsToSeller(seller)) {

            throw new IllegalArgumentException(
                    "Customer is already linked to another seller."
            );
        }


        /*
         * Find product only inside this seller's catalog.
         */
        Product product =
                productRepository
                        .findByProductIdAndSeller(
                                productId,
                                seller
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Product not found for this seller: "
                                                + productId
                                )
                        );


        /*
         * Create the real order entity.
         */
        Order order =
                new Order();

        order.setOrderId(orderId);

        /*
         * THIS IS THE CRITICAL FIX.
         *
         * customer_id will now be populated because
         * the actual User entity is assigned.
         */
        order.setCustomer(customer);

        /*
         * seller_id
         */
        order.setSeller(seller);

        /*
         * product_id
         */
        order.setProduct(product);

        order.setQuantity(quantity);

        order.setAmount(amount);

        order.setOrderDate(
                orderDate != null
                        ? orderDate
                        : LocalDate.now()
        );

        order.setDeliveryDate(
                deliveryDate
        );

        order.setTrackingNumber(
                trackingNumber
        );

        order.setPaymentStatus(
                paymentStatus != null && !paymentStatus.isBlank()
                        ? paymentStatus
                        : "PAID"
        );

        order.setStatus(
                status != null && !status.isBlank()
                        ? status
                        : "PLACED"
        );


        /*
         * Keep customer contact snapshot.
         */
        order.setCustomerPhone(
                customer.getPhone()
        );

        order.setCustomerEmail(
                customer.getEmail()
        );


        return orderRepository.save(order);
    }


    // =========================================================
    // GET SELLER ORDERS
    // =========================================================

    @Transactional(readOnly = true)
    public List<Order> getOrdersForSeller(
            String sellerId
    ) {

        Seller seller =
                sellerRepository
                        .findBySellerId(sellerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Seller not found: " + sellerId
                                )
                        );


        return orderRepository
                .findBySellerOrderByIdDesc(seller);
    }


    // =========================================================
    // GET ALL ORDERS
    // =========================================================

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {

        return orderRepository.findAll();
    }


    // =========================================================
    // GET ONE ORDER
    // =========================================================

    @Transactional(readOnly = true)
    public Order getOrderById(
            Long id,
            Seller seller
    ) {

        return orderRepository
                .findByIdAndSeller(id, seller)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order not found."
                        )
                );
    }


    // =========================================================
    // DELETE ORDER
    // =========================================================

    @Transactional
    public void deleteOrder(
            Long id,
            Seller seller
    ) {

        Order order =
                orderRepository
                        .findByIdAndSeller(id, seller)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Order not found."
                                )
                        );


        orderRepository.delete(order);
    }
}