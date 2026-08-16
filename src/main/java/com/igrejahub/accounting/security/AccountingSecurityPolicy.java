package com.igrejahub.accounting.security;

import com.igrejahub.security.UserPrincipal;
import org.springframework.stereotype.Component;

@Component
public class AccountingSecurityPolicy {

    public boolean canView(UserPrincipal user) {
        return user.hasPermission("ROOT_ACCESS") || user.hasPermission("ACCOUNTING_VIEW");
    }

    public boolean canManage(UserPrincipal user) {
        return user.hasPermission("ROOT_ACCESS") || user.hasPermission("ACCOUNTING_MANAGE");
    }

    public boolean canPost(UserPrincipal user) {
        return user.hasPermission("ROOT_ACCESS") || user.hasPermission("ACCOUNTING_POST");
    }

    public boolean canClosePeriod(UserPrincipal user) {
        return user.hasPermission("ROOT_ACCESS") || user.hasPermission("ACCOUNTING_CLOSE_PERIOD");
    }
}