package com.igrejahub.organizations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {
    private Long id;
    private String name;
    private String legalName;
    private String cnpj;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String logoUrl;
    private String plan;
    private Boolean active;
    private LocalDate trialEndDate;
    private LocalDate subscriptionEndDate;
}
