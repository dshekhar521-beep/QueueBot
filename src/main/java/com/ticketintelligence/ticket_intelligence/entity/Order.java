package com.ticketintelligence.ticket_intelligence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    /*
     * Actual customer relationship.
     * This is what writes customer_id into orders.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /*
     * Seller owning this order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    /*
     * Product contained in this order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(nullable = false)
    private String status;

    /*
     * Snapshot fields.
     * These preserve customer contact information at order time.
     */
    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;


    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public Order() {
    }


    public Order(
            String orderId,
            User customer,
            Seller seller,
            Product product,
            Integer quantity,
            BigDecimal amount,
            LocalDate orderDate,
            LocalDate deliveryDate,
            String trackingNumber,
            String paymentStatus,
            String status
    ) {
        this.orderId = orderId;
        this.customer = customer;
        this.seller = seller;
        this.product = product;
        this.quantity = quantity;
        this.amount = amount;
        this.orderDate = orderDate;
        this.deliveryDate = deliveryDate;
        this.trackingNumber = trackingNumber;
        this.paymentStatus = paymentStatus;
        this.status = status;

        if (customer != null) {
            this.customerPhone = customer.getPhone();
            this.customerEmail = customer.getEmail();
        }
    }


    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public Long getId() {
        return id;
    }


    public String getOrderId() {
        return orderId;
    }


    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }


    public User getCustomer() {
        return customer;
    }


    public void setCustomer(User customer) {
        this.customer = customer;

        if (customer != null) {
            this.customerPhone = customer.getPhone();
            this.customerEmail = customer.getEmail();
        }
    }


    public Seller getSeller() {
        return seller;
    }


    public void setSeller(Seller seller) {
        this.seller = seller;
    }


    public Product getProduct() {
        return product;
    }


    public void setProduct(Product product) {
        this.product = product;
    }


    public Integer getQuantity() {
        return quantity;
    }


    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }


    public BigDecimal getAmount() {
        return amount;
    }


    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


    public LocalDate getOrderDate() {
        return orderDate;
    }


    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }


    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }


    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }


    public String getTrackingNumber() {
        return trackingNumber;
    }


    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }


    public String getPaymentStatus() {
        return paymentStatus;
    }


    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }


    public String getCustomerPhone() {
        return customerPhone;
    }


    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }


    public String getCustomerEmail() {
        return customerEmail;
    }


    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
}