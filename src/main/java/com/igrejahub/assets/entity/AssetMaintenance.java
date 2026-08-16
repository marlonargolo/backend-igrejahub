package com.igrejahub.assets.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "asset_maintenances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMaintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "cost_cents")
    private Long costCents;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }
}
