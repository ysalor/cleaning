package com.justlife.cleaning;

import com.justlife.cleaning.dto.AvailabilityRequest;
import com.justlife.cleaning.dto.BookingRequest;
import com.justlife.cleaning.dto.BookingResponse;
import com.justlife.cleaning.dto.CleanerAvailabilityDto;
import com.justlife.cleaning.entity.Booking;
import com.justlife.cleaning.entity.Cleaner;
import com.justlife.cleaning.entity.Vehicle;
import com.justlife.cleaning.exception.BusinessException;
import com.justlife.cleaning.repository.BookingRepository;
import com.justlife.cleaning.repository.CleanerRepository;
import com.justlife.cleaning.repository.VehicleRepository;
import com.justlife.cleaning.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CleanerRepository cleanerRepository;
    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private BookingService bookingService;

    private Cleaner cleaner;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicle = Vehicle.builder().id(1L).cleaners(new ArrayList<>()).build();
        cleaner = Cleaner.builder().id(1L).name("John").vehicle(vehicle).build();
        vehicle.getCleaners().add(cleaner);
    }

    @Test
    void checkAvailability_ShouldReturnEmptyList_OnFriday() {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .date(LocalDate.of(2023, 11, 24)) // A Friday
                .build();

        List<CleanerAvailabilityDto> result = bookingService.checkAvailability(request);

        assertTrue(result.isEmpty());
        verify(cleanerRepository, never()).findAll();
    }

    @Test
    void checkAvailability_ShouldReturnAvailableCleaners() {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .date(LocalDate.of(2023, 11, 23)) // A Thursday
                .startTime(LocalTime.of(10, 0))
                .duration(2)
                .build();

        when(cleanerRepository.findAll()).thenReturn(List.of(cleaner));
        when(bookingRepository.findActiveBookingsForCleaner(any(), any(), any())).thenReturn(Collections.emptyList());

        List<CleanerAvailabilityDto> result = bookingService.checkAvailability(request);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(cleaner.getId(), result.get(0).getCleanerId());
    }

    @Test
    void createBooking_ShouldThrowException_WhenStartsBefore8AM() {
        BookingRequest request = BookingRequest.builder()
                .date(LocalDate.of(2023, 11, 23))
                .startTime(LocalTime.of(7, 0))
                .duration(2)
                .cleanerCount(1)
                .build();

        assertThrows(BusinessException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void createBooking_ShouldThrowException_WhenEndsAfter10PM() {
        BookingRequest request = BookingRequest.builder()
                .date(LocalDate.of(2023, 11, 23))
                .startTime(LocalTime.of(21, 0)) // Ends at 23:00
                .duration(2)
                .cleanerCount(1)
                .build();

        assertThrows(BusinessException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void createBooking_ShouldSucceed_WhenCleanersAvailable() {
        BookingRequest request = BookingRequest.builder()
                .date(LocalDate.of(2023, 11, 23))
                .startTime(LocalTime.of(10, 0))
                .duration(2)
                .cleanerCount(1)
                .customerName("Test Customer")
                .build();

        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));
        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals(1, response.getCleanerNames().size());
        assertEquals("John", response.getCleanerNames().get(0));
    }
}
