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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public void logAction(String action, String entityType, Long entityId, Object oldValues, Object newValues) {
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
    }

    @Transactional
    public void logAccountingAccessDenied() {
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

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
