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
public class MemberListDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String status;
    private String congregationName;
    private LocalDate memberSince;
}
