package com.igrejahub.common.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class PaginatedResponse<T> {
    private List<T> data;
    private PaginationMeta meta;

    @Getter
    @Builder
    public static class PaginationMeta {
        private int page;
        private int pageSize;
        private long total;
        private int totalPages;
    }
}
