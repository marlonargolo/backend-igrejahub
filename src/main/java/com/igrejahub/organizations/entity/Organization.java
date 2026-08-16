package com.igrejahub.organizations.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Organization extends BaseEntity {
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "legal_name", length = 200)
    private String legalName;
    @Column(name = "cnpj", length = 18)
    private String cnpj;
    @Column(name = "email", length = 100)
    private String email;
    @Column(name = "phone", length = 20)
    private String phone;
    @Column(name = "address", length = 255)
    private String address;
    @Column(name = "city", length = 100)
    private String city;
    @Column(name = "state", length = 2)
    private String state;
    @Column(name = "zip_code", length = 10)
    private String zipCode;
    @Column(name = "country", length = 50)
    @Builder.Default private String country = "BR";
    @Column(name = "logo_url", length = 500)
    private String logoUrl;
    @Column(name = "plan", length = 50)
    @Builder.Default private String plan = "FREE";
    @Column(name = "is_active")
    @Builder.Default private boolean active = true;
    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;
    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;
}
