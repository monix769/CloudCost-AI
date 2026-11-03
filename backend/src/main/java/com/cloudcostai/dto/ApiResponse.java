package com.cloudcostai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Universal API Response wrapper for consistent REST API payloads.
 *
 * @param <T> Payload data type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates whether the operation was successful", example = "true")
    private boolean success;

    @Schema(description = "Human-readable response message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Timestamp when the response was generated")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Schema(description = "Payload data object")
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Request processed successfully")
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
