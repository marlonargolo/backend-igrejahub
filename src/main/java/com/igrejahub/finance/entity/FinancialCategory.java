package com.igrejahub.finance.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "finance_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FinancialCategory extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private FinancialTransaction.TransactionType type;

    @Column(name = "color", length = 9)
    private String color;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Igreja dona da categoria.
     * null = categoria global do sistema (visível para todas as igrejas da org).
     * preenchido = categoria privada da Igreja (visível apenas para ela).
     */
    @Column(name = "church_id")
    private Long churchId;
}