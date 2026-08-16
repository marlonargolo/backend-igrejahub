package com.igrejahub.finance.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "finance_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FinancialAccount extends BaseEntity {
    @Column(name = "church_id")
    private Long churchId;
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AccountType type;
    @Column(name = "bank_name", length = 100)
    private String bankName;
    @Column(name = "agency", length = 20)
    private String agency;
    @Column(name = "account_number", length = 30)
    private String accountNumber;
    @Column(name = "initial_balance_cents", nullable = false)
    @Builder.Default
    private Long initialBalanceCents = 0L;
    @Column(name = "current_balance_cents", nullable = false)
    @Builder.Default
    private Long currentBalanceCents = 0L;
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum AccountType { CHECKING, SAVINGS, CASH, OTHER }
}