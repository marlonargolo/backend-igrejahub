package com.igrejahub.support.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.support.dto.*;
import com.igrejahub.support.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/support/tickets")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportTicketDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(supportService.listTickets()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupportTicketDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(supportService.getTicket(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupportTicketDto>> create(
            @Valid @RequestBody CreateTicketRequest req) {
        return ResponseEntity.ok(ApiResponse.success(supportService.createTicket(req)));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<SupportMessageDto>> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(ApiResponse.success(supportService.sendMessage(id, req)));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<SupportTicketDto>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(supportService.closeTicket(id)));
    }
}