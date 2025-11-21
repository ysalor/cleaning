package com.justlife.cleaning.dto;

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
public class BookingResponse {
    private Long id;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Integer durationHours;
    private List<String> cleanerNames;
    private String customerName;
}
