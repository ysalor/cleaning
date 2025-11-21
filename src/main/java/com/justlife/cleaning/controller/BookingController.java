package com.justlife.cleaning.controller;

import com.justlife.cleaning.dto.*;
import com.justlife.cleaning.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create Booking", description = "Create a new cleaning appointment")
    public BookingResponse createBooking(@Valid @RequestBody BookingRequest request) {
        return bookingService.createBooking(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Booking", description = "Update date/time of an existing booking")
    public BookingResponse updateBooking(@PathVariable Long id, @Valid @RequestBody BookingUpdateRequest request) {
        return bookingService.updateBooking(id, request);
    }

}
