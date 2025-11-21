package com.justlife.cleaning.repository;

import com.justlife.cleaning.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b " +
            "JOIN b.cleaners c " +
            "WHERE c.id = :cleanerId " +
            "AND b.startDateTime >= :start " +
            "AND b.endDateTime <= :end")
    List<Booking> findActiveBookingsForCleaner(@Param("cleanerId") Long cleanerId,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query("SELECT b FROM Booking b " +
            "JOIN b.cleaners c " +
            "WHERE c.id IN :cleanerIds " +
            "AND ((b.startDateTime < :end AND b.endDateTime > :start))")
    List<Booking> findConflictingBookings(@Param("cleanerIds") List<Long> cleanerIds,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}
