package com.igrejahub.assets.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Asset extends BaseEntity {

    @Column(name = "church_id")
    private Long churchId;

    @Column(name = "congregation_id")
    private Long congregationId;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "original_value_cents")
    private Long originalValueCents;

    @Column(name = "current_value_cents")
    private Long currentValueCents;

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "responsible_member_id")
    private Long responsibleMemberId;

    @Column(name = "responsible_user_id")
    private Long responsibleUserId;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;
}
