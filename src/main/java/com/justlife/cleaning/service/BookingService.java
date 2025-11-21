package com.justlife.cleaning.service;

import com.justlife.cleaning.repository.BookingRepository;
import com.justlife.cleaning.repository.CleanerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingService {

    private BookingRepository bookingRepository;
    private CleanerRepository cleanerRepository;

}
