package com.igrejahub.assets.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "asset_movements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "from_location", length = 255)
    private String fromLocation;

    @Column(name = "to_location", nullable = false, length = 255)
    private String toLocation;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(name = "responsible_user_id")
    private Long responsibleUserId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }
}
