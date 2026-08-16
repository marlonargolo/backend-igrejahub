package com.igrejahub.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CancelTransactionRequest {
    @NotBlank(message = "Motivo do cancelamento é obrigatório")
    private String reason;
}