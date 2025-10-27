package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.City;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, String> {
    Optional<City> findByName(String name);
    
    @Query("SELECT c FROM City c WHERE c.department.departmentId = :departmentId")
    List<City> findByDepartmentId(@Param("departmentId") String departmentId);
}

