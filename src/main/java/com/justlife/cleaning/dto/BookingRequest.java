package com.justlife.cleaning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Schema(description = "Request to create a new booking")
public class BookingRequest {

    @NotNull(message = "Date is required")
    @Schema(description = "Booking date (cannot be Friday)", example = "2023-11-23", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    @NotNull(message = "Start time is required")
    @Schema(description = "Start time (between 08:00 and 22:00)", example = "10:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime startTime;

    @NotNull(message = "Duration is required")
    @Schema(description = "Duration in hours (must be 2 or 4)", example = "2", allowableValues = {"2", "4"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer duration;

    @Min(value = 1, message = "Minimum 1 cleaner required")
    @Max(value = 3, message = "Maximum 3 cleaners allowed")
    @Schema(description = "Number of cleaners required (1-3)", example = "2", minimum = "1", maximum = "3")
    private int cleanerCount;

    @NotNull(message = "Customer name is required")
    @Schema(description = "Customer name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerName;

    @Schema(description = "Customer phone number (optional)", example = "+1234567890")
    private String customerPhone;
}
