package com.igrejahub.roles.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import com.igrejahub.roles.dto.CreateRoleRequest;
import com.igrejahub.roles.dto.RoleDto;
import com.igrejahub.roles.dto.UpdateRoleRequest;
import com.igrejahub.roles.repository.RoleRepository;
import com.igrejahub.roles.service.RoleService;
import com.igrejahub.security.SecurityUtils;
import com.igrejahub.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class RoleController {

    private final RoleService roleService;
    private final RoleRepository roleRepository;
    private final SecurityUtils securityUtils;

    // Permissões que nunca aparecem em roles delegáveis para não-ROOT
    private static final Set<String> NEVER_DELEGATABLE = Set.of(
        "ROOT_ACCESS", "PLAN_MANAGE", "CHURCH_MANAGE",
        "BILLING_MANAGE", "BILLING_VIEW", "ACCOUNTING_ADMIN", "AUDIT_VIEW"
    );

    @GetMapping
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<RoleDto>>> getRoles(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<RoleDto> page = roleService.getRoles(pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.<RoleDto>builder()
            .data(page.getContent())
            .meta(PaginatedResponse.PaginationMeta.builder()
                .page(page.getNumber() + 1).pageSize(page.getSize())
                .total(page.getTotalElements()).totalPages(page.getTotalPages()).build())
            .build()));
    }

    /**
     * Roles que o usuário logado pode atribuir a outros.
     *
     * Lógica:
     * - ROOT: todas as roles exceto ROOT
     * - Outros: apenas roles cujas permissões são subconjunto das permissões do criador
     *   E que não contenham nenhuma permissão em NEVER_DELEGATABLE
     *
     * Isso garante que um usuário nunca cria outro com mais poder que ele próprio.
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAvailableRoles() {
        boolean isRoot = securityUtils.isRoot();

        // Permissões do criador (role + individuais)
        Set<String> callerPermissions = securityUtils.getCurrentUser()
            .map(UserPrincipal::getPermissions)
            .orElse(Set.of());

        List<RoleDto> available = roleRepository.findAll().stream()
            .filter(role -> role.isActive())
            .filter(role -> !"ROOT".equals(role.getName())) // ROOT nunca delegável
            .filter(role -> {
                if (isRoot) return true; // ROOT passa tudo exceto ROOT

                Set<String> rolePerms = role.getPermissions().stream()
                    .map(p -> p.getName())
                    .collect(Collectors.toSet());

                // Bloquear se contém permissões never delegatable
                if (rolePerms.stream().anyMatch(NEVER_DELEGATABLE::contains)) return false;

                // Bloquear se a role tem permissões que o criador não tem
                return callerPermissions.containsAll(rolePerms);
            })
            .map(role -> RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .active(role.isActive())
                .system(role.isSystem())
                .permissionNames(role.getPermissions().stream()
                    .map(p -> p.getName())
                    .collect(Collectors.toSet()))
                .build())
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(available));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<RoleDto>> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getRole(id)));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(roleService.createRole(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(
            @PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(roleService.updateRole(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}