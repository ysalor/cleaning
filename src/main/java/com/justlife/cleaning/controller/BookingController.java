package com.justlife.cleaning.controller;

import com.justlife.cleaning.dto.AvailabilityRequest;
import com.justlife.cleaning.dto.CleanerAvailabilityDto;
import com.justlife.cleaning.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Booking management APIs")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/availability")
    @Operation(summary = "Check availability", description = "Get available cleaners for date/time")
    public List<CleanerAvailabilityDto> checkAvailability(@RequestBody AvailabilityRequest request) {
        return bookingService.checkAvailability(request);
    }

}
