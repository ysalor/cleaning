package com.justlife.cleaning.repository;

import com.justlife.cleaning.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @Query("SELECT DISTINCT v FROM Vehicle v LEFT JOIN FETCH v.cleaners")
    List<Vehicle> findAllWithCleaners();
}
