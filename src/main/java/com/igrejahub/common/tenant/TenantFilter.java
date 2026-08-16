package com.igrejahub.common.tenant;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantFilter {

    private static final String FILTER_NAME = "tenantFilter";
    private static final String PARAM_NAME = "organizationId";

    public void enable(EntityManager entityManager) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            log.debug("Nenhum tenant no contexto, filtro não habilitado");
            return;
        }
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter(FILTER_NAME).setParameter(PARAM_NAME, tenantId);
    }

    public void disable(EntityManager entityManager) {
        Session session = entityManager.unwrap(Session.class);
        session.disableFilter(FILTER_NAME);
    }
}