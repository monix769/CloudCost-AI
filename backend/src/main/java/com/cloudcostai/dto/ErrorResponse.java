package com.cloudcostai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized error payload returned upon HTTP exceptions or validation failures.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error payload")
public class ErrorResponse {

    @Schema(description = "Error timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error reason phrase", example = "Bad Request")
    private String error;

    @Schema(description = "Detailed human-readable error description")
    private String message;

    @Schema(description = "Request URI path where error occurred", example = "/api/status")
    private String path;

    @Schema(description = "Field-level validation error map, if applicable")
    private Map<String, String> validationErrors;
}
