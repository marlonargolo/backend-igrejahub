package com.igrejahub.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
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
    /**
     * true (padrão) = pode ver a Igreja inteira.
     * false = restrito só a linkedCongregationIds — nunca vê a Igreja toda,
     * mesmo sem congregationId fixo.
     */
    private boolean accessMainChurch;
    /** Congregações vinculadas (user_congregation_access) — só relevante quando accessMainChurch=false. */
    private List<Long> linkedCongregationIds;
    /** Chaves dos módulos habilitados para a Igreja do usuário (Admin Externa → Módulos). */
    private List<String> enabledModules;
    private Set<String> roles;
    private Set<String> permissions;
}