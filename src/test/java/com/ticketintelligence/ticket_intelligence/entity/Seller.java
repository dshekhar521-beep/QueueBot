package com.ticketintelligence.ticket_intelligence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sellers")
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sellerId;

    @Column(nullable = false)
    private String businessName;

    private String phone;

    private String email;

    private String address;

    @Column(nullable = false)
    private boolean active = true;

    public Seller() {
    }

    public Seller(String sellerId, String businessName,
                  String phone, String email, String address) {
        this.sellerId = sellerId;
        this.businessName = businessName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}