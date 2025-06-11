package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollegeRepository extends JpaRepository<College, Long> {

    // Find a college by its name
    Optional<College> findByName(String name);

    // You can add more custom queries here as needed in the future
}
