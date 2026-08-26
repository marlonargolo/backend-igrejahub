package com.igrejahub.members.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberOccurrenceDto {
    private Long id;
    private Long memberId;
    private LocalDate occurrenceDate;
    private String description;
    private LocalDateTime createdAt;
}
