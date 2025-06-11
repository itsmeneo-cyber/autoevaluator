package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.entity.College;
import com.autoevaluator.domain.entity.Department;
import com.autoevaluator.domain.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

//    @Query("SELECT t FROM Teacher t JOIN AppUser a ON t.appUser.id = a.id WHERE a.username = :username")
//    Optional<Teacher> findByUsername(@Param("username") String username);
@Query("SELECT t FROM Teacher t WHERE t.username = :username")
Optional<Teacher> findByUsername(@Param("username") String username);

    @Query("SELECT t FROM Teacher t " +
            "LEFT JOIN FETCH t.department " +
            "LEFT JOIN FETCH t.courses " +
            "WHERE t.username = :username")
    Optional<Teacher> findByUsernameWithAssignments(@Param("username") String username);


//    List<Teacher> findByCollegeAndDepartment(College college, Department department);


    @Query("SELECT DISTINCT t FROM Teacher t " +
            "LEFT JOIN FETCH t.courses " +
            "WHERE t.college = :college AND t.department = :department")
    List<Teacher> findByCollegeAndDepartment(
            @Param("college") College college,
            @Param("department") Department department);

    Optional<Teacher> findByRegistrationId(String registrationId);


    List<Teacher> findTop10ByUsernameContainingIgnoreCase(String email);
}

