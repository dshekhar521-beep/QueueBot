package com.ticketintelligence.ticket_intelligence.service;

public record TicketIntelligenceResult(

        String category,

        String sentiment,

        String urgency,

        String escalationRisk,

        int priorityScore,

        String finalPriority,

        String priorityReason,

        String suggestedResponse

) {
}