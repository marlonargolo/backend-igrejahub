package com.igrejahub.congregations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCongregationRequest {
    @NotNull(message = "Igreja é obrigatória")
    private Long churchId;
    @NotBlank(message = "Nome é obrigatório")
    private String name;
    private String city;
    private String state;
    private String address;
    private Long pastorId;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
}
