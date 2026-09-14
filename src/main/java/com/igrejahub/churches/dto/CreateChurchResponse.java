package com.igrejahub.churches.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateChurchResponse {
    private ChurchDto church;
    private AdminCredentials admin;
    private String message;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AdminCredentials {
        private String name;
        private String email;
        private String password; // texto puro — apenas na resposta de criação
    }
}