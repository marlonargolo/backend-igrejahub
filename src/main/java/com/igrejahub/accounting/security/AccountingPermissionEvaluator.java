package com.igrejahub.accounting.security;

import com.igrejahub.accounting.entity.JournalEntry;
import com.igrejahub.accounting.repository.JournalEntryRepository;
import com.igrejahub.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("accountingPermissionEvaluator")
@RequiredArgsConstructor
public class AccountingPermissionEvaluator {

    private final JournalEntryRepository journalEntryRepository;

    public boolean canAccessEntry(Long journalEntryId) {
        Long orgId = TenantContext.getCurrentTenant();
        return journalEntryRepository.findById(journalEntryId)
                .map(JournalEntry::getOrganizationId)
                .map(orgId::equals)
                .orElse(false);
    }
}