package com.igrejahub.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private Object meta;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    public static <T> ApiResponse<T> success(T data, Object meta) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .meta(meta)
            .build();
    }

    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
            .success(true)
            .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .build();
    }
}
