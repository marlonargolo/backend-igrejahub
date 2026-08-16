package com.igrejahub.finance.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "financial_transfers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FinancialTransfer extends BaseEntity {
    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;
    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;
    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;
    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;
    @Column(name = "description", length = 255)
    private String description;
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "CONFIRMED";
}