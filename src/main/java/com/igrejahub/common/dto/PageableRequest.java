package com.igrejahub.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageableRequest {

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    private String sortBy;

    @Builder.Default
    private String sortDirection = "ASC";

    public Pageable toPageable() {
        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(page, size);
        }
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}