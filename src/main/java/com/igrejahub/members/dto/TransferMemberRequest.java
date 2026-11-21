package com.igrejahub.members.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferMemberRequest {
    /** NULL = transferir de volta para a sede (sem congregação). */
    private Long toCongregationId;

    private String reason;
}
