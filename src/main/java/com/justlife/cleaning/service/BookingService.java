package com.justlife.cleaning.service;

import com.justlife.cleaning.dto.*;
import com.justlife.cleaning.entity.*;
import com.justlife.cleaning.exception.BusinessException;
import com.justlife.cleaning.exception.ResourceNotFoundException;
import com.justlife.cleaning.repository.BookingRepository;
import com.justlife.cleaning.repository.CleanerRepository;
import com.justlife.cleaning.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CleanerRepository cleanerRepository;
    private final VehicleRepository vehicleRepository;

    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(22, 0);
    private static final int BREAK_MINUTES = 30;

    @Transactional(readOnly = true)
    public List<CleanerAvailabilityDto> checkAvailability(AvailabilityRequest request) {
        validateRequest(request.getDate(), request.getStartTime(), request.getDuration());

        LocalDate date = request.getDate();
        
        if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return Collections.emptyList();
        }

        List<CleanerAvailabilityDto> availabilityList = new ArrayList<>();

        // Fetch only cleaner IDs to avoid loading full entities upfront
        List<Long> cleanerIds = cleanerRepository.findAllIds();

        // Fetch all bookings for all cleaners in a single query and group them
        Map<Long, List<Booking>> bookingsByCleaner = getBookingsByCleanerIds(cleanerIds, date);

        for (Long cleanerId : cleanerIds) {
            List<Booking> bookings = bookingsByCleaner.getOrDefault(cleanerId, Collections.emptyList());

            List<String> freeSlots;
            if (request.getStartTime() != null && request.getDuration() != null) {
                // Check specific time
                boolean isAvailable = isCleanerAvailable(bookings, date, request.getStartTime(), request.getDuration());
                freeSlots = isAvailable ? List.of(request.getStartTime() + " - " + request.getStartTime().plusHours(request.getDuration())) : Collections.emptyList();
            } else {
                freeSlots = calculateFreeSlots(bookings, date);
            }

            if (!freeSlots.isEmpty()) {
                Cleaner cleaner = cleanerRepository.getReferenceById(cleanerId);

                availabilityList.add(CleanerAvailabilityDto.builder()
                        .cleanerId(cleanerId)
                        .name(cleaner.getName())
                        .vehicleId(cleaner.getVehicle().getId())
                        .availableTimeSlots(freeSlots)
                        .build());
            }
        }

        return availabilityList;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        validateRequest(request.getDate(), request.getStartTime(), request.getDuration());

        LocalDateTime startDateTime = LocalDateTime.of(request.getDate(), request.getStartTime());
        LocalDateTime endDateTime = startDateTime.plusHours(request.getDuration());

        List<Vehicle> vehicles = vehicleRepository.findAllWithCleaners();
        Map<Long, List<Booking>> conflictsByCleaner = groupConflictsByCleaner(
                vehicles, startDateTime, endDateTime);

        List<Cleaner> selectedCleaners = selectAvailableCleaners(
                vehicles, conflictsByCleaner, request.getCleanerCount());

        if (selectedCleaners == null) {
            throw new BusinessException("No available cleaners found for the requested time and count constraint.");
        }

        Booking booking = Booking.builder()
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .durationHours(request.getDuration())
                .customerName(request.getCustomerName())
                .cleaners(new ArrayList<>(selectedCleaners))
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        return mapToResponse(savedBooking);
    }

    @Transactional
    public BookingResponse updateBooking(Long id, BookingUpdateRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        validateRequest(request.getDate(), request.getStartTime(), booking.getDurationHours());

        LocalDateTime newStart = LocalDateTime.of(request.getDate(), request.getStartTime());
        LocalDateTime newEnd = newStart.plusHours(booking.getDurationHours());

        List<Long> cleanerIds = booking.getCleaners().stream().map(Cleaner::getId).toList();

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                cleanerIds,
                newStart.minusMinutes(BREAK_MINUTES),
                newEnd.plusMinutes(BREAK_MINUTES)
        );

        boolean hasConflict = conflicts.stream().anyMatch(b -> !b.getId().equals(id));

        if (hasConflict) {
            throw new BusinessException("Selected cleaners are not available at the new time.");
        }

        booking.setStartDateTime(newStart);
        booking.setEndDateTime(newEnd);

        return mapToResponse(bookingRepository.save(booking));
    }

    private void validateRequest(LocalDate date, LocalTime time, Integer duration) {
        if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            throw new BusinessException("We do not work on Fridays.");
        }

        if (time != null ) {
            if ( time.isBefore(WORK_START)) {
                throw new BusinessException("Cannot start before " + WORK_START);
            }

            LocalTime endTime = time.plusHours(duration);
            if (endTime.isAfter(WORK_END)) {
                throw new BusinessException("Must finish before " + WORK_END);
            }
        }

        if (duration !=null && !duration.equals(2) && !duration.equals(4)) {
            throw new BusinessException("Duration must be 2 or 4 hours.");
        }
    }


    private boolean isCleanerAvailable(List<Booking> bookings, LocalDate date, LocalTime requestedStart, int duration) {
        LocalDateTime reqStart = LocalDateTime.of(date, requestedStart);
        LocalDateTime reqEnd = reqStart.plusHours(duration);

        // Check limits
        if (requestedStart.isBefore(WORK_START) || reqEnd.toLocalTime().isAfter(WORK_END)) {
            return false;
        }

        for (Booking b : bookings) {
            // Add break to existing booking
            LocalDateTime existingStartWithBuffer = b.getStartDateTime().minusMinutes(BREAK_MINUTES);
            LocalDateTime existingEndWithBuffer = b.getEndDateTime().plusMinutes(BREAK_MINUTES);

            // Check overlap
            if (reqStart.isBefore(existingEndWithBuffer) && reqEnd.isAfter(existingStartWithBuffer)) {
                return false;
            }
        }
        return true;
    }

    private List<String> calculateFreeSlots(List<Booking> bookings, LocalDate date) {
        List<String> slots = new ArrayList<>();
        LocalTime current = WORK_START;
        
        bookings.sort(Comparator.comparing(Booking::getStartDateTime));

        while (current.plusHours(2).isBefore(WORK_END) || current.plusHours(2).equals(WORK_END)) {
            if (isCleanerAvailable(bookings, date, current, 2)) {
                slots.add(current + " (2h)");
            }
            if (current.plusHours(4).isBefore(WORK_END) || current.plusHours(4).equals(WORK_END)) {
                 if (isCleanerAvailable(bookings, date, current, 4)) {
                    slots.add(current + " (4h)");
                }
            }
            current = current.plusMinutes(30);
        }
        return slots;
    }

    public Map<Long, List<Booking>> getBookingsByCleanerIds(List<Long> cleanerIds, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingRepository.findActiveBookingsForCleaners(
                cleanerIds, start, end
        );

        Map<Long, List<Booking>> map = new HashMap<>();
        for (Booking booking : bookings) {
            booking.getCleaners().stream()
                    .map(Cleaner::getId)
                    .filter(cleanerIds::contains)
                    .forEach(id -> map
                            .computeIfAbsent(id, k -> new ArrayList<>())
                            .add(booking));
        }
        return map;
    }

    private Map<Long, List<Booking>> groupConflictsByCleaner(
            List<Vehicle> vehicles, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<Long> allCleanerIds = vehicles.stream()
                .flatMap(vehicle -> vehicle.getCleaners().stream())
                .map(Cleaner::getId)
                .toList();

        List<Booking> allConflicts = bookingRepository.findConflictingBookings(
                allCleanerIds,
                startDateTime.minusMinutes(BREAK_MINUTES), // Check break before
                endDateTime.plusMinutes(BREAK_MINUTES)       // Check break after
        );

        Map<Long, List<Booking>> conflictsByCleaner = new HashMap<>();
        for (Booking conflict : allConflicts) {
            for (Cleaner cleaner : conflict.getCleaners()) {
                conflictsByCleaner.computeIfAbsent(cleaner.getId(), k -> new ArrayList<>())
                        .add(conflict);
            }
        }
        return conflictsByCleaner;
    }

    private List<Cleaner> selectAvailableCleaners(
            List<Vehicle> vehicles, Map<Long, List<Booking>> conflictsByCleaner, int requiredCount) {
        for (Vehicle vehicle : vehicles) {
            List<Cleaner> availableCleanersInVehicle = new ArrayList<>();
            for (Cleaner cleaner : vehicle.getCleaners()) {
                List<Booking> conflicts = conflictsByCleaner.getOrDefault(cleaner.getId(), Collections.emptyList());

                if (conflicts.isEmpty()) {
                    availableCleanersInVehicle.add(cleaner);
                }
            }

            if (availableCleanersInVehicle.size() >= requiredCount) {
                return availableCleanersInVehicle.subList(0, requiredCount);
            }
        }
        return null;
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .startDateTime(booking.getStartDateTime())
                .endDateTime(booking.getEndDateTime())
                .durationHours(booking.getDurationHours())
                .cleanerNames(booking.getCleaners().stream().map(Cleaner::getName).toList())
                .customerName(booking.getCustomerName())
                .build();
    }
}
