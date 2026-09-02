package com.igrejahub.support.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.notifications.service.NotificationService;
import com.igrejahub.security.SecurityUtils;
import com.igrejahub.security.UserPrincipal;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REGRAS DE ISOLAMENTO DE TICKETS:
 *
 *   ROOT global         → vê todos os tickets da organização
 *   ROOT com contexto   → vê tickets da Igreja atual
 *   Admin/Pastor Igreja → vê tickets da sua Igreja
 *   Pastor Congregação  → vê tickets da sua Congregação
 *   Usuário comum       → vê apenas seus próprios tickets
 *
 * CRIAÇÃO:
 *   churchId preenchido pelo TenantContext (não vem do request)
 *   congregationId preenchido pelo TenantContext se o usuário tiver
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {

    private final SupportTicketRepository ticketRepo;
    private final SupportMessageRepository messageRepo;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    // ── Identity helpers ──────────────────────────────────────────────────────

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return 1L;
        if (auth.getPrincipal() instanceof UserPrincipal u) return u.getId();
        Long fromCtx = TenantContext.getCurrentUserId();
        return fromCtx != null ? fromCtx : 1L;
    }

    private String getCurrentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "Usuário";
        if (auth.getPrincipal() instanceof UserPrincipal u) return u.getName();
        return auth.getName();
    }

    // ── Listagem ──────────────────────────────────────────────────────────────

    public List<SupportTicketDto> listTickets() {
        Long orgId    = TenantContext.getCurrentTenant();
        Long userId   = getCurrentUserId();
        Long churchId = TenantContext.getCurrentChurchId();
        Long congId   = TenantContext.getCurrentCongregationId();

        // ROOT global → todos
        if (securityUtils.canViewAll()) {
            return ticketRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId)
                .stream().map(this::toDto).collect(Collectors.toList());
        }

        // Pastor de Congregação → só da sua congregação
        if (congId != null && !securityUtils.isRoot()) {
            return ticketRepo.findByOrganizationIdAndCongregationId(orgId, congId)
                .stream().map(this::toDto).collect(Collectors.toList());
        }

        // ROOT com contexto ou Admin/Pastor → só da Igreja
        if (churchId != null) {
            return ticketRepo.findByOrganizationIdAndChurchId(orgId, churchId)
                .stream().map(this::toDto).collect(Collectors.toList());
        }

        // Usuário comum sem Igreja → só seus tickets
        return ticketRepo.findByOrganizationIdAndUserId(orgId, userId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public SupportTicketDto getTicket(Long id) {
        SupportTicket t = ticketRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        assertTicketAccess(t);
        return toDto(t);
    }

    // ── Criação ───────────────────────────────────────────────────────────────

    @Transactional
    public SupportTicketDto createTicket(CreateTicketRequest req) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long userId   = getCurrentUserId();
        String name   = getCurrentUserName();
        Long churchId = TenantContext.getCurrentChurchId();
        Long congId   = TenantContext.getCurrentCongregationId();

        // Não-ROOT sem Igreja: bloquear
        if (!securityUtils.canViewAll() && churchId == null) {
            throw new BusinessException("Seu usuário não está vinculado a nenhuma Igreja.");
        }

        SupportTicket ticket = SupportTicket.builder()
            .organizationId(orgId)
            .userId(userId)
            .churchId(churchId)         // vem do contexto, não do request
            .congregationId(congId)     // vem do contexto se houver
            .title(req.getTitle())
            .category(req.getCategory())
            .priority(req.getPriority() != null ? req.getPriority() : "MEDIA")
            .status("ABERTO")
            .build();
        ticket = ticketRepo.save(ticket);

        messageRepo.save(SupportMessage.builder()
            .ticket(ticket).userId(userId)
            .author(name).message(req.getDescricao()).type("cliente")
            .build());

        // Notificar ROOTs da Igreja do ticket
        notificationService.notifySupport(ticket.getId(), req.getTitle(), name, churchId);

        log.info("Chamado #{} aberto por {} church={}", ticket.getId(), name, churchId);
        return getTicket(ticket.getId());
    }

    // ── Mensagem ──────────────────────────────────────────────────────────────

    @Transactional
    public SupportMessageDto sendMessage(Long ticketId, SendMessageRequest req) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        assertTicketAccess(ticket);

        SupportMessage msg = SupportMessage.builder()
            .ticket(ticket).userId(getCurrentUserId())
            .author(getCurrentUserName()).message(req.getMessage()).type("cliente")
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
        assertTicketAccess(ticket);
        ticket.setStatus("FECHADO");
        return toDto(ticketRepo.save(ticket));
    }

    // ── Guard ─────────────────────────────────────────────────────────────────

    private void assertTicketAccess(SupportTicket t) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long userId   = getCurrentUserId();
        Long churchId = TenantContext.getCurrentChurchId();
        Long congId   = TenantContext.getCurrentCongregationId();

        if (!t.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado a este chamado.");
        }
        if (securityUtils.canViewAll()) return;

        // Pastor de Congregação
        if (congId != null && !securityUtils.isRoot()) {
            if (t.getCongregationId() == null || !t.getCongregationId().equals(congId)) {
                throw new BusinessException("Você não tem acesso a este chamado.");
            }
            return;
        }

        // Admin/Pastor ou ROOT com contexto
        if (churchId != null) {
            if (!churchId.equals(t.getChurchId())) {
                throw new BusinessException("Você não tem acesso a este chamado.");
            }
            return;
        }

        // Usuário comum: só seus tickets
        if (!t.getUserId().equals(userId)) {
            throw new BusinessException("Você não tem acesso a este chamado.");
        }
    }

    // ── DTO mappers ───────────────────────────────────────────────────────────

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