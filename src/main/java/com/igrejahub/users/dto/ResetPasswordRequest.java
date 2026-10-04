package com.igrejahub.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    /** Opcional: quando omitido, o backend gera uma senha aleatória e a devolve. */
    private String newPassword;
}
