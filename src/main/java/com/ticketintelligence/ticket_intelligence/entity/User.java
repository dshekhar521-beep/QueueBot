package com.ticketintelligence.ticket_intelligence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // CUSTOMER ID
    // ============================================================

    @Column(nullable = false, unique = true)
    private String customerId;

    // ============================================================
    // BASIC DETAILS
    // ============================================================

    @Column(nullable = false)
    private String name;

    /*
     * Nullable because a customer may register with email only.
     */
    private String phone;

    private String email;

    // ============================================================
    // PASSWORD
    // ============================================================

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    // ============================================================
    // ROLE
    // ============================================================

    @Column(nullable = false)
    private String role;

    // ============================================================
    // SELLER
    //
    // Nullable because a customer can register before
    // purchasing anything.
    // ============================================================

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "seller_id",
            nullable = true
    )
    private Seller seller;

    // ============================================================
    // ACCOUNT STATUS
    // ============================================================

    @Column(nullable = false)
    private boolean active = true;

    @Column(
            name = "account_verified",
            nullable = false
    )
    private boolean accountVerified = false;

    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public User() {
    }

    public User(
            String customerId,
            String name,
            String phone,
            String email,
            String password,
            String role,
            Seller seller
    ) {

        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.role = role;
        this.seller = seller;

        this.active = true;
        this.accountVerified = false;
    }

    // ============================================================
    // GETTERS / SETTERS
    // ============================================================

    public Long getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(
            String customerId
    ) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(
            String name
    ) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(
            String phone
    ) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(
            String email
    ) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(
            String password
    ) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(
            String role
    ) {
        this.role = role;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(
            Seller seller
    ) {
        this.seller = seller;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(
            boolean active
    ) {
        this.active = active;
    }

    public boolean isAccountVerified() {
        return accountVerified;
    }

    public void setAccountVerified(
            boolean accountVerified
    ) {
        this.accountVerified =
                accountVerified;
    }

    // ============================================================
    // COMPATIBILITY GETTERS
    // ============================================================

    public boolean getActive() {
        return active;
    }

    public boolean getAccountVerified() {
        return accountVerified;
    }

    // ============================================================
    // SELLER OWNERSHIP CHECK
    //
    // FIX:
    // OrderService expects:
    //
    // customer.belongsToSeller(seller)
    // ============================================================

    public boolean belongsToSeller(
            Seller seller
    ) {

        if (seller == null ||
                this.seller == null) {

            return false;
        }

        if (this.seller.getId() == null ||
                seller.getId() == null) {

            return false;
        }

        return this.seller
                .getId()
                .equals(
                        seller.getId()
                );
    }
}