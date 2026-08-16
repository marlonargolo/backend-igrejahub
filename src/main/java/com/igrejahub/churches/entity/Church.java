package com.igrejahub.churches.entity;

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
@Table(name = "churches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Church extends BaseEntity {
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "city", length = 100)
    private String city;
    @Column(name = "state", length = 2)
    private String state;
    @Column(name = "address", length = 255)
    private String address;
    @Column(name = "zip_code", length = 10)
    private String zipCode;
    @Column(name = "phone", length = 20)
    private String phone;
    @Column(name = "email", length = 100)
    private String email;
    @Column(name = "cnpj", length = 18)
    private String cnpj;
    @Column(name = "logo_url", length = 500)
    private String logoUrl;
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";
    @Column(name = "pastor_id")
    private Long pastorId;
}
