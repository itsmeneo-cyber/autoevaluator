
package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.entity.AppUser;
import com.autoevaluator.domain.entity.Course;
import com.autoevaluator.domain.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    AppUser findByUsername(String username);

}
