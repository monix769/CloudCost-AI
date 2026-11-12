package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Count of unread notifications for badge rendering")
public class UnreadNotificationCountResponse {

    @Schema(description = "Number of unread notifications for the active organization", example = "3")
    private long unreadCount;
}

