package com.ticketintelligence.ticket_intelligence.service;

import com.ticketintelligence.ticket_intelligence.entity.Feedback;
import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.Ticket;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.repository.FeedbackRepository;
import com.ticketintelligence.ticket_intelligence.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final TicketRepository ticketRepository;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            TicketRepository ticketRepository) {

        this.feedbackRepository = feedbackRepository;
        this.ticketRepository = ticketRepository;
    }

    public Feedback submitFeedback(
            User customer,
            Long ticketId,
            int rating,
            String comment) {

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "Rating must be between 1 and 5."
            );
        }

        Ticket ticket =
                ticketRepository
                        .findById(ticketId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Ticket not found."
                                ));

        if (ticket.getCreatedBy() == null ||
                !ticket.getCreatedBy()
                        .getId()
                        .equals(customer.getId())) {

            throw new IllegalArgumentException(
                    "You can only review your own ticket."
            );
        }

        if (!"RESOLVED".equalsIgnoreCase(
                ticket.getStatus())
                &&
                !"CLOSED".equalsIgnoreCase(
                        ticket.getStatus())) {

            throw new IllegalArgumentException(
                    "Feedback can be submitted after the ticket is resolved."
            );
        }

        if (feedbackRepository.existsByTicket(ticket)) {

            throw new IllegalArgumentException(
                    "Feedback has already been submitted for this ticket."
            );
        }

        Feedback feedback = new Feedback();

        feedback.setSeller(customer.getSeller());
        feedback.setCustomer(customer);
        feedback.setTicket(ticket);
        feedback.setRating(rating);
        feedback.setComment(
                comment == null
                        ? ""
                        : comment.trim()
        );

        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getSellerFeedback(
            Seller seller) {

        return feedbackRepository
                .findBySellerOrderByCreatedAtDesc(seller);
    }

    public List<Feedback> getCustomerFeedback(
            User customer) {

        return feedbackRepository
                .findByCustomerOrderByCreatedAtDesc(customer);
    }
}