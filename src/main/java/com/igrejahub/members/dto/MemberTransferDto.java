package com.igrejahub.members.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberTransferDto {
    private Long id;
    private Long memberId;
    private String memberName;
    private Long fromCongregationId;
    private String fromCongregationName;
    private Long toCongregationId;
    private String toCongregationName;
    private String reason;
    private LocalDateTime transferredAt;
}
