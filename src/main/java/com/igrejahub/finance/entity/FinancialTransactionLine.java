package com.igrejahub.finance.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "financial_transaction_lines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FinancialTransactionLine extends BaseEntity {
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;
    @Column(name = "category_id")
    private Long categoryId;
    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;
    @Column(name = "description", length = 255)
    private String description;
}