package com.justlife.cleaning;

import com.justlife.cleaning.dto.*;
import com.justlife.cleaning.entity.Booking;
import com.justlife.cleaning.entity.Cleaner;
import com.justlife.cleaning.entity.Vehicle;
import com.justlife.cleaning.exception.BusinessException;
import com.justlife.cleaning.exception.ResourceNotFoundException;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

        assertThrows(BusinessException.class, () -> bookingService.checkAvailability(request));
    }

    @Test
    void checkAvailability_ShouldReturnAvailableCleaners() {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .date(LocalDate.of(2023, 11, 23)) // A Thursday
                .startTime(LocalTime.of(10, 0))
                .duration(2)
                .build();

        when(cleanerRepository.findAllIds()).thenReturn(List.of(cleaner.getId()));
        when(bookingRepository.findActiveBookingsForCleaners(anyList(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(cleanerRepository.getReferenceById(cleaner.getId())).thenReturn(cleaner);

        List<CleanerAvailabilityDto> result = bookingService.checkAvailability(request);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(cleaner.getId(), result.get(0).getCleanerId());
    }

    @Test
    void checkAvailability_ShouldReturnEmpty_WhenSlotConflictsWithExistingBooking() {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .date(LocalDate.of(2023, 11, 23))
                .startTime(LocalTime.of(10, 0))
                .duration(2)
                .build();

        Booking existing = Booking.builder()
                .id(1L)
                .startDateTime(LocalDateTime.of(2023, 11, 23, 9, 30))
                .endDateTime(LocalDateTime.of(2023, 11, 23, 11, 30))
                .cleaners(List.of(cleaner))
                .customerName("Conflict test")
                .build();

        when(cleanerRepository.findAllIds()).thenReturn(List.of(cleaner.getId()));
        when(bookingRepository.findActiveBookingsForCleaners(anyList(), any(), any()))
                .thenReturn(List.of(existing));

        List<CleanerAvailabilityDto> result = bookingService.checkAvailability(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void checkAvailability_ShouldListFreeSlots_WhenNoSpecificTimeProvided() {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .date(LocalDate.of(2023, 11, 23))
                .build();

        Booking existing = Booking.builder()
                .id(1L)
                .startDateTime(LocalDateTime.of(2023, 11, 23, 10, 0))
                .endDateTime(LocalDateTime.of(2023, 11, 23, 12, 0))
                .build();

        when(cleanerRepository.findAllIds()).thenReturn(List.of(cleaner.getId()));
        when(bookingRepository.findActiveBookingsForCleaners(anyList(), any(), any()))
                .thenReturn(new ArrayList<>(List.of(existing)));
        when(cleanerRepository.getReferenceById(cleaner.getId())).thenReturn(cleaner);

        List<CleanerAvailabilityDto> result = bookingService.checkAvailability(request);

        assertEquals(1, result.size());
        CleanerAvailabilityDto dto = result.get(0);
        assertEquals(cleaner.getId(), dto.getCleanerId());
        assertFalse(dto.getAvailableTimeSlots().isEmpty());
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
    void createBooking_ShouldThrowException_WhenDurationIsInvalid() {
        BookingRequest request = BookingRequest.builder()
                .date(LocalDate.of(2023, 11, 23))
                .startTime(LocalTime.of(10, 0))
                .duration(3)
                .cleanerCount(1)
                .build();

        assertThrows(BusinessException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void createBooking_ShouldThrowException_WhenDateIsFriday() {
        BookingRequest request = BookingRequest.builder()
                .date(LocalDate.of(2023, 11, 24)) // Friday
                .startTime(LocalTime.of(10, 0))
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

        when(vehicleRepository.findAllWithCleaners()).thenReturn(List.of(vehicle));
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

    @Test
    void createBooking_ShouldThrowException_WhenNoAvailableCleanersInAnyVehicle() {
        BookingRequest request = BookingRequest.builder()
                .date(LocalDate.of(2023, 11, 23))
                .startTime(LocalTime.of(10, 0))
                .duration(2)
                .cleanerCount(1)
                .customerName("No Cleaner Customer")
                .build();

        Vehicle v = Vehicle.builder()
                .id(2L)
                .cleaners(List.of(cleaner))
                .build();

        when(vehicleRepository.findAllWithCleaners()).thenReturn(List.of(v));
        // Always conflict so that cleaner is never available
        // The conflict must include the cleaner in its cleaners list for the grouping logic to work
        Booking conflictingBooking = Booking.builder()
                .id(99L)
                .cleaners(List.of(cleaner))
                .build();
        when(bookingRepository.findConflictingBookings(any(), any(), any()))
                .thenReturn(List.of(conflictingBooking));

        assertThrows(BusinessException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void updateBooking_ShouldUpdateTimes_WhenNoConflicts() {
        Long bookingId = 1L;
        LocalDate newDate = LocalDate.of(2023, 11, 23);
        LocalTime newTime = LocalTime.of(12, 0);

        Booking existing = Booking.builder()
                .id(bookingId)
                .startDateTime(LocalDateTime.of(2023, 11, 23, 10, 0))
                .endDateTime(LocalDateTime.of(2023, 11, 23, 12, 0))
                .durationHours(2)
                .cleaners(List.of(cleaner))
                .customerName("Update Test")
                .build();

        BookingUpdateRequest updateRequest = BookingUpdateRequest.builder()
                .date(newDate)
                .startTime(newTime)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(bookingRepository.findConflictingBookings(anyList(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.updateBooking(bookingId, updateRequest);

        assertEquals(LocalDateTime.of(newDate, newTime), response.getStartDateTime());
        assertEquals(LocalDateTime.of(newDate, newTime.plusHours(2)), response.getEndDateTime());
    }

    @Test
    void updateBooking_ShouldThrowException_WhenConflictingBookingExists() {
        Long bookingId = 1L;
        LocalDate newDate = LocalDate.of(2023, 11, 23);
        LocalTime newTime = LocalTime.of(12, 0);

        Booking existing = Booking.builder()
                .id(bookingId)
                .startDateTime(LocalDateTime.of(2023, 11, 23, 10, 0))
                .endDateTime(LocalDateTime.of(2023, 11, 23, 12, 0))
                .durationHours(2)
                .cleaners(List.of(cleaner))
                .customerName("Update Conflict Test")
                .build();

        Booking conflicting = Booking.builder()
                .id(2L)
                .startDateTime(LocalDateTime.of(2023, 11, 23, 11, 30))
                .endDateTime(LocalDateTime.of(2023, 11, 23, 13, 30))
                .build();

        BookingUpdateRequest updateRequest = BookingUpdateRequest.builder()
                .date(newDate)
                .startTime(newTime)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(bookingRepository.findConflictingBookings(anyList(), any(), any()))
                .thenReturn(List.of(conflicting));

        assertThrows(BusinessException.class, () -> bookingService.updateBooking(bookingId, updateRequest));
    }

    @Test
    void updateBooking_ShouldThrowResourceNotFound_WhenBookingDoesNotExist() {
        Long nonExistingId = 999L;
        BookingUpdateRequest updateRequest = BookingUpdateRequest.builder()
                .date(LocalDate.of(2023, 11, 23))
                .startTime(LocalTime.of(10, 0))
                .build();

        when(bookingRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.updateBooking(nonExistingId, updateRequest));
    }


}
