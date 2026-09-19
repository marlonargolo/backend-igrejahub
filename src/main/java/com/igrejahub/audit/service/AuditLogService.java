package com.igrejahub.audit.service;

import com.igrejahub.audit.entity.AuditLog;
import com.igrejahub.audit.repository.AuditLogRepository;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;
    private final PlatformTransactionManager transactionManager;

    /**
     * Registra uma ação em log de auditoria. NUNCA deve derrubar a operação de
     * negócio que a chamou.
     *
     * Roda numa transação própria (REQUIRES_NEW, conexão separada da
     * chamadora) via TransactionTemplate — e não via @Transactional — porque
     * o commit precisa acontecer DENTRO do try/catch abaixo. No Postgres, um
     * erro em qualquer statement aborta toda a transação corrente até o
     * ROLLBACK; se o log de auditoria falhar (ex.: coluna ausente por
     * migration pendente) e essa falha não for isolada em transação própria,
     * o COMMIT da operação de negócio (criar usuário, etc.) falha junto —
     * mesmo capturando a exceção original em Java.
     */
    public void logAction(String action, String entityType, Long entityId, Object oldValues, Object newValues) {
        try {
            newRequiresNewTemplate().executeWithoutResult(status -> {
                HttpServletRequest request = getCurrentRequest();
                AuditLog auditLog = new AuditLog();
                auditLog.setOrganizationId(TenantContext.getCurrentTenant());
                auditLog.setChurchId(TenantContext.getCurrentChurchId());
                auditLog.setUserId(securityUtils.getCurrentUserId());
                auditLog.setUserEmail(securityUtils.getCurrentUserEmail());
                auditLog.setAction(action);
                auditLog.setEntityType(entityType);
                auditLog.setEntityId(entityId);
                auditLog.setOldValues(oldValues);
                auditLog.setNewValues(newValues);
                auditLog.setIp(request != null ? request.getRemoteAddr() : null);
                auditLog.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
                auditLogRepository.save(auditLog);
            });
        } catch (Exception e) {
            log.warn("Falha ao gravar log de auditoria (ação={}, entidade={}/{}): {}",
                action, entityType, entityId, e.getMessage());
        }
    }

    public void logAccountingAccessDenied() {
        try {
            newRequiresNewTemplate().executeWithoutResult(status -> {
                HttpServletRequest request = getCurrentRequest();
                AuditLog auditLog = new AuditLog();
                auditLog.setOrganizationId(TenantContext.getCurrentTenant());
                auditLog.setChurchId(TenantContext.getCurrentChurchId());
                auditLog.setUserId(securityUtils.getCurrentUserId());
                auditLog.setUserEmail(securityUtils.getCurrentUserEmail());
                auditLog.setAction("ACCOUNTING_ACCESS_DENIED");
                auditLog.setEntityType("ACCOUNTING");
                auditLog.setIp(request != null ? request.getRemoteAddr() : null);
                auditLog.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
                auditLogRepository.save(auditLog);
            });
        } catch (Exception e) {
            log.warn("Falha ao gravar log de auditoria (ACCOUNTING_ACCESS_DENIED): {}", e.getMessage());
        }
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable, String action, Long userId) {
        return getAuditLogs(pageable, action, userId, null, null, null);
    }

    /**
     * Usado pela administração externa (ROOT): filtros por período, Igreja,
     * usuário e tipo de ação. churchId só é livre para ROOT em modo global —
     * nos demais casos é forçado para a Igreja efetiva do usuário.
     */
    public Page<AuditLog> getAuditLogs(Pageable pageable, String action, Long userId,
                                        Long churchId, LocalDateTime startDate, LocalDateTime endDate) {
        Long orgId = TenantContext.getCurrentTenant();
        Long effectiveChurchId = securityUtils.canViewAll() ? churchId : securityUtils.getEffectiveChurchId();
        return auditLogRepository.findByFilters(
            orgId, effectiveChurchId, userId, action, startDate, endDate, pageable);
    }

    private TransactionTemplate newRequiresNewTemplate() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
