package com.justlife.cleaning.config;

import com.justlife.cleaning.entity.Cleaner;
import com.justlife.cleaning.entity.Vehicle;
import com.justlife.cleaning.repository.CleanerRepository;
import com.justlife.cleaning.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final VehicleRepository vehicleRepository;
    private final CleanerRepository cleanerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (vehicleRepository.count() > 0) {
            return;
        }

        for (int i = 1; i <= 5; i++) {
            Vehicle vehicle = Vehicle.builder()
                    .licencePlate("DXB-" + i * 1000)
                    .cleaners(new ArrayList<>())
                    .build();
            
            // We must save vehicle first if cascade doesn't handle the relationship assignment properly 
            // or add cleaners to vehicle and save vehicle (due to cascade ALL)
            
            for (int j = 1; j <= 5; j++) {
                Cleaner cleaner = Cleaner.builder()
                        .name("Cleaner " + i + "-" + j)
                        .vehicle(vehicle)
                        .build();
                vehicle.getCleaners().add(cleaner);
            }
            
            vehicleRepository.save(vehicle);
        }
    }
}
