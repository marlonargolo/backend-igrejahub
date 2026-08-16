package com.igrejahub.finance.dto;

import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConfirmTransactionRequest {
    private LocalDate confirmedAt;
    private String notes;
}