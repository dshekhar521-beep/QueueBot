package com.ticketintelligence.ticket_intelligence.service;

import com.ticketintelligence.ticket_intelligence.dto.TicketStats;
import com.ticketintelligence.ticket_intelligence.entity.Order;
import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.Ticket;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.repository.OrderRepository;
import com.ticketintelligence.ticket_intelligence.repository.TicketRepository;
import com.ticketintelligence.ticket_intelligence.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    private final TicketIntelligenceService ticketIntelligenceService;

    private final UserRepository userRepository;

    private final OrderRepository orderRepository;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public TicketService(
            TicketRepository ticketRepository,
            TicketIntelligenceService ticketIntelligenceService,
            UserRepository userRepository,
            OrderRepository orderRepository
    ) {

        this.ticketRepository =
                ticketRepository;

        this.ticketIntelligenceService =
                ticketIntelligenceService;

        this.userRepository =
                userRepository;

        this.orderRepository =
                orderRepository;
    }


    // =========================================================
    // CREATE TICKET FROM ENTITY
    // =========================================================

    @Transactional
    public Ticket createTicket(
            Ticket ticket
    ) {

        if (ticket == null) {

            throw new IllegalArgumentException(
                    "Ticket cannot be null."
            );
        }


        if (
                ticket.getDescription() == null ||
                        ticket.getDescription().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Ticket description is required."
            );
        }


        if (
                ticket.getTicketId() == null ||
                        ticket.getTicketId().isBlank()
        ) {

            ticket.setTicketId(
                    generateTicketId()
            );
        }


        if (
                ticket.getStatus() == null ||
                        ticket.getStatus().isBlank()
        ) {

            ticket.setStatus(
                    "OPEN"
            );
        }


        if (
                ticket.getCreatedAt() == null
        ) {

            ticket.setCreatedAt(
                    LocalDateTime.now()
            );
        }


        /*
         * Run QueueBot intelligence.
         *
         * IMPORTANT:
         * analyzeAndSetIntelligence() now sends:
         *
         * issueType + title + description
         *
         * instead of description only.
         */

        analyzeAndSetIntelligence(
                ticket
        );


        return ticketRepository.save(
                ticket
        );
    }


    // =========================================================
    // CREATE CUSTOMER TICKET
    // =========================================================

    @Transactional
    public Ticket createCustomerTicket(
            User customer,
            Long orderId,
            String issueType,
            String title,
            String description
    ) {

        if (customer == null) {

            throw new IllegalArgumentException(
                    "Customer is required."
            );
        }


        if (orderId == null) {

            throw new IllegalArgumentException(
                    "Order ID is required."
            );
        }


        if (
                description == null ||
                        description.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Ticket description is required."
            );
        }


        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Order not found."
                                )
                        );


        if (
                order.getCustomer() == null ||
                        !order.getCustomer()
                                .getId()
                                .equals(
                                        customer.getId()
                                )
        ) {

            throw new IllegalArgumentException(
                    "This order does not belong to the customer."
            );
        }


        Ticket ticket =
                new Ticket();


        ticket.setTicketId(
                generateTicketId()
        );


        ticket.setTitle(
                title != null &&
                        !title.isBlank()
                        ? title
                        : (
                        issueType != null &&
                                !issueType.isBlank()
                                ? issueType
                                : "Customer Support Request"
                )
        );


        ticket.setDescription(
                description.trim()
        );


        ticket.setIssueType(
                issueType != null &&
                        !issueType.isBlank()
                        ? issueType.trim().toUpperCase()
                        : "GENERAL"
        );


        ticket.setStatus(
                "OPEN"
        );


        ticket.setCreatedBy(
                customer
        );


        ticket.setOrder(
                order
        );


        ticket.setSeller(
                order.getSeller()
        );


        return createTicket(
                ticket
        );
    }


    // =========================================================
    // GET ALL TICKETS
    // =========================================================

    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {

        return ticketRepository.findAll();
    }


    // =========================================================
    // GET ONE TICKET
    // =========================================================

    @Transactional(readOnly = true)
    public Ticket getTicketById(
            Long id
    ) {

        if (id == null) {

            return null;
        }


        return ticketRepository
                .findById(id)
                .orElse(null);
    }


    // =========================================================
    // SELLER TICKETS
    // =========================================================

    @Transactional(readOnly = true)
    public List<Ticket> getSellerTickets(
            Seller seller
    ) {

        if (seller == null) {

            throw new IllegalArgumentException(
                    "Seller is required."
            );
        }


        return ticketRepository
                .findBySellerOrderByPriorityScoreDesc(
                        seller
                );
    }


    // =========================================================
    // CUSTOMER TICKETS
    // =========================================================

    @Transactional(readOnly = true)
    public List<Ticket> getCustomerTickets(
            User customer
    ) {

        if (customer == null) {

            throw new IllegalArgumentException(
                    "Customer is required."
            );
        }


        return ticketRepository
                .findByCreatedByOrderByCreatedAtDesc(
                        customer
                );
    }


    // =========================================================
    // GET SELLER TICKET
    // =========================================================

    @Transactional(readOnly = true)
    public Ticket getSellerTicket(
            Seller seller,
            Long id
    ) {

        if (
                seller == null ||
                        id == null
        ) {

            return null;
        }


        return ticketRepository
                .findByIdAndSeller(
                        id,
                        seller
                )
                .orElse(null);
    }


    // =========================================================
    // GET CUSTOMER TICKET
    // =========================================================

    @Transactional(readOnly = true)
    public Ticket getCustomerTicket(
            User customer,
            Long id
    ) {

        if (
                customer == null ||
                        id == null
        ) {

            return null;
        }


        return ticketRepository
                .findByIdAndCreatedBy(
                        id,
                        customer
                )
                .orElse(null);
    }


    // =========================================================
    // UPDATE TICKET
    // =========================================================

    @Transactional
    public Ticket updateTicket(
            Long id,
            Ticket updatedTicket
    ) {

        if (
                id == null ||
                        updatedTicket == null
        ) {

            return null;
        }


        Ticket existingTicket =
                ticketRepository
                        .findById(id)
                        .orElse(null);


        if (existingTicket == null) {

            return null;
        }


        if (
                updatedTicket.getTitle() != null &&
                        !updatedTicket
                                .getTitle()
                                .isBlank()
        ) {

            existingTicket.setTitle(
                    updatedTicket.getTitle()
            );
        }


        if (
                updatedTicket.getDescription() != null &&
                        !updatedTicket
                                .getDescription()
                                .isBlank()
        ) {

            existingTicket.setDescription(
                    updatedTicket.getDescription()
            );
        }


        if (
                updatedTicket.getIssueType() != null &&
                        !updatedTicket
                                .getIssueType()
                                .isBlank()
        ) {

            existingTicket.setIssueType(
                    updatedTicket.getIssueType()
            );
        }


        if (
                updatedTicket.getStatus() != null &&
                        !updatedTicket
                                .getStatus()
                                .isBlank()
        ) {

            existingTicket.setStatus(
                    updatedTicket.getStatus()
            );
        }


        /*
         * Re-run AI after an update.
         *
         * This now includes title + issue type + description.
         */

        analyzeAndSetIntelligence(
                existingTicket
        );


        return ticketRepository.save(
                existingTicket
        );
    }


    // =========================================================
    // RESPOND TO TICKET
    // =========================================================

    @Transactional
    public Ticket respondToTicket(
            User adminUser,
            Long ticketId,
            String response,
            String status
    ) {

        if (adminUser == null) {

            throw new IllegalArgumentException(
                    "Administrator is required."
            );
        }


        if (
                response == null ||
                        response.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Response cannot be empty."
            );
        }


        Ticket ticket =
                ticketRepository
                        .findById(ticketId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Ticket not found."
                                )
                        );


        ticket.setAdminResponse(
                response.trim()
        );


        ticket.setRespondedAt(
                LocalDateTime.now()
        );


        /*
         * The replying administrator becomes the
         * assigned administrator.
         */

        ticket.setAssignedAdmin(
                adminUser
        );


        if (
                status != null &&
                        !status.isBlank()
        ) {

            ticket.setStatus(
                    status.trim().toUpperCase()
            );

        } else {

            ticket.setStatus(
                    "IN_PROGRESS"
            );
        }


        if (
                "RESOLVED".equalsIgnoreCase(
                        ticket.getStatus()
                )
        ) {

            ticket.setResolvedAt(
                    LocalDateTime.now()
            );
        }


        return ticketRepository.save(
                ticket
        );
    }


    // =========================================================
    // ASSIGN TICKET
    // =========================================================

    @Transactional
    public Ticket assignTicket(
            User adminUser,
            Long ticketId,
            String adminIdentifier
    ) {

        if (adminUser == null) {

            throw new IllegalArgumentException(
                    "Administrator is required."
            );
        }


        Ticket ticket =
                ticketRepository
                        .findById(ticketId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Ticket not found."
                                )
                        );


        User assignedAdmin =
                findUserByIdentifier(
                        adminIdentifier
                );


        if (assignedAdmin == null) {

            assignedAdmin =
                    adminUser;
        }


        ticket.setAssignedAdmin(
                assignedAdmin
        );


        if (
                ticket.getStatus() == null ||
                        "OPEN".equalsIgnoreCase(
                                ticket.getStatus()
                        )
        ) {

            ticket.setStatus(
                    "IN_PROGRESS"
            );
        }


        return ticketRepository.save(
                ticket
        );
    }


    // =========================================================
    // DELETE TICKET
    // =========================================================

    @Transactional
    public void deleteTicket(
            Long id
    ) {

        if (id == null) {

            return;
        }


        if (
                ticketRepository
                        .existsById(id)
        ) {

            ticketRepository.deleteById(
                    id
            );
        }
    }


    // =========================================================
    // DELETE SELLER TICKET
    // =========================================================

    @Transactional
    public void deleteSellerTicket(
            Seller seller,
            Long id
    ) {

        if (
                seller == null ||
                        id == null
        ) {

            throw new IllegalArgumentException(
                    "Seller and ticket ID are required."
            );
        }


        Ticket ticket =
                ticketRepository
                        .findByIdAndSeller(
                                id,
                                seller
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Ticket not found."
                                )
                        );


        ticketRepository.delete(
                ticket
        );
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Transactional(readOnly = true)
    public List<Ticket> searchTickets(
            String priority,
            String status,
            String category
    ) {

        return ticketRepository.searchTickets(
                priority,
                status,
                category
        );
    }


    // =========================================================
    // TICKET STATISTICS
    // =========================================================

    @Transactional(readOnly = true)
    public TicketStats getTicketStats() {

        List<Ticket> tickets =
                ticketRepository.findAll();


        long totalTickets =
                tickets.size();


        long openTickets =
                tickets.stream()
                        .filter(ticket ->
                                "OPEN".equalsIgnoreCase(
                                        ticket.getStatus()
                                )
                        )
                        .count();


        long inProgressTickets =
                tickets.stream()
                        .filter(ticket ->
                                "IN_PROGRESS".equalsIgnoreCase(
                                        ticket.getStatus()
                                )
                        )
                        .count();


        long criticalTickets =
                tickets.stream()
                        .filter(ticket ->
                                "CRITICAL".equalsIgnoreCase(
                                        ticket.getPriority()
                                )
                        )
                        .count();


        long highPriorityTickets =
                tickets.stream()
                        .filter(ticket ->
                                "HIGH".equalsIgnoreCase(
                                        ticket.getPriority()
                                )
                        )
                        .count();


        long negativeSentimentTickets =
                tickets.stream()
                        .filter(ticket ->
                                "NEGATIVE".equalsIgnoreCase(
                                        ticket.getSentiment()
                                )
                        )
                        .count();


        long highEscalationRiskTickets =
                tickets.stream()
                        .filter(ticket ->
                                "HIGH".equalsIgnoreCase(
                                        ticket.getEscalationRisk()
                                )
                        )
                        .count();


        double averagePriorityScore =
                tickets.stream()
                        .mapToInt(
                                Ticket::getPriorityScore
                        )
                        .average()
                        .orElse(0.0);


        return new TicketStats(
                totalTickets,
                openTickets,
                inProgressTickets,
                criticalTickets,
                highPriorityTickets,
                negativeSentimentTickets,
                highEscalationRiskTickets,
                averagePriorityScore
        );
    }


    // =========================================================
    // AI ANALYSIS
    // =========================================================

    private void analyzeAndSetIntelligence(
            Ticket ticket
    ) {

        /*
         * IMPORTANT FIX
         *
         * Send all relevant ticket information:
         *
         * 1. issueType
         * 2. title
         * 3. description
         *
         * The previous code sent description only.
         */

        TicketIntelligenceResult result =
                ticketIntelligenceService
                        .analyzeTicket(
                                ticket.getIssueType(),
                                ticket.getTitle(),
                                ticket.getDescription()
                        );


        /*
         * TicketIntelligenceResult is a record,
         * therefore use record accessors.
         */


        ticket.setCategory(
                result.category()
        );


        ticket.setPriority(
                result.finalPriority()
        );


        ticket.setSentiment(
                result.sentiment()
        );


        ticket.setUrgency(
                result.urgency()
        );


        ticket.setEscalationRisk(
                result.escalationRisk()
        );


        ticket.setPriorityScore(
                result.priorityScore()
        );


        ticket.setPriorityReason(
                result.priorityReason()
        );


        ticket.setSuggestedResponse(
                result.suggestedResponse()
        );
    }


    // =========================================================
    // FIND USER
    // =========================================================

    private User findUserByIdentifier(
            String identifier
    ) {

        if (
                identifier == null ||
                        identifier.isBlank()
        ) {

            return null;
        }


        String value =
                identifier.trim();


        Optional<User> byCustomerId =
                userRepository
                        .findByCustomerId(
                                value
                        );


        if (
                byCustomerId.isPresent()
        ) {

            return byCustomerId.get();
        }


        Optional<User> byPhone =
                userRepository
                        .findByPhone(
                                value
                        );


        if (
                byPhone.isPresent()
        ) {

            return byPhone.get();
        }


        return userRepository
                .findByEmailIgnoreCase(
                        value
                )
                .orElse(null);
    }


    // =========================================================
    // TICKET ID
    // =========================================================

    private String generateTicketId() {

        return "TKT-" +
                UUID.randomUUID()
                        .toString()
                        .replace(
                                "-",
                                ""
                        )
                        .substring(
                                0,
                                8
                        )
                        .toUpperCase();
    }
}