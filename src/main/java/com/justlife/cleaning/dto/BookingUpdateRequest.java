package com.justlife.cleaning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing booking")
public class BookingUpdateRequest {

    @NotNull(message = "Date is required")
    @Schema(description = "New booking date (cannot be Friday)", example = "2023-11-23", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    @NotNull(message = "Start time is required")
    @Schema(description = "New start time (between 08:00 and 22:00)", example = "14:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime startTime;
}
