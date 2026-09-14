package com.igrejahub.churches.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChurchRequest {
    @NotBlank(message = "Nome é obrigatório")
    private String name;
    private String city;
    private String state;
    private String address;
    private String zipCode;
    private String phone;
    private String email;
    private String cnpj;
    private String logoUrl;
    private Long pastorId;
    private Long planId;

    /** Nome do usuário administrador criado automaticamente para a Igreja. */
    private String adminName;
    /** Email do usuário administrador. Se ausente, usa o email da Igreja. */
    private String adminEmail;
}