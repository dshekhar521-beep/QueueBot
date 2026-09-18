package com.ticketintelligence.ticket_intelligence.controller;

import com.ticketintelligence.ticket_intelligence.entity.Ticket;
import com.ticketintelligence.ticket_intelligence.entity.TicketAttachment;
import com.ticketintelligence.ticket_intelligence.repository.TicketRepository;
import com.ticketintelligence.ticket_intelligence.service.FileStorageService;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets")
public class TicketAttachmentController {

    private final TicketRepository ticketRepository;
    private final FileStorageService fileStorageService;

    public TicketAttachmentController(
            TicketRepository ticketRepository,
            FileStorageService fileStorageService) {

        this.ticketRepository = ticketRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<?> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        try {

            Ticket ticket =
                    ticketRepository.findById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Ticket not found."
                                    )
                            );

            String username =
                    authentication.getName();

            String[] parts =
                    username.split("\\|", 2);

            boolean admin =
                    authentication.getAuthorities()
                            .stream()
                            .anyMatch(a ->
                                    a.getAuthority()
                                            .equals("ROLE_ADMIN")
                                            ||
                                            a.getAuthority()
                                                    .equals("ROLE_SUPER_ADMIN")
                            );

            if (!admin) {

                if (parts.length != 2 ||
                        !ticket.getCreatedBy()
                                .getPhone()
                                .equals(parts[1])) {

                    return ResponseEntity
                            .status(403)
                            .body(
                                    "You cannot upload to this ticket."
                            );
                }
            }

            String url =
                    fileStorageService.saveFile(
                            file,
                            "tickets"
                    );

            TicketAttachment attachment =
                    new TicketAttachment();

            attachment.setFileName(
                    file.getOriginalFilename()
            );

            attachment.setFileUrl(url);

            attachment.setContentType(
                    file.getContentType()
            );

            attachment.setFileSize(
                    file.getSize()
            );

            attachment.setTicket(ticket);

            ticket.getAttachments()
                    .add(attachment);

            ticketRepository.save(ticket);

            return ResponseEntity.ok(
                    attachment
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}