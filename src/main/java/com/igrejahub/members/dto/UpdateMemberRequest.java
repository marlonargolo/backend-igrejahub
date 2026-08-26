package com.igrejahub.members.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRequest {
    private String name;
    private String email;
    private String phone;
    private String rg;
    private String cpf;
    private LocalDate birthDate;
    private String gender;
    private String maritalStatus;
    private String profession;
    private LocalDate baptismDate;
    private LocalDate memberSince;
    private String address;
    private String notes;
    private String cargo;
    private String funcoes;
    private String role;
    private String status;
}