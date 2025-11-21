package com.justlife.cleaning.dto;

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
public class BookingRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "Duration is required")
    private Integer duration;

    @Min(value = 1, message = "Minimum 1 cleaner required")
    @Max(value = 3, message = "Maximum 3 cleaners allowed")
    private int cleanerCount;

    @NotNull(message = "Customer name is required")
    private String customerName;

    private String customerPhone;
}
