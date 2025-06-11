package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.dto.DepartmentDTOGPT;
import com.autoevaluator.domain.entity.College;
import com.autoevaluator.domain.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByNameIn(List<String> departmentNames);
    Optional<Department> findByName(String name);
    Optional<Department> findByNameAndCollege(String name, College college);
    @Query("SELECT new com.autoevaluator.domain.dto.DepartmentDTOGPT(d.name, COUNT(c), COUNT(t)) " +
            "FROM Department d " +
            "LEFT JOIN d.courses c " +
            "LEFT JOIN d.teachers t " +
            "WHERE d.college.name = :collegeName " +
            "GROUP BY d.name")
    List<DepartmentDTOGPT> findDepartmentsByCollegeName(@Param("collegeName") String collegeName);

}

