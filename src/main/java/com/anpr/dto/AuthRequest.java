package com.anpr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    
    @NotBlank(message = "Username is required")
    @Schema(description = "Login username", example = "superadmin", defaultValue = "superadmin")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "Login password", example = "SuperAdmin@123", defaultValue = "SuperAdmin@123")
    private String password;
}
