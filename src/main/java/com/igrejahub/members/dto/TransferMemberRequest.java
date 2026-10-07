package com.igrejahub.members.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferMemberRequest {
    @NotNull(message = "Congregação destino é obrigatória")
    private Long toCongregationId;

    private String reason;
}
