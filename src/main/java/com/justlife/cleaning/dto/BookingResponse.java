package com.justlife.cleaning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking response with booking details")
public class BookingResponse {
    
    @Schema(description = "Booking ID", example = "1")
    private Long id;
    
    @Schema(description = "Booking start date and time", example = "2023-11-23T10:00:00")
    private LocalDateTime startDateTime;
    
    @Schema(description = "Booking end date and time", example = "2023-11-23T12:00:00")
    private LocalDateTime endDateTime;
    
    @Schema(description = "Duration in hours", example = "2", allowableValues = {"2", "4"})
    private Integer durationHours;
    
    @Schema(description = "List of cleaner names assigned to this booking", example = "[\"John\", \"Jane\"]")
    private List<String> cleanerNames;
    
    @Schema(description = "Customer name", example = "John Doe")
    private String customerName;
}
