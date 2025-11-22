package com.justlife.cleaning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justlife.cleaning.dto.AvailabilityRequest;
import com.justlife.cleaning.dto.BookingRequest;
import com.justlife.cleaning.dto.BookingUpdateRequest;
import com.justlife.cleaning.entity.Booking;
import com.justlife.cleaning.entity.Cleaner;
import com.justlife.cleaning.entity.Vehicle;
import com.justlife.cleaning.repository.BookingRepository;
import com.justlife.cleaning.repository.CleanerRepository;
import com.justlife.cleaning.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CleanerRepository cleanerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        bookingRepository.deleteAll();
        cleanerRepository.deleteAll();
        vehicleRepository.deleteAll();

        Vehicle vehicle = Vehicle.builder()
                .licencePlate("TEST-001")
                .cleaners(new ArrayList<>())
                .build();
        

        vehicleRepository.save(vehicle);

        Cleaner cleaner1 = Cleaner.builder().name("Cleaner 1").vehicle(vehicle).build();
        Cleaner cleaner2 = Cleaner.builder().name("Cleaner 2").vehicle(vehicle).build();
        
        cleanerRepository.save(cleaner1);
        cleanerRepository.save(cleaner2);
        
        vehicle.getCleaners().add(cleaner1);
        vehicle.getCleaners().add(cleaner2);
        vehicleRepository.save(vehicle);
    }

    @Test
    void checkAvailability_ShouldReturnAvailableSlots_ForSpecificTime() throws Exception {
        LocalDate testDate = LocalDate.now().plusDays(1);
        if (testDate.getDayOfWeek() == DayOfWeek.FRIDAY) {
            testDate = testDate.plusDays(1);
        }

        AvailabilityRequest request = AvailabilityRequest.builder()
                .date(testDate)
                .startTime(LocalTime.of(10, 0))
                .duration(2)
                .build();

        mockMvc.perform(post("/api/bookings/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // Both cleaners should be free
                .andExpect(jsonPath("$[0].availableTimeSlots[0]").value("10:00 - 12:00"));
    }

    @Test
    void checkAvailability_ShouldReturnBadRequest_WhenDateIsFriday() throws Exception {
        LocalDate friday = nextFriday(LocalDate.now());

        AvailabilityRequest request = AvailabilityRequest.builder()
                .date(friday)
                .startTime(LocalTime.of(10, 0))
                .duration(2)
                .build();

        mockMvc.perform(post("/api/bookings/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBooking_ShouldCreateBooking_WhenValidRequestAndCleanersAvailable() throws Exception {
        LocalDate date = nextNonFriday(LocalDate.now().plusDays(1));

        BookingRequest request = BookingRequest.builder()
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .duration(2)
                .cleanerCount(2)
                .customerName("John Doe")
                .build();

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.durationHours").value(2))
                .andExpect(jsonPath("$.cleanerNames.length()").value(2));

        List<Booking> all = bookingRepository.findAll();
        assertThat(all).hasSize(1);
        Booking booking = all.get(0);
        assertThat(booking.getCustomerName()).isEqualTo("John Doe");
        assertThat(booking.getCleaners()).hasSize(2);
    }

    @Test
    void createBooking_ShouldReturnBadRequest_WhenNoCleanersAvailable() throws Exception {
        LocalDate date = nextNonFriday(LocalDate.now().plusDays(1));
        LocalTime time = LocalTime.of(10, 0);

        // Create a conflicting booking that occupies all cleaners
        List<Cleaner> allCleaners = cleanerRepository.findAll();
        Booking conflictingBooking = Booking.builder()
                .startDateTime(LocalDateTime.of(date, time))
                .endDateTime(LocalDateTime.of(date, time.plusHours(2)))
                .durationHours(2)
                .customerName("Conflict Holder")
                .cleaners(new ArrayList<>(allCleaners))
                .build();
        bookingRepository.save(conflictingBooking);

        BookingRequest request = BookingRequest.builder()
                .date(date)
                .startTime(time)
                .duration(2)
                .cleanerCount(1) // Even 1 cleaner is not available
                .customerName("Rejected Customer")
                .build();

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBooking_ShouldUpdateDateTime_WhenNoConflicts() throws Exception {
        LocalDate date = nextNonFriday(LocalDate.now().plusDays(1));

        BookingRequest request = BookingRequest.builder()
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .duration(2)
                .cleanerCount(1)
                .customerName("To Update")
                .build();

        String response = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Booking created = objectMapper.readValue(response, Booking.class);
        Long bookingId = created.getId();

        BookingUpdateRequest updateRequest = BookingUpdateRequest.builder()
                .date(date)
                .startTime(LocalTime.of(14, 0))
                .build();

        mockMvc.perform(put("/api/bookings/{id}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId));

        Booking updated = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(updated.getStartDateTime()).isEqualTo(LocalDateTime.of(date, LocalTime.of(14, 0, 0)));
    }

    @Test
    void updateBooking_ShouldReturnNotFound_WhenBookingDoesNotExist() throws Exception {
        BookingUpdateRequest request = BookingUpdateRequest.builder()
                .date(LocalDate.now())
                .startTime(LocalTime.of(12, 0))
                .build();

        mockMvc.perform(put("/api/bookings/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    private LocalDate nextNonFriday(LocalDate start) {
        LocalDate date = start;
        while (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private LocalDate nextFriday(LocalDate start) {
        LocalDate date = start;
        while (date.getDayOfWeek() != DayOfWeek.FRIDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

}
