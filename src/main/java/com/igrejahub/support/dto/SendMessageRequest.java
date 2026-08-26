package com.igrejahub.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SendMessageRequest {
    @NotBlank private String message;
}