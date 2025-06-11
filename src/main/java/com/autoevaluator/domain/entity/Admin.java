package com.autoevaluator.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor
@Data
@ToString
public class Admin extends AppUser  {

    private  String adminUniqueID;

    private String name;

    @ManyToOne
    @JoinColumn(name = "college_id")
    private College college;

    private boolean isPrimary;

}
