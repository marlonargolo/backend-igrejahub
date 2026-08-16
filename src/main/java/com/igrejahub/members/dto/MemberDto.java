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
public class MemberDto {
    private Long id;
    private Long churchId;
    private String churchName;
    private Long congregationId;
    private String congregationName;
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
    private String avatarUrl;
    private String role;
    private String status;
}
