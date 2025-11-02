package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.BusinessCategory;

import java.util.Optional;

@Repository
public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory, String> {
    Optional<BusinessCategory> findByName(String name);
    boolean existsByName(String name);
}

