package com.igrejahub.members.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberRequest {
    @NotNull(message = "Igreja é obrigatória")
    private Long churchId;
    private Long congregationId;
    @NotBlank(message = "Nome é obrigatório")
    private String name;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String gender;
    private String maritalStatus;
    private String profession;
    private LocalDate baptismDate;
    private LocalDate memberSince;
    private String address;
    private String notes;
    private String role;
}
