package com.igrejahub.notifications.service;

import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.notifications.dto.NotificationDto;
import com.igrejahub.notifications.entity.Notification;
import com.igrejahub.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repo;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return 1L;
        Object principal = auth.getPrincipal();
        try {
            return (Long) principal.getClass().getMethod("getUserId").invoke(principal);
        } catch (Exception e) {
            return 1L;
        }
    }

    public List<NotificationDto> listForCurrentUser() {
        return repo.findByUserIdOrderByCreatedAtDesc(getCurrentUserId())
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public Map<String, Long> countUnread() {
        return Map.of("unread", repo.countByUserIdAndReadFalse(getCurrentUserId()));
    }

    @Transactional
    public void markAllRead() {
        repo.markAllReadByUserId(getCurrentUserId());
    }

    @Transactional
    public void push(Long userId, Long orgId, String title, String body, String type, String link) {
        Notification n = Notification.builder()
            .userId(userId).organizationId(orgId)
            .title(title).body(body).type(type).link(link)
            .build();
        repo.save(n);
    }

    @Transactional
    public void notifySupport(Long ticketId, String titulo, String abertoPor) {
        Long orgId = TenantContext.getCurrentTenant();
        push(1L, orgId,
            "Novo chamado de suporte",
            "\"" + titulo + "\" aberto por " + abertoPor,
            "SUPPORT",
            "/suporte/chamados");
        log.info("Notificação de suporte — ticket #{}", ticketId);
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
            .id(n.getId()).title(n.getTitle()).body(n.getBody())
            .type(n.getType()).read(n.getRead()).link(n.getLink())
            .createdAt(n.getCreatedAt()).build();
    }
}
