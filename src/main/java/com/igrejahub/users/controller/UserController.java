package com.igrejahub.users.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
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
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Endpoints de usuários")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Listar usuários")
    @GetMapping
    @PreAuthorize("hasPermission(null, 'USER_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserDto>>> getUsers(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String search) {

        Page<UserDto> page = userService.getUsers(pageable, search);

        PaginatedResponse<UserDto> response = PaginatedResponse.<UserDto>builder()
            .data(page.getContent())
            .meta(PaginatedResponse.PaginationMeta.builder()
                .page(page.getNumber() + 1)
                .pageSize(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build())
            .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Buscar usuário")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'USER_VIEW')")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUser(id)));
    }

    @Operation(summary = "Criar usuário")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'USER_CREATE')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.createUser(request)));
    }

    @Operation(summary = "Atualizar usuário")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(id, request)));
    }

    @Operation(summary = "Desabilitar usuário")
    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasPermission(null, 'USER_DISABLE')")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Habilitar usuário")
    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasPermission(null, 'USER_DISABLE')")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Alterar senha")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Listar igrejas vinculadas ao usuário")
    @GetMapping("/{id}/churches")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String,Object>>>> getUserChurches(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserChurches(id)));
    }

    @Operation(summary = "Vincular igrejas ao usuário")
    @PutMapping("/{id}/churches")
    public ResponseEntity<ApiResponse<Void>> setUserChurches(
            @PathVariable Long id,
            @RequestBody java.util.Set<Long> churchIds) {
        userService.setUserChurches(id, churchIds);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Vincular congregações ao usuário")
    @PutMapping("/{id}/congregations")
    public ResponseEntity<ApiResponse<Void>> setUserCongregations(
            @PathVariable Long id,
            @RequestBody java.util.Set<Long> congregationIds) {
        userService.setUserCongregations(id, congregationIds);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Listar congregações vinculadas ao usuário")
    @GetMapping("/{id}/congregations")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String,Object>>>> getUserCongregations(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserCongregations(id)));
    }
}