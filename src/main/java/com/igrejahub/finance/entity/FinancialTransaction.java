package com.igrejahub.finance.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "financial_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FinancialTransaction extends BaseEntity {
    @Column(name = "church_id")
    private Long churchId;
    @Column(name = "congregation_id")
    private Long congregationId;
    @Column(name = "account_id")
    private Long accountId;
    @Column(name = "category_id")
    private Long categoryId;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;
    @Column(name = "description", nullable = false, length = 255)
    private String description;
    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;
    @Column(name = "reference", length = 100)
    private String reference;
    @Column(name = "notes", length = 500)
    private String notes;
    @Column(name = "approved_by")
    private Long approvedBy;
    @Column(name = "approved_at")
    private LocalDate approvedAt;
    @Column(name = "confirmed_at")
    private LocalDate confirmedAt;
    @Column(name = "cancelled_at")
    private LocalDate cancelledAt;
    @Column(name = "cancelled_by")
    private Long cancelledBy;
    @Column(name = "accounting_entry_id")
    private Long accountingEntryId;

    public enum TransactionType { REVENUE, EXPENSE }
    public enum TransactionStatus { PENDING, CONFIRMED, CANCELLED }
    public enum PaymentMethod { CASH, BANK_TRANSFER, CHECK, CREDIT_CARD, DEBIT_CARD, PIX, OTHER }
}
