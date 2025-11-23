package com.justlife.cleaning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = {
    @Index(name = "idx_booking_start_datetime", columnList = "startDateTime"),
    @Index(name = "idx_booking_end_datetime", columnList = "endDateTime"),
    @Index(name = "idx_booking_datetime_range", columnList = "startDateTime,endDateTime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private Integer durationHours;

    @ManyToMany
    @JoinTable(
            name = "booking_cleaner",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "cleaner_id"),
            indexes = {
                @Index(name = "idx_booking_cleaner_cleaner_id", columnList = "cleaner_id"),
                @Index(name = "idx_booking_cleaner_booking_id", columnList = "booking_id")
            }
    )
    @Builder.Default
    private List<Cleaner> cleaners = new ArrayList<>();


    @Column(nullable = false)
    private String customerName;

}
