package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.ApplicationState;

import java.util.Optional;

@Repository
public interface ApplicationStateRepository extends JpaRepository<ApplicationState, String> {
    Optional<ApplicationState> findByName(String name);
}

