package com.justlife.cleaning.repository;

import com.justlife.cleaning.entity.Cleaner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CleanerRepository extends JpaRepository<Cleaner, Long> {

    @Query("select c.id from Cleaner c")
    List<Long> findAllIds();

}
