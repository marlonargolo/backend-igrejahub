package com.igrejahub.reports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {
    @NotBlank(message = "Nome é obrigatório")
    private String name;
    @NotNull(message = "Tipo é obrigatório")
    private String type;
    @NotNull(message = "Formato é obrigatório")
    private String format;
    private Map<String, Object> filters;
}
