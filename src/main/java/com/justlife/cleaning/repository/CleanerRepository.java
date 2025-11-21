package com.justlife.cleaning.repository;

import com.justlife.cleaning.entity.Cleaner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CleanerRepository extends JpaRepository<Cleaner, Long> {

}
