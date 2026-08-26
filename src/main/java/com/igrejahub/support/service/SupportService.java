package com.igrejahub.support.service;

import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.notifications.service.NotificationService;
import com.igrejahub.support.dto.*;
import com.igrejahub.support.entity.SupportMessage;
import com.igrejahub.support.entity.SupportTicket;
import com.igrejahub.support.repository.SupportMessageRepository;
import com.igrejahub.support.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {

    private final SupportTicketRepository ticketRepo;
    private final SupportMessageRepository messageRepo;
    private final NotificationService notificationService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return 1L;
        try {
            return (Long) auth.getPrincipal().getClass().getMethod("getUserId").invoke(auth.getPrincipal());
        } catch (Exception e) { return 1L; }
    }

    private String getCurrentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "Usuário";
        return auth.getName();
    }

    public List<SupportTicketDto> listTickets() {
        Long orgId = TenantContext.getCurrentTenant();
        return ticketRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public SupportTicketDto getTicket(Long id) {
        SupportTicket t = ticketRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        return toDto(t);
    }

    @Transactional
    public SupportTicketDto createTicket(CreateTicketRequest req) {
        Long orgId = TenantContext.getCurrentTenant();
        Long userId = getCurrentUserId();
        String userName = getCurrentUserName();

        SupportTicket ticket = SupportTicket.builder()
            .organizationId(orgId)
            .userId(userId)
            .title(req.getTitle())
            .category(req.getCategory())
            .priority(req.getPriority() != null ? req.getPriority() : "MEDIA")
            .status("ABERTO")
            .build();
        ticket = ticketRepo.save(ticket);

        SupportMessage msg = SupportMessage.builder()
            .ticket(ticket)
            .userId(userId)
            .author(userName)
            .message(req.getDescricao())
            .type("cliente")
            .build();
        messageRepo.save(msg);

        notificationService.notifySupport(ticket.getId(), req.getTitle(), getCurrentUserName());
        log.info("Chamado #{} aberto por {}", ticket.getId(), getCurrentUserName());
        return getTicket(ticket.getId());
    }

    @Transactional
    public SupportMessageDto sendMessage(Long ticketId, SendMessageRequest req) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        SupportMessage msg = SupportMessage.builder()
            .ticket(ticket)
            .userId(getCurrentUserId())
            .author(getCurrentUserName())
            .message(req.getMessage())
            .type("cliente")
            .build();
        msg = messageRepo.save(msg);

        if ("RESOLVIDO".equals(ticket.getStatus()) || "FECHADO".equals(ticket.getStatus())) {
            ticket.setStatus("ABERTO");
            ticketRepo.save(ticket);
        }
        return toMessageDto(msg);
    }

    @Transactional
    public SupportTicketDto closeTicket(Long ticketId) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        ticket.setStatus("FECHADO");
        return toDto(ticketRepo.save(ticket));
    }

    private SupportTicketDto toDto(SupportTicket t) {
        List<SupportMessageDto> msgs = messageRepo
            .findByTicketIdOrderByCreatedAtAsc(t.getId())
            .stream().map(this::toMessageDto).collect(Collectors.toList());
        return SupportTicketDto.builder()
            .id(t.getId())
            .organizationId(t.getOrganizationId())
            .userId(t.getUserId())
            .title(t.getTitle())
            .category(t.getCategory())
            .priority(t.getPriority())
            .status(t.getStatus())
            .criadoEm(t.getCreatedAt())
            .mensagens(msgs)
            .build();
    }

    private SupportMessageDto toMessageDto(SupportMessage m) {
        return SupportMessageDto.builder()
            .id(m.getId())
            .autor(m.getAuthor())
            .texto(m.getMessage())
            .tipo(m.getType())
            .dataHora(m.getCreatedAt())
            .build();
    }
}
