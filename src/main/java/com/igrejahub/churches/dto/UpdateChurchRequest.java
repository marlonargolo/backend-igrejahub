package com.igrejahub.churches.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChurchRequest {
    private String name;
    private String city;
    private String state;
    private String address;
    private String zipCode;
    private String phone;
    private String email;
    private String cnpj;
    private String logoUrl;
    private String status;
    private Long pastorId;
}