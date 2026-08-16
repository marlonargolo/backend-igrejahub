package com.igrejahub.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {
    private Long id;
    private String name;
    private String email;
    private Long organizationId;
    private String organizationName;
    private Set<String> roles;
    private Set<String> permissions;
}
