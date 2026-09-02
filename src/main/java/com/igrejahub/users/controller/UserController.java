package com.igrejahub.users.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import com.igrejahub.permissions.dto.PermissionDto;
import com.igrejahub.permissions.repository.PermissionRepository;
import com.igrejahub.security.SecurityUtils;
import com.igrejahub.users.dto.ChangePasswordRequest;
import com.igrejahub.users.dto.CreateUserRequest;
import com.igrejahub.users.dto.UpdateUserRequest;
import com.igrejahub.users.dto.UserDto;
import com.igrejahub.users.service.UserService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@Tag(name = "Users")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;
    private final PermissionRepository permissionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SecurityUtils securityUtils;

    private static final Set<String> BLOCKED_INDIVIDUAL = Set.of(
        "ROOT_ACCESS", "BILLING_MANAGE", "BILLING_VIEW",
        "ACCOUNTING_ADMIN", "AUDIT_VIEW"
    );

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasPermission(null, 'USER_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserDto>>> getUsers(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String search) {
        Page<UserDto> page = userService.getUsers(pageable, search);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.<UserDto>builder()
            .data(page.getContent())
            .meta(PaginatedResponse.PaginationMeta.builder()
                .page(page.getNumber() + 1).pageSize(page.getSize())
                .total(page.getTotalElements()).totalPages(page.getTotalPages()).build())
            .build()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'USER_VIEW')")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUser(id)));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'USER_CREATE')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.createUser(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(id, request)));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasPermission(null, 'USER_DISABLE')")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasPermission(null, 'USER_DISABLE')")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * Exclusão permanente (soft delete — some da listagem imediatamente).
     * Requer USER_DISABLE ou ROOT_ACCESS.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'USER_DISABLE')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── Vínculos ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}/churches")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserChurches(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserChurches(id)));
    }

    @PutMapping("/{id}/churches")
    public ResponseEntity<ApiResponse<Void>> setUserChurches(
            @PathVariable Long id, @RequestBody Set<Long> churchIds) {
        userService.setUserChurches(id, churchIds);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}/congregations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserCongregations(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserCongregations(id)));
    }

    @PutMapping("/{id}/congregations")
    public ResponseEntity<ApiResponse<Void>> setUserCongregations(
            @PathVariable Long id, @RequestBody Set<Long> congregationIds) {
        userService.setUserCongregations(id, congregationIds);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── Permissões individuais ────────────────────────────────────────────────

    @GetMapping("/permissions/available")
    @PreAuthorize("hasPermission(null, 'USER_ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAvailablePermissions() {
        boolean isRoot = securityUtils.isRoot();
        List<PermissionDto> list = permissionRepository.findByActiveTrue().stream()
            .filter(p -> {
                if ("ROOT_ACCESS".equals(p.getName())) return false;
                if (!isRoot && BLOCKED_INDIVIDUAL.contains(p.getName())) return false;
                if (!isRoot && "SYSTEM".equals(p.getCategory())) return false;
                if (!isRoot && "BILLING".equals(p.getCategory())) return false;
                return true;
            })
            .map(p -> PermissionDto.builder()
                .id(p.getId()).name(p.getName())
                .description(p.getDescription())
                .category(p.getCategory())
                .active(p.isActive()).system(p.isSystem()).build())
            .sorted((a, b) -> {
                int c = (a.getCategory() == null ? "" : a.getCategory())
                    .compareTo(b.getCategory() == null ? "" : b.getCategory());
                return c != 0 ? c : a.getName().compareTo(b.getName());
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasPermission(null, 'USER_ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<String>>> getUserPermissions(@PathVariable Long id) {
        List<String> perms = jdbcTemplate.queryForList(
            "SELECT p.name FROM permissions p " +
            "JOIN user_permissions up ON p.id = up.permission_id WHERE up.user_id = ?",
            String.class, id);
        return ResponseEntity.ok(ApiResponse.success(perms));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasPermission(null, 'USER_ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> setUserPermissions(
            @PathVariable Long id, @RequestBody Set<String> permissionNames) {
        Long grantedBy = securityUtils.getCurrentUserId();
        boolean isRoot = securityUtils.isRoot();
        Set<String> toGrant = permissionNames.stream()
            .filter(n -> isRoot || !BLOCKED_INDIVIDUAL.contains(n))
            .collect(Collectors.toSet());
        jdbcTemplate.update("DELETE FROM user_permissions WHERE user_id = ?", id);
        for (String name : toGrant) {
            permissionRepository.findByName(name).ifPresent(p ->
                jdbcTemplate.update(
                    "INSERT INTO user_permissions(user_id,permission_id,granted_by) VALUES(?,?,?) ON CONFLICT DO NOTHING",
                    id, p.getId(), grantedBy));
        }
        return ResponseEntity.ok(ApiResponse.success());
    }
}