package com.igrejahub.notifications.service;

import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.notifications.dto.NotificationDto;
import com.igrejahub.notifications.entity.Notification;
import com.igrejahub.notifications.repository.NotificationRepository;
import com.igrejahub.security.SecurityUtils;
import com.igrejahub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repo;
    private final SecurityUtils securityUtils;
    private final JdbcTemplate jdbcTemplate;

    // ── Helpers de identity ───────────────────────────────────────────────────

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return 1L;
        if (auth.getPrincipal() instanceof UserPrincipal u) return u.getId();
        return TenantContext.getCurrentUserId() != null ? TenantContext.getCurrentUserId() : 1L;
    }

    // ── Listagem para o usuário logado ────────────────────────────────────────

    public List<NotificationDto> listForCurrentUser() {
        Long userId   = getCurrentUserId();
        Long churchId = TenantContext.getCurrentChurchId();

        // ROOT modo global → todas as notificações do usuário
        if (securityUtils.canViewAll()) {
            return repo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
        }

        // Demais → filtradas pela Igreja atual
        return repo.findByUserIdAndChurchId(userId, churchId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public Map<String, Long> countUnread() {
        Long userId   = getCurrentUserId();
        Long churchId = TenantContext.getCurrentChurchId();

        if (securityUtils.canViewAll()) {
            return Map.of("unread", repo.countByUserIdAndReadFalse(userId));
        }
        return Map.of("unread", repo.countByUserIdAndReadFalseAndChurchId(userId, churchId));
    }

    @Transactional
    public void markAllRead() {
        Long userId   = getCurrentUserId();
        Long churchId = TenantContext.getCurrentChurchId();

        if (securityUtils.canViewAll()) {
            repo.markAllReadByUserId(userId);
        } else {
            repo.markAllReadByUserIdAndChurchId(userId, churchId);
        }
    }

    // ── Envio de notificação ──────────────────────────────────────────────────

    @Transactional
    public void push(Long userId, Long orgId, Long churchId,
                     String title, String body, String type, String link) {
        Notification n = Notification.builder()
            .userId(userId).organizationId(orgId).churchId(churchId)
            .title(title).body(body).type(type).link(link)
            .build();
        repo.save(n);
    }

    /**
     * Notifica os ROOTs da Igreja que abriu o ticket.
     * Se churchId for null → modo global, notifica todos os ROOTs da org.
     */
    @Transactional
    public void notifySupport(Long ticketId, String titulo, String abertoPor, Long churchId) {
        Long orgId = TenantContext.getCurrentTenant();
        List<Long> rootIds = findRootUsers(orgId, churchId);

        for (Long userId : rootIds) {
            push(userId, orgId, churchId,
                "Novo chamado de suporte",
                "\"" + titulo + "\" aberto por " + abertoPor,
                "SUPPORT",
                "/suporte/chamados");
        }
        log.info("Notificação ticket #{} → {} ROOT(s) | church={}", ticketId, rootIds.size(), churchId);
    }

    /**
     * Sobrecarga legada — usado por código que não tem churchId.
     * Notifica apenas o ROOT global (userId = ID do primeiro ROOT da org).
     */
    @Transactional
    public void notifySupport(Long ticketId, String titulo, String abertoPor) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long churchId = TenantContext.getCurrentChurchId(); // pode ser null se ROOT global
        notifySupport(ticketId, titulo, abertoPor, churchId);
    }

    // ── Buscar ROOTs da organização ───────────────────────────────────────────

    private List<Long> findRootUsers(Long orgId, Long churchId) {
        try {
            if (churchId == null) {
                // ROOT modo global: todos os ROOTs da organização
                return jdbcTemplate.queryForList(
                    "SELECT u.id FROM users u " +
                    "JOIN user_roles ur ON u.id = ur.user_id " +
                    "JOIN roles r ON ur.role_id = r.id " +
                    "WHERE u.organization_id = ? AND r.name = 'ROOT' " +
                    "AND u.is_active = true AND (u.deleted IS NULL OR u.deleted = false)",
                    Long.class, orgId);
            } else {
                // ROOTs com acesso à Igreja específica via user_church_access
                List<Long> rooted = jdbcTemplate.queryForList(
                    "SELECT DISTINCT u.id FROM users u " +
                    "JOIN user_roles ur ON u.id = ur.user_id " +
                    "JOIN roles r ON ur.role_id = r.id " +
                    "LEFT JOIN user_church_access uca ON u.id = uca.user_id " +
                    "WHERE u.organization_id = ? AND r.name = 'ROOT' " +
                    "AND u.is_active = true AND (u.deleted IS NULL OR u.deleted = false) " +
                    "AND (uca.church_id = ? OR u.church_id IS NULL)",
                    Long.class, orgId, churchId);
                // Fallback: se não encontrar ROOT com vínculo, notifica todos os ROOTs
                if (rooted.isEmpty()) {
                    return jdbcTemplate.queryForList(
                        "SELECT u.id FROM users u " +
                        "JOIN user_roles ur ON u.id = ur.user_id " +
                        "JOIN roles r ON ur.role_id = r.id " +
                        "WHERE u.organization_id = ? AND r.name = 'ROOT' " +
                        "AND u.is_active = true AND (u.deleted IS NULL OR u.deleted = false)",
                        Long.class, orgId);
                }
                return rooted;
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar ROOTs para notificação: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
            .id(n.getId()).title(n.getTitle()).body(n.getBody())
            .type(n.getType()).read(n.getRead()).link(n.getLink())
            .createdAt(n.getCreatedAt()).build();
    }
}