package com.igrejahub.common.tenant;

public interface TenantAware {
    Long getOrganizationId();
    void setOrganizationId(Long organizationId);
}