package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.Profession;

import java.util.Optional;

@Repository
public interface ProfessionRepository extends JpaRepository<Profession, String> {
    Optional<Profession> findByName(String name);
    boolean existsByName(String name);
}

