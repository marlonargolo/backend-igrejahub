package com.igrejahub.accounting.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "accounting_periods")
@Data
public class AccountingPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long organizationId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
