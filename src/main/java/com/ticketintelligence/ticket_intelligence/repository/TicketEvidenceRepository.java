package com.ticketintelligence.ticket_intelligence.repository;

import com.ticketintelligence.ticket_intelligence.entity.Ticket;
import com.ticketintelligence.ticket_intelligence.entity.TicketEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketEvidenceRepository
        extends JpaRepository<TicketEvidence, Long> {

    Optional<TicketEvidence>
    findFirstByTicketOrderByUploadedAtDesc(Ticket ticket);
}