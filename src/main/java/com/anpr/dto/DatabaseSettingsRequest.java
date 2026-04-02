package com.anpr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseSettingsRequest {

    @NotBlank(message = "Database host is required")
    private String host;

    @Min(value = 1, message = "Port must be greater than zero")
    @Max(value = 65535, message = "Port must be less than or equal to 65535")
    private int port = 3306;

    @NotBlank(message = "Database name is required")
    private String databaseName;

    @NotBlank(message = "Database username is required")
    private String username;

    @NotBlank(message = "Database password is required")
    private String password;

    private boolean useSsl;
}
