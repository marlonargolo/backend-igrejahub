package com.igrejahub.members.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import com.igrejahub.members.dto.CreateMemberRequest;
import com.igrejahub.members.dto.MemberDto;
import com.igrejahub.members.dto.UpdateMemberRequest;
import com.igrejahub.members.service.MemberService;
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

@RestController
@RequestMapping("/members")
@Tag(name = "Members", description = "Endpoints de membros")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "Listar membros")
    @GetMapping
    @PreAuthorize("hasPermission(null, 'MEMBER_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<MemberDto>>> getMembers(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) Long congregationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        Page<MemberDto> page = memberService.getMembers(pageable, congregationId, status, search);

        PaginatedResponse<MemberDto> response = PaginatedResponse.<MemberDto>builder()
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

    @Operation(summary = "Buscar membro")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'MEMBER_VIEW')")
    public ResponseEntity<ApiResponse<MemberDto>> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMember(id)));
    }

    @Operation(summary = "Criar membro")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'MEMBER_CREATE')")
    public ResponseEntity<ApiResponse<MemberDto>> createMember(@Valid @RequestBody CreateMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success(memberService.createMember(request)));
    }

    @Operation(summary = "Atualizar membro")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'MEMBER_UPDATE')")
    public ResponseEntity<ApiResponse<MemberDto>> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMember(id, request)));
    }

    @Operation(summary = "Excluir membro")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'MEMBER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
