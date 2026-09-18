package com.ticketintelligence.ticket_intelligence.controller;

import com.ticketintelligence.ticket_intelligence.entity.Order;
import com.ticketintelligence.ticket_intelligence.entity.Ticket;
import com.ticketintelligence.ticket_intelligence.entity.TicketEvidence;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.repository.OrderRepository;
import com.ticketintelligence.ticket_intelligence.repository.TicketEvidenceRepository;
import com.ticketintelligence.ticket_intelligence.repository.TicketRepository;
import com.ticketintelligence.ticket_intelligence.service.TicketIntelligenceResult;
import com.ticketintelligence.ticket_intelligence.service.TicketIntelligenceService;
import com.ticketintelligence.ticket_intelligence.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
public class CustomerPortalController {

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final TicketEvidenceRepository ticketEvidenceRepository;
    private final TicketIntelligenceService
            ticketIntelligenceService;

    private final Path uploadDirectory =
            Paths.get("uploads", "tickets");

    public CustomerPortalController(
            UserService userService,
            OrderRepository orderRepository,
            TicketRepository ticketRepository,
            TicketEvidenceRepository ticketEvidenceRepository,
            TicketIntelligenceService ticketIntelligenceService
    ) {
        this.userService = userService;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.ticketEvidenceRepository =
                ticketEvidenceRepository;
        this.ticketIntelligenceService =
                ticketIntelligenceService;
    }

    // ============================================================
    // CUSTOMER ORDERS
    //
    // Returns empty list for a customer who has not purchased yet.
    // ============================================================

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrders(
            Authentication authentication
    ) {

        try {

            User customer =
                    resolveCustomer(authentication);

            List<Order> orders =
                    orderRepository
                            .findByCustomerOrderByIdDesc(
                                    customer
                            );

            List<Map<String, Object>> result =
                    orders.stream()
                            .map(this::orderToMap)
                            .toList();

            return ResponseEntity.ok(
                    result
            );

        } catch (Exception e) {

            return badRequest(
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // SINGLE ORDER
    // ============================================================

    @GetMapping("/orders/{orderId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrder(
            @PathVariable String orderId,
            Authentication authentication
    ) {

        try {

            User customer =
                    resolveCustomer(authentication);

            Order order =
                    findCustomerOrder(
                            customer,
                            orderId
                    );

            if (order == null) {

                return ResponseEntity.status(
                        HttpStatus.NOT_FOUND
                ).body(
                        Map.of(
                                "success",
                                false,
                                "message",
                                "Order not found."
                        )
                );
            }

            return ResponseEntity.ok(
                    orderToMap(order)
            );

        } catch (Exception e) {

            return badRequest(
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // CUSTOMER TICKETS
    // ============================================================

    @GetMapping("/tickets")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getTickets(
            Authentication authentication
    ) {

        try {

            User customer =
                    resolveCustomer(authentication);

            List<Ticket> tickets =
                    ticketRepository
                            .findByCreatedByOrderByCreatedAtDesc(
                                    customer
                            );

            List<Map<String, Object>> result =
                    tickets.stream()
                            .map(this::ticketToMap)
                            .toList();

            return ResponseEntity.ok(
                    result
            );

        } catch (Exception e) {

            return badRequest(
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // CREATE CUSTOMER TICKET
    // ============================================================

    @PostMapping("/tickets")
    @Transactional
    public ResponseEntity<?> createTicket(
            @RequestBody CustomerTicketRequest request,
            Authentication authentication
    ) {

        try {

            User customer =
                    resolveCustomer(authentication);

            if (request.orderId() == null ||
                    request.orderId().isBlank()) {

                return badRequest(
                        "Order ID is required."
                );
            }

            if (request.description() == null ||
                    request.description().isBlank()) {

                return badRequest(
                        "Ticket description is required."
                );
            }

            Order order =
                    findCustomerOrder(
                            customer,
                            request.orderId()
                    );

            if (order == null) {

                return ResponseEntity.status(
                        HttpStatus.FORBIDDEN
                ).body(
                        Map.of(
                                "success",
                                false,
                                "message",
                                "This order does not belong to your account."
                        )
                );
            }

            Ticket ticket =
                    new Ticket();

            ticket.setTicketId(
                    generateTicketId()
            );

            ticket.setTitle(
                    isBlank(request.title())
                            ? "Customer Support Request"
                            : request.title().trim()
            );

            ticket.setDescription(
                    request.description().trim()
            );

            ticket.setIssueType(
                    isBlank(request.issueType())
                            ? "GENERAL"
                            : request.issueType()
                            .trim()
            );

            ticket.setStatus(
                    "OPEN"
            );

            ticket.setCreatedAt(
                    LocalDateTime.now()
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

            // ----------------------------------------------------
            // AI ANALYSIS
            // ----------------------------------------------------

            TicketIntelligenceResult intelligence =
                    ticketIntelligenceService
                            .analyzeTicket(
                                    ticket.getDescription()
                            );

            ticket.setCategory(
                    intelligence.category()
            );

            ticket.setSentiment(
                    intelligence.sentiment()
            );

            ticket.setUrgency(
                    intelligence.urgency()
            );

            ticket.setEscalationRisk(
                    intelligence.escalationRisk()
            );

            ticket.setPriorityScore(
                    intelligence.priorityScore()
            );

            ticket.setPriority(
                    intelligence.finalPriority()
            );

            ticket.setPriorityReason(
                    intelligence.priorityReason()
            );

            ticket.setSuggestedResponse(
                    intelligence.suggestedResponse()
            );

            Ticket saved =
                    ticketRepository.save(ticket);

            // ----------------------------------------------------
            // Evidence
            // ----------------------------------------------------

            if (!isBlank(
                    request.evidenceImageUrl()
            )) {

                TicketEvidence evidence =
                        new TicketEvidence();

                evidence.setTicket(saved);

                evidence.setImageUrl(
                        request.evidenceImageUrl()
                );

                evidence.setUploadedAt(
                        LocalDateTime.now()
                );

                ticketEvidenceRepository.save(
                        evidence
                );
            }

            return ResponseEntity.ok(
                    ticketToMap(saved)
            );

        } catch (Exception e) {

            return badRequest(
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // IMAGE UPLOAD
    // ============================================================

    @PostMapping("/tickets/upload-image")
    public ResponseEntity<?> uploadImage(
            @RequestParam("image")
            MultipartFile image,
            Authentication authentication
    ) {

        try {

            // Ensure the user is authenticated
            resolveCustomer(authentication);

            if (image == null ||
                    image.isEmpty()) {

                return badRequest(
                        "Image is required."
                );
            }

            if (image.getSize() >
                    5 * 1024 * 1024) {

                return badRequest(
                        "Image must be 5 MB or smaller."
                );
            }

            String contentType =
                    image.getContentType();

            if (contentType == null ||
                    (!contentType.equalsIgnoreCase(
                            "image/jpeg"
                    ) &&
                            !contentType.equalsIgnoreCase(
                                    "image/png"
                            ) &&
                            !contentType.equalsIgnoreCase(
                                    "image/webp"
                            ))) {

                return badRequest(
                        "Only JPG, PNG and WEBP images are allowed."
                );
            }

            Files.createDirectories(
                    uploadDirectory
            );

            String extension =
                    getExtension(
                            image.getOriginalFilename(),
                            contentType
                    );

            String fileName =
                    UUID.randomUUID()
                            .toString()
                            + extension;

            Path target =
                    uploadDirectory.resolve(
                            fileName
                    );

            Files.copy(
                    image.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String url =
                    "/uploads/tickets/" +
                            fileName;

            return ResponseEntity.ok(
                    Map.of(
                            "success",
                            true,
                            "imageUrl",
                            url,
                            "originalFileName",
                            image.getOriginalFilename()
                    )
            );

        } catch (IOException e) {

            return badRequest(
                    "Unable to save image."
            );

        } catch (Exception e) {

            return badRequest(
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // RESOLVE CURRENT CUSTOMER
    // ============================================================

    private User resolveCustomer(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "Customer authentication required."
            );
        }

        String username =
                authentication.getName();

        if (username == null ||
                username.isBlank()) {

            throw new IllegalStateException(
                    "Invalid customer authentication."
            );
        }

        /*
         * Expected:
         *
         * CUST-XXXXXX|PHONE
         * CUST-XXXXXX|EMAIL
         */

        if (username.contains("|")) {

            String[] parts =
                    username.split(
                            "\\|",
                            2
                    );

            if (parts.length == 2) {

                User customer =
                        userService.findByLoginIdentifier(
                                parts[0].trim(),
                                parts[1].trim()
                        );

                if (customer != null &&
                        ("ROLE_USER".equalsIgnoreCase(
                                customer.getRole()
                        ) ||
                                "USER".equalsIgnoreCase(
                                        customer.getRole()
                                ))) {

                    return customer;
                }
            }
        }

        /*
         * Fallback to Customer ID.
         */

        if (username
                .toUpperCase()
                .startsWith("CUST-")) {

            User customer =
                    userService.findByCustomerId(
                            username
                    );

            if (customer != null) {
                return customer;
            }
        }

        throw new IllegalStateException(
                "Customer account could not be resolved."
        );
    }

    // ============================================================
    // FIND CUSTOMER ORDER
    // ============================================================

    private Order findCustomerOrder(
            User customer,
            String orderId
    ) {

        if (orderId == null ||
                orderId.isBlank()) {

            return null;
        }

        String value =
                orderId.trim();

        /*
         * First try database Order ID.
         */

        try {

            Long databaseId =
                    Long.parseLong(value);

            OptionalOrder:
            {
                Order order =
                        orderRepository
                                .findById(
                                        databaseId
                                )
                                .orElse(null);

                if (order != null &&
                        order.getCustomer()
                                .getId()
                                .equals(
                                        customer.getId()
                                )) {

                    return order;
                }
            }

        } catch (NumberFormatException ignored) {
        }

        /*
         * Then try business Order ID.
         */

        Order order =
                orderRepository
                        .findByOrderId(value)
                        .orElse(null);

        if (order != null &&
                order.getCustomer()
                        .getId()
                        .equals(
                                customer.getId()
                        )) {

            return order;
        }

        return null;
    }

    // ============================================================
    // ORDER MAPPER
    // ============================================================

    private Map<String, Object> orderToMap(
            Order order
    ) {

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put(
                "id",
                order.getId()
        );

        map.put(
                "orderId",
                order.getOrderId()
        );

        map.put(
                "quantity",
                order.getQuantity()
        );

        map.put(
                "amount",
                order.getAmount()
        );

        map.put(
                "orderDate",
                order.getOrderDate()
        );

        map.put(
                "deliveryDate",
                order.getDeliveryDate()
        );

        map.put(
                "trackingNumber",
                order.getTrackingNumber()
        );

        map.put(
                "paymentStatus",
                order.getPaymentStatus()
        );

        map.put(
                "status",
                order.getStatus()
        );

        map.put(
                "customerPhone",
                order.getCustomerPhone()
        );

        map.put(
                "customerEmail",
                order.getCustomerEmail()
        );

        if (order.getSeller() != null) {

            map.put(
                    "sellerId",
                    order.getSeller()
                            .getSellerId()
            );

            map.put(
                    "businessName",
                    order.getSeller()
                            .getBusinessName()
            );

        } else {

            map.put(
                    "sellerId",
                    null
            );

            map.put(
                    "businessName",
                    null
            );
        }

        if (order.getProduct() != null) {

            map.put(
                    "productId",
                    order.getProduct()
                            .getProductId()
            );

            map.put(
                    "productName",
                    order.getProduct()
                            .getName()
            );

            map.put(
                    "productImageUrl",
                    order.getProduct()
                            .getImageUrl()
            );

            map.put(
                    "productPrice",
                    order.getProduct()
                            .getPrice()
            );

            map.put(
                    "productBrand",
                    order.getProduct()
                            .getBrand()
            );

        } else {

            map.put(
                    "productId",
                    null
            );

            map.put(
                    "productName",
                    null
            );

            map.put(
                    "productImageUrl",
                    null
            );

            map.put(
                    "productPrice",
                    null
            );

            map.put(
                    "productBrand",
                    null
            );
        }

        return map;
    }

    // ============================================================
    // TICKET MAPPER
    // ============================================================

    private Map<String, Object> ticketToMap(
            Ticket ticket
    ) {

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put(
                "id",
                ticket.getId()
        );

        map.put(
                "ticketId",
                ticket.getTicketId()
        );

        map.put(
                "title",
                ticket.getTitle()
        );

        map.put(
                "description",
                ticket.getDescription()
        );

        map.put(
                "issueType",
                ticket.getIssueType()
        );

        map.put(
                "category",
                ticket.getCategory()
        );

        map.put(
                "priority",
                ticket.getPriority()
        );

        map.put(
                "priorityScore",
                ticket.getPriorityScore()
        );

        map.put(
                "priorityReason",
                ticket.getPriorityReason()
        );

        map.put(
                "sentiment",
                ticket.getSentiment()
        );

        map.put(
                "urgency",
                ticket.getUrgency()
        );

        map.put(
                "escalationRisk",
                ticket.getEscalationRisk()
        );

        map.put(
                "suggestedResponse",
                ticket.getSuggestedResponse()
        );

        map.put(
                "adminResponse",
                ticket.getAdminResponse()
        );

        map.put(
                "status",
                ticket.getStatus()
        );

        map.put(
                "createdAt",
                ticket.getCreatedAt()
        );

        map.put(
                "respondedAt",
                ticket.getRespondedAt()
        );

        map.put(
                "resolvedAt",
                ticket.getResolvedAt()
        );

        if (ticket.getOrder() != null) {

            map.put(
                    "orderId",
                    ticket.getOrder()
                            .getOrderId()
            );

        } else {

            map.put(
                    "orderId",
                    null
            );
        }

        if (ticket.getSeller() != null) {

            map.put(
                    "sellerId",
                    ticket.getSeller()
                            .getSellerId()
            );

        } else {

            map.put(
                    "sellerId",
                    null
            );
        }

        return map;
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private String generateTicketId() {

        return "TKT-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }

    private boolean isBlank(
            String value
    ) {

        return value == null ||
                value.trim().isEmpty();
    }

    private String getExtension(
            String originalName,
            String contentType
    ) {

        if (originalName != null &&
                originalName.contains(".")) {

            String extension =
                    originalName
                            .substring(
                                    originalName
                                            .lastIndexOf(".")
                            )
                            .toLowerCase();

            if (extension.equals(".jpg") ||
                    extension.equals(".jpeg") ||
                    extension.equals(".png") ||
                    extension.equals(".webp")) {

                return extension;
            }
        }

        if ("image/png".equalsIgnoreCase(
                contentType
        )) {

            return ".png";
        }

        if ("image/webp".equalsIgnoreCase(
                contentType
        )) {

            return ".webp";
        }

        return ".jpg";
    }

    private ResponseEntity<?> badRequest(
            String message
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "success",
                                false,
                                "message",
                                message == null ||
                                        message.isBlank()
                                        ? "Something went wrong."
                                        : message
                        )
                );
    }

    // ============================================================
    // REQUEST DTO
    // ============================================================

    public record CustomerTicketRequest(
            String orderId,
            String title,
            String description,
            String issueType,
            String evidenceImageUrl
    ) {}
}