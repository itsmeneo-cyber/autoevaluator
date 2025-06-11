package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.entity.Admin;
import com.autoevaluator.domain.entity.College;
import com.autoevaluator.domain.entity.Student;
import com.autoevaluator.domain.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(@Param("username") String username);

    List<Admin> findByCollege(College college);

}

