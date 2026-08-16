package com.igrejahub.accounting.controller;

import com.igrejahub.accounting.dto.CreateJournalEntryRequest;
import com.igrejahub.accounting.dto.JournalEntryDto;
import com.igrejahub.accounting.dto.UpdateJournalEntryRequest;
import com.igrejahub.accounting.service.JournalEntryService;
import com.igrejahub.common.dto.ApiResponse;
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
@RequestMapping("/accounting/journal-entries")
@Tag(name = "Journal Entries", description = "Lançamentos contábeis")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_VIEW')")
    public ResponseEntity<ApiResponse<Page<JournalEntryDto>>> getEntries(
            @PageableDefault(size = 20, sort = "entryDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(journalEntryService.getEntries(pageable, status)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_VIEW')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> getEntry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(journalEntryService.getEntry(id)));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_MANAGE')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> createEntry(@Valid @RequestBody CreateJournalEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(journalEntryService.createEntry(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_MANAGE')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> updateEntry(
            @PathVariable Long id, @Valid @RequestBody UpdateJournalEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(journalEntryService.updateEntry(id, request)));
    }

    @PatchMapping("/{id}/post")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_POST')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> postEntry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(journalEntryService.postEntry(id)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_POST')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> cancelEntry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(journalEntryService.cancelEntry(id)));
    }
}