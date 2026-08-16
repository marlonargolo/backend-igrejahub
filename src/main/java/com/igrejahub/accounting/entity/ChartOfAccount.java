package com.igrejahub.accounting.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "chart_of_accounts")
@Data
public class ChartOfAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long organizationId;
    private String code;
    private String name;
    private String accountType;
    private Long parentId;
    private Integer level;
    private String normalBalance;
    private Boolean analytical;
    private Boolean active;
}
