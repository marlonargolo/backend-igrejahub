package com.igrejahub.congregations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCongregationRequest {
    private Long churchId;
    private String name;
    private String city;
    private String state;
    private String address;
    private Long pastorId;
    private String imageUrl;
    private String status;
    private Double latitude;
    private Double longitude;
}
