package com.justlife.cleaning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cleaner availability information")
public class CleanerAvailabilityDto {
    
    @Schema(description = "Cleaner ID", example = "1")
    private Long cleanerId;
    
    @Schema(description = "Cleaner name", example = "John Doe")
    private String name;
    
    @Schema(description = "Vehicle ID assigned to the cleaner", example = "1")
    private Long vehicleId;
    
    @Schema(description = "List of available time slots", example = "[\"08:00 (2h)\", \"08:00 (4h)\", \"08:30 (2h)\"]")
    private List<String> availableTimeSlots;
}
