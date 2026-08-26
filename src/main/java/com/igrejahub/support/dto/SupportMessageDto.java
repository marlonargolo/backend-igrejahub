package com.igrejahub.support.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupportMessageDto {
    private Long id;
    private String autor;
    private String texto;
    private String tipo;
    private LocalDateTime dataHora;
}