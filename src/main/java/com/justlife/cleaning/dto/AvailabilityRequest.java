package com.justlife.cleaning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to check cleaner availability")
public class AvailabilityRequest {
    
    @Schema(description = "Date to check availability (cannot be Friday)", example = "2023-11-23", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;
    
    @Schema(description = "Specific start time to check (optional - if not provided, returns all available slots)", example = "10:00")
    private LocalTime startTime;
    
    @Schema(description = "Duration in hours (optional - if not provided, returns all available slots)", example = "2", allowableValues = {"2", "4"})
    private Integer duration;
}
