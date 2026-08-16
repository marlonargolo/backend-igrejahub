package com.igrejahub.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Long organizationId;
    private String organizationName;
    private Boolean active;
    private Boolean verified;
    private LocalDateTime lastLoginAt;
    private Set<String> roles;
    private Set<String> permissions;
}
