package com.igrejahub.congregations.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "congregations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Congregation extends BaseEntity {
    @Column(name = "church_id", nullable = false)
    private Long churchId;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "city", length = 100)
    private String city;
    @Column(name = "state", length = 2)
    private String state;
    @Column(name = "address", length = 255)
    private String address;
    @Column(name = "pastor_id")
    private Long pastorId;
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";
    @Column(name = "latitude")
    private Double latitude;
    @Column(name = "longitude")
    private Double longitude;
}
