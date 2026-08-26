package com.igrejahub.members.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOccurrenceRequest {
    @NotNull(message = "Data é obrigatória")
    private LocalDate occurrenceDate;
    @NotBlank(message = "Descrição é obrigatória")
    private String description;
}