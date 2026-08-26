package com.igrejahub.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateTicketRequest {
    @NotBlank private String title;
    @NotBlank private String category;
    private String priority;
    @NotBlank private String descricao;
}