package com.ticketintelligence.ticket_intelligence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(
            name = "ticket_id",
            nullable = false,
            unique = true
    )
    private String ticketId;


    @Column(nullable = false)
    private String title;


    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;


    private String issueType;


    private String category;


    private String priority;


    private String status;


    private String sentiment;


    private String urgency;


    private String escalationRisk;


    private int priorityScore;


    @Lob
    @Column(columnDefinition = "TEXT")
    private String priorityReason;


    @Lob
    @Column(columnDefinition = "TEXT")
    private String suggestedResponse;


    @Lob
    @Column(columnDefinition = "TEXT")
    private String adminResponse;


    private LocalDateTime createdAt;


    private LocalDateTime respondedAt;


    private LocalDateTime resolvedAt;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id")
    private User createdBy;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id")
    private Seller seller;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private Order order;


    @OneToMany(
            mappedBy = "ticket",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<TicketAttachment> attachments =
            new ArrayList<>();


    public Ticket() {

        this.createdAt =
                LocalDateTime.now();

        this.status =
                "OPEN";
    }


    /* =========================================================
       GETTERS / SETTERS
    ========================================================== */

    public Long getId() {
        return id;
    }


    public String getTicketId() {
        return ticketId;
    }


    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }


    public String getTitle() {
        return title;
    }


    public void setTitle(String title) {
        this.title = title;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getIssueType() {
        return issueType;
    }


    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }


    public String getCategory() {
        return category;
    }


    public void setCategory(String category) {
        this.category = category;
    }


    public String getPriority() {
        return priority;
    }


    public void setPriority(String priority) {
        this.priority = priority;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }


    public String getSentiment() {
        return sentiment;
    }


    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }


    public String getUrgency() {
        return urgency;
    }


    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }


    public String getEscalationRisk() {
        return escalationRisk;
    }


    public void setEscalationRisk(
            String escalationRisk
    ) {

        this.escalationRisk =
                escalationRisk;
    }


    public int getPriorityScore() {
        return priorityScore;
    }


    public void setPriorityScore(
            int priorityScore
    ) {

        this.priorityScore =
                priorityScore;
    }


    public String getPriorityReason() {
        return priorityReason;
    }


    public void setPriorityReason(
            String priorityReason
    ) {

        this.priorityReason =
                priorityReason;
    }


    public String getSuggestedResponse() {
        return suggestedResponse;
    }


    public void setSuggestedResponse(
            String suggestedResponse
    ) {

        this.suggestedResponse =
                suggestedResponse;
    }


    public String getAdminResponse() {
        return adminResponse;
    }


    public void setAdminResponse(
            String adminResponse
    ) {

        this.adminResponse =
                adminResponse;

        if (
                adminResponse != null
                        && !adminResponse.isBlank()
        ) {

            this.respondedAt =
                    LocalDateTime.now();
        }
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(
            LocalDateTime createdAt
    ) {

        this.createdAt =
                createdAt;
    }


    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }


    public void setRespondedAt(
            LocalDateTime respondedAt
    ) {

        this.respondedAt =
                respondedAt;
    }


    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }


    public void setResolvedAt(
            LocalDateTime resolvedAt
    ) {

        this.resolvedAt =
                resolvedAt;
    }


    public User getCreatedBy() {
        return createdBy;
    }


    public void setCreatedBy(
            User createdBy
    ) {

        this.createdBy =
                createdBy;
    }


    public User getAssignedAdmin() {
        return assignedAdmin;
    }


    public void setAssignedAdmin(
            User assignedAdmin
    ) {

        this.assignedAdmin =
                assignedAdmin;
    }


    public Seller getSeller() {
        return seller;
    }


    public void setSeller(
            Seller seller
    ) {

        this.seller =
                seller;
    }


    public Order getOrder() {
        return order;
    }


    public void setOrder(
            Order order
    ) {

        this.order =
                order;
    }


    public List<TicketAttachment> getAttachments() {
        return attachments;
    }


    public void setAttachments(
            List<TicketAttachment> attachments
    ) {

        this.attachments =
                attachments == null
                        ? new ArrayList<>()
                        : attachments;
    }


    public void addAttachment(
            TicketAttachment attachment
    ) {

        if (attachment == null) {
            return;
        }

        attachments.add(
                attachment
        );

        attachment.setTicket(
                this
        );
    }
}