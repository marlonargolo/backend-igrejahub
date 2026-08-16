package com.igrejahub.members.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberFilterDto {
    private String search;
    private Long congregationId;
    private String status;
    private String role;
    private Integer page;
    private Integer pageSize;
    private String sort;
    private String order;
}
