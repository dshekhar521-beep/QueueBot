package com.ticketintelligence.ticket_intelligence.controller;

import com.ticketintelligence.ticket_intelligence.entity.Order;
import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.Ticket;
import com.ticketintelligence.ticket_intelligence.entity.TicketEvidence;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.repository.SellerRepository;
import com.ticketintelligence.ticket_intelligence.repository.TicketEvidenceRepository;
import com.ticketintelligence.ticket_intelligence.repository.UserRepository;
import com.ticketintelligence.ticket_intelligence.service.TicketService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final TicketEvidenceRepository ticketEvidenceRepository;

    public TicketController(
            TicketService ticketService,
            SellerRepository sellerRepository,
            UserRepository userRepository,
            TicketEvidenceRepository ticketEvidenceRepository
    ) {
        this.ticketService = ticketService;
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
        this.ticketEvidenceRepository = ticketEvidenceRepository;
    }

    // =========================================================
    // GET SELLER TICKETS
    // =========================================================

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getTickets(
            Authentication authentication
    ) {
        try {

            Seller seller =
                    getAuthenticatedSeller(authentication);

            List<Ticket> tickets =
                    ticketService.getSellerTickets(seller);

            return ResponseEntity.ok(
                    tickets.stream()
                            .map(this::ticketResponse)
                            .toList()
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(error(e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            error(
                                    "Could not load tickets."
                            )
                    );
        }
    }

    // =========================================================
    // GET SINGLE TICKET
    // =========================================================

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getTicket(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {

            Seller seller =
                    getAuthenticatedSeller(authentication);

            Ticket ticket =
                    ticketService.getSellerTicket(
                            seller,
                            id
                    );

            if (ticket == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                error(
                                        "Ticket not found."
                                )
                        );
            }

            return ResponseEntity.ok(
                    ticketResponse(ticket)
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            error(
                                    "Could not load ticket."
                            )
                    );
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<?> searchTickets(
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            Authentication authentication
    ) {
        try {

            Seller seller =
                    getAuthenticatedSeller(authentication);

            List<Ticket> tickets =
                    ticketService
                            .searchTickets(
                                    priority,
                                    status,
                                    category
                            )
                            .stream()
                            .filter(ticket ->
                                    ticket.getSeller() != null &&
                                            seller.getId()
                                                    .equals(
                                                            ticket.getSeller()
                                                                    .getId()
                                                    )
                            )
                            .toList();

            return ResponseEntity.ok(
                    tickets.stream()
                            .map(this::ticketResponse)
                            .toList()
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            error(
                                    "Could not search tickets."
                            )
                    );
        }
    }

    // =========================================================
    // STATS
    // =========================================================

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getStats(
            Authentication authentication
    ) {
        try {

            Seller seller =
                    getAuthenticatedSeller(authentication);

            List<Ticket> tickets =
                    ticketService.getSellerTickets(seller);

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

            Map<String, Object> stats =
                    new LinkedHashMap<>();

            stats.put(
                    "totalTickets",
                    totalTickets
            );

            stats.put(
                    "openTickets",
                    openTickets
            );

            stats.put(
                    "inProgressTickets",
                    inProgressTickets
            );

            stats.put(
                    "criticalTickets",
                    criticalTickets
            );

            stats.put(
                    "highPriorityTickets",
                    highPriorityTickets
            );

            stats.put(
                    "negativeSentimentTickets",
                    negativeSentimentTickets
            );

            stats.put(
                    "highEscalationRiskTickets",
                    highEscalationRiskTickets
            );

            stats.put(
                    "averagePriorityScore",
                    averagePriorityScore
            );

            return ResponseEntity.ok(stats);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            error(
                                    "Could not load ticket statistics."
                            )
                    );
        }
    }

    // =========================================================
    // SEND RESPONSE
    // =========================================================

    @PostMapping("/{id}/respond")
    @Transactional
    public ResponseEntity<?> respondToTicket(
            @PathVariable Long id,
            @RequestBody ResponseRequest request,
            Authentication authentication
    ) {
        try {

            Seller seller =
                    getAuthenticatedSeller(authentication);

            /*
             * IMPORTANT:
             * Check Spring Security authority directly.
             */
            requireAdministrator(authentication);

            User admin =
                    getAuthenticatedAdmin(
                            authentication,
                            seller
                    );

            Ticket ticket =
                    ticketService.getSellerTicket(
                            seller,
                            id
                    );

            if (ticket == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                error(
                                        "Ticket not found."
                                )
                        );
            }

            if (
                    request == null ||
                            request.response() == null ||
                            request.response().isBlank()
            ) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                error(
                                        "Response cannot be empty."
                                )
                        );
            }

            String status =
                    request.status() == null ||
                            request.status().isBlank()
                            ? "IN_PROGRESS"
                            : request.status()
                            .trim()
                            .toUpperCase();

            Ticket updated =
                    ticketService.respondToTicket(
                            admin,
                            id,
                            request.response().trim(),
                            status
                    );

            return ResponseEntity.ok(
                    ticketResponse(updated)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            error(e.getMessage())
                    );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            error(
                                    "Could not send response."
                            )
                    );
        }
    }

    // =========================================================
    // ASSIGN TICKET
    // =========================================================

    @PostMapping("/{id}/assign")
    @Transactional
    public ResponseEntity<?> assignTicket(
            @PathVariable Long id,
            @RequestBody AssignRequest request,
            Authentication authentication
    ) {
        try {

            Seller seller =
                    getAuthenticatedSeller(authentication);

            requireAdministrator(authentication);

            User admin =
                    getAuthenticatedAdmin(
                            authentication,
                            seller
                    );

            Ticket ticket =
                    ticketService.getSellerTicket(
                            seller,
                            id
                    );

            if (ticket == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                error(
                                        "Ticket not found."
                                )
                        );
            }

            Ticket updated =
                    ticketService.assignTicket(
                            admin,
                            id,
                            request == null
                                    ? ""
                                    : request.adminIdentifier()
                    );

            return ResponseEntity.ok(
                    ticketResponse(updated)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(error(e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            error(
                                    "Could not assign ticket."
                            )
                    );
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateTicket(
            @PathVariable Long id,
            @RequestBody Ticket updatedTicket,
            Authentication authentication
    ) {
        try {

            Seller seller =
                    getAuthenticatedSeller(authentication);

            Ticket existing =
                    ticketService.getSellerTicket(
                            seller,
                            id
                    );

            if (existing == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                error(
                                        "Ticket not found."
                                )
                        );
            }

            Ticket saved =
                    ticketService.updateTicket(
                            id,
                            updatedTicket
                    );

            return ResponseEntity.ok(
                    ticketResponse(saved)
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            error(
                                    "Could not update ticket."
                            )
                    );
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteTicket(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {

            Seller seller =
                    getAuthenticatedSeller(authentication);

            Ticket ticket =
                    ticketService.getSellerTicket(
                            seller,
                            id
                    );

            if (ticket == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                error(
                                        "Ticket not found."
                                )
                        );
            }

            ticketService.deleteSellerTicket(
                    seller,
                    id
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Ticket deleted successfully."
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            error(
                                    "Could not delete ticket."
                            )
                    );
        }
    }

    // =========================================================
    // GENERIC CREATE
    // =========================================================

    @PostMapping(consumes = "application/json")
    @Transactional
    public ResponseEntity<?> createTicket(
            @RequestBody Ticket ticket,
            Authentication authentication
    ) {
        try {

            Seller seller =
                    getAuthenticatedSeller(authentication);

            User user =
                    getAuthenticatedUser(
                            authentication,
                            seller
                    );

            if (user == null) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(
                                error(
                                        "User account not found."
                                )
                        );
            }

            ticket.setCreatedBy(user);

            ticket.setSeller(seller);

            if (
                    ticket.getStatus() == null ||
                            ticket.getStatus().isBlank()
            ) {

                ticket.setStatus("OPEN");
            }

            Ticket saved =
                    ticketService.createTicket(
                            ticket
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(
                            ticketResponse(saved)
                    );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            error(
                                    "Could not create ticket."
                            )
                    );
        }
    }

    // =========================================================
    // SECURITY CHECK
    // =========================================================

    private void requireAdministrator(
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

        boolean administrator =
                authentication
                        .getAuthorities()
                        .stream()
                        .anyMatch(authority ->

                                "ROLE_ADMIN".equals(
                                        authority.getAuthority()
                                )

                                        ||

                                        "ROLE_SUPER_ADMIN".equals(
                                                authority.getAuthority()
                                        )
                        );

        if (!administrator) {

            throw new IllegalArgumentException(
                    "Administrator access required."
            );
        }
    }

    // =========================================================
    // SELLER
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

        // CURRENT AUTHENTICATION FORMAT:
        // ADMIN-2958
        //
        // Find the logged-in User by customerId
        // and get the Seller linked to that User.

        Optional<User> user =
                userRepository.findByCustomerId(username);

        if (
                user.isPresent() &&
                        user.get().getSeller() != null
        ) {

            return user.get().getSeller();
        }

        // LEGACY COMPATIBILITY:
        // SELLER-XXXX|phone
        // SELLER-XXXX|email

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

            return sellerRepository
                    .findBySellerId(sellerId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Seller account not found."
                            )
                    );
        }

        throw new IllegalArgumentException(
                "Invalid seller session."
        );
    }

    // =========================================================
    // USER
    // =========================================================

    private User getAuthenticatedUser(
            Authentication authentication,
            Seller seller
    ) {

        String identifier =
                getIdentifier(authentication);

        Optional<User> user =
                userRepository.findByPhone(
                        identifier
                );

        if (user.isEmpty()) {

            user =
                    userRepository
                            .findByEmailIgnoreCase(
                                    identifier
                            );
        }

        if (user.isEmpty()) {

            user =
                    userRepository
                            .findByCustomerId(
                                    identifier
                            );
        }

        return user.orElse(null);
    }

    // =========================================================
    // ADMIN
    // =========================================================

    private User getAuthenticatedAdmin(
            Authentication authentication,
            Seller seller
    ) {

        String identifier =
                getIdentifier(authentication);

        Optional<User> user =
                userRepository.findByPhone(
                        identifier
                );

        if (user.isEmpty()) {

            user =
                    userRepository
                            .findByEmailIgnoreCase(
                                    identifier
                            );
        }

        if (user.isEmpty()) {

            user =
                    userRepository
                            .findByCustomerId(
                                    identifier
                            );
        }

        if (user.isEmpty()) {

            throw new IllegalArgumentException(
                    "Administrator account not found."
            );
        }

        User admin =
                user.get();

        String role =
                admin.getRole() == null
                        ? ""
                        : admin.getRole()
                        .trim()
                        .toUpperCase();

        /*
         * Accept both:
         * ADMIN
         * ROLE_ADMIN
         * SUPER_ADMIN
         * ROLE_SUPER_ADMIN
         */
        boolean adminRole =
                "ADMIN".equals(role) ||
                        "ROLE_ADMIN".equals(role) ||
                        "SUPER_ADMIN".equals(role) ||
                        "ROLE_SUPER_ADMIN".equals(role);

        if (!adminRole) {

            throw new IllegalArgumentException(
                    "Administrator access required."
            );
        }

        if (!admin.isActive()) {

            throw new IllegalArgumentException(
                    "Administrator account is inactive."
            );
        }

        /*
         * Ensure this administrator belongs to
         * the seller whose ticket is being modified.
         */
        if (
                admin.getSeller() != null &&
                        admin.getSeller().getId() != null &&
                        seller.getId() != null &&
                        !admin.getSeller()
                                .getId()
                                .equals(
                                        seller.getId()
                                )
        ) {

            throw new IllegalArgumentException(
                    "Administrator is not connected to this seller."
            );
        }

        return admin;
    }

    // =========================================================
    // IDENTIFIER
    // =========================================================

    private String getIdentifier(
            Authentication authentication
    ) {

        String username =
                authentication.getName();

        int separator =
                username.indexOf("|");

        if (separator <= 0) {

            throw new IllegalArgumentException(
                    "Invalid authenticated session."
            );
        }

        return username
                .substring(
                        separator + 1
                )
                .trim();
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    private Map<String, Object> ticketResponse(
            Ticket ticket
    ) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "id",
                ticket.getId()
        );

        result.put(
                "ticketId",
                ticket.getTicketId()
        );

        result.put(
                "title",
                ticket.getTitle()
        );

        result.put(
                "description",
                ticket.getDescription()
        );

        result.put(
                "issueType",
                ticket.getIssueType()
        );

        result.put(
                "category",
                ticket.getCategory()
        );

        result.put(
                "priority",
                ticket.getPriority()
        );

        result.put(
                "sentiment",
                ticket.getSentiment()
        );

        result.put(
                "urgency",
                ticket.getUrgency()
        );

        result.put(
                "escalationRisk",
                ticket.getEscalationRisk()
        );

        result.put(
                "priorityScore",
                ticket.getPriorityScore()
        );

        result.put(
                "priorityReason",
                ticket.getPriorityReason()
        );

        result.put(
                "suggestedResponse",
                ticket.getSuggestedResponse()
        );

        result.put(
                "adminResponse",
                ticket.getAdminResponse()
        );

        result.put(
                "status",
                ticket.getStatus()
        );

        result.put(
                "createdAt",
                ticket.getCreatedAt()
        );

        result.put(
                "respondedAt",
                ticket.getRespondedAt()
        );

        result.put(
                "resolvedAt",
                ticket.getResolvedAt()
        );

        if (
                ticket.getSeller() != null
        ) {

            result.put(
                    "sellerId",
                    ticket.getSeller()
                            .getSellerId()
            );

            result.put(
                    "businessName",
                    ticket.getSeller()
                            .getBusinessName()
            );
        }

        if (
                ticket.getCreatedBy() != null
        ) {

            User customer =
                    ticket.getCreatedBy();

            Map<String, Object> customerMap =
                    new LinkedHashMap<>();

            customerMap.put(
                    "id",
                    customer.getId()
            );

            customerMap.put(
                    "customerId",
                    customer.getCustomerId()
            );

            customerMap.put(
                    "name",
                    customer.getName()
            );

            customerMap.put(
                    "phone",
                    customer.getPhone()
            );

            customerMap.put(
                    "email",
                    customer.getEmail()
            );

            result.put(
                    "customer",
                    customerMap
            );
        }

        Order order =
                ticket.getOrder();

        if (order != null) {

            Map<String, Object> orderMap =
                    new LinkedHashMap<>();

            orderMap.put(
                    "id",
                    order.getId()
            );

            orderMap.put(
                    "orderId",
                    order.getOrderId()
            );

            orderMap.put(
                    "amount",
                    order.getAmount()
            );

            orderMap.put(
                    "quantity",
                    order.getQuantity()
            );

            orderMap.put(
                    "status",
                    order.getStatus()
            );

            if (
                    order.getProduct() != null
            ) {

                orderMap.put(
                        "productName",
                        order.getProduct()
                                .getName()
                );

                orderMap.put(
                        "productId",
                        order.getProduct()
                                .getProductId()
                );
            }

            result.put(
                    "order",
                    orderMap
            );

            result.put(
                    "orderId",
                    order.getOrderId()
            );
        }

        if (
                ticket.getAssignedAdmin() != null
        ) {

            result.put(
                    "assignedAdmin",
                    ticket.getAssignedAdmin()
                            .getName()
            );
        }

        Optional<TicketEvidence> evidence =
                ticketEvidenceRepository
                        .findFirstByTicketOrderByUploadedAtDesc(
                                ticket
                        );

        result.put(
                "evidenceImageUrl",
                evidence
                        .map(
                                TicketEvidence::getImageUrl
                        )
                        .orElse(null)
        );

        result.put(
                "evidenceFileName",
                evidence
                        .map(
                                TicketEvidence::getOriginalFileName
                        )
                        .orElse(null)
        );

        return result;
    }

    // =========================================================
    // ERROR
    // =========================================================

    private Map<String, String> error(
            String message
    ) {

        return Map.of(
                "message",
                message == null
                        ? "Request failed."
                        : message
        );
    }

    // =========================================================
    // REQUESTS
    // =========================================================

    public record ResponseRequest(
            String response,
            String status
    ) {
    }

    public record AssignRequest(
            String adminIdentifier
    ) {
    }
}