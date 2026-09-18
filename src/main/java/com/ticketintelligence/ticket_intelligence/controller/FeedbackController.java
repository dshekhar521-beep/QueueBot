package com.ticketintelligence.ticket_intelligence.controller;

import com.ticketintelligence.ticket_intelligence.entity.Feedback;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.service.FeedbackService;
import com.ticketintelligence.ticket_intelligence.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserService userService;

    public FeedbackController(
            FeedbackService feedbackService,
            UserService userService) {

        this.feedbackService = feedbackService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> submit(
            @RequestBody FeedbackRequest request,
            Authentication authentication) {

        try {

            User customer =
                    currentUser(authentication);

            Feedback feedback =
                    feedbackService.submitFeedback(
                            customer,
                            request.ticketId(),
                            request.rating(),
                            request.comment()
                    );

            return ResponseEntity.ok(
                    toMap(feedback)
            );

        } catch (Exception ex) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            ex.getMessage()
                    ));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> myFeedback(
            Authentication authentication) {

        User customer =
                currentUser(authentication);

        return ResponseEntity.ok(
                feedbackService
                        .getCustomerFeedback(customer)
                        .stream()
                        .map(this::toMap)
                        .toList()
        );
    }

    @GetMapping("/seller")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> sellerFeedback(
            Authentication authentication) {

        User admin =
                currentUser(authentication);

        List<Feedback> feedback =
                feedbackService.getSellerFeedback(
                        admin.getSeller()
                );

        double average =
                feedback.stream()
                        .mapToInt(Feedback::getRating)
                        .average()
                        .orElse(0);

        long positive =
                feedback.stream()
                        .filter(f -> f.getRating() >= 4)
                        .count();

        long negative =
                feedback.stream()
                        .filter(f -> f.getRating() <= 2)
                        .count();

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "averageRating",
                Math.round(average * 10.0) / 10.0
        );

        response.put("total", feedback.size());
        response.put("positive", positive);
        response.put("negative", negative);

        response.put(
                "items",
                feedback.stream()
                        .map(this::toMap)
                        .toList()
        );

        return ResponseEntity.ok(response);
    }

    private User currentUser(
            Authentication authentication) {

        String[] parts =
                authentication.getName()
                        .split("\\|", 2);

        return userService.findByLoginIdentifier(
                parts[0],
                parts[1]
        );
    }

    private Map<String, Object> toMap(
            Feedback feedback) {

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put("id", feedback.getId());
        map.put("rating", feedback.getRating());
        map.put("comment", feedback.getComment());
        map.put("createdAt", feedback.getCreatedAt());

        map.put(
                "customer",
                Map.of(
                        "customerId",
                        feedback.getCustomer()
                                .getCustomerId(),
                        "name",
                        feedback.getCustomer().getName()
                )
        );

        map.put(
                "ticket",
                Map.of(
                        "id",
                        feedback.getTicket().getId(),
                        "title",
                        feedback.getTicket().getTitle()
                )
        );

        return map;
    }

    public record FeedbackRequest(
            Long ticketId,
            int rating,
            String comment
    ) {
    }
}