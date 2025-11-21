package com.justlife.cleaning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleanerAvailabilityDto {
    private Long cleanerId;
    private String name;
    private Long vehicleId;
    private List<String> availableTimeSlots;
}
