package com.ticketintelligence.ticket_intelligence.dto;

public class TicketStats {

    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long criticalTickets;
    private long highPriorityTickets;
    private long negativeSentimentTickets;
    private long highEscalationRiskTickets;
    private double averagePriorityScore;

    public TicketStats(
            long totalTickets,
            long openTickets,
            long inProgressTickets,
            long criticalTickets,
            long highPriorityTickets,
            long negativeSentimentTickets,
            long highEscalationRiskTickets,
            double averagePriorityScore) {

        this.totalTickets = totalTickets;
        this.openTickets = openTickets;
        this.inProgressTickets = inProgressTickets;
        this.criticalTickets = criticalTickets;
        this.highPriorityTickets = highPriorityTickets;
        this.negativeSentimentTickets = negativeSentimentTickets;
        this.highEscalationRiskTickets = highEscalationRiskTickets;
        this.averagePriorityScore = averagePriorityScore;
    }

    public long getTotalTickets() {
        return totalTickets;
    }

    public long getOpenTickets() {
        return openTickets;
    }

    public long getInProgressTickets() {
        return inProgressTickets;
    }

    public long getCriticalTickets() {
        return criticalTickets;
    }

    public long getHighPriorityTickets() {
        return highPriorityTickets;
    }

    public long getNegativeSentimentTickets() {
        return negativeSentimentTickets;
    }

    public long getHighEscalationRiskTickets() {
        return highEscalationRiskTickets;
    }

    public double getAveragePriorityScore() {
        return averagePriorityScore;
    }
}