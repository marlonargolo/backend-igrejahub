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
    private Long churchId;        // escopo de Igreja
    private Long congregationId;  // escopo de Congregação (null = acesso à Igreja toda)
    private Set<String> roles;
    private Set<String> permissions;
}