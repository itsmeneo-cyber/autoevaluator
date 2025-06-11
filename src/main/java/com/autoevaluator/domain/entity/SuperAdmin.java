package com.autoevaluator.domain.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class SuperAdmin extends AppUser {
    // Add specific fields if needed later
    private String name;
}
