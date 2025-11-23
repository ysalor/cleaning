package com.justlife.cleaning.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = {
    @Index(name = "idx_cleaner_vehicle_id", columnList = "vehicle_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cleaner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToMany(mappedBy = "cleaners")
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

}
