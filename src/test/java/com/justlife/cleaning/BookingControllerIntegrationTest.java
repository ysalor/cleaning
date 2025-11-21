package com.justlife.cleaning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justlife.cleaning.dto.AvailabilityRequest;
import com.justlife.cleaning.dto.BookingRequest;
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
import java.time.LocalTime;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

        // Assuming cleaner setup from BeforeEach is cleaner 1 and 2
        mockMvc.perform(post("/api/bookings/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // Both cleaners should be free
                .andExpect(jsonPath("$[0].availableTimeSlots[0]").value("10:00 - 12:00"));
    }

}
