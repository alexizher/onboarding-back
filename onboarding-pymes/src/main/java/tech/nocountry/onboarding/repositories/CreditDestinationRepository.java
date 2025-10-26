package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.CreditDestination;

import java.util.Optional;

@Repository
public interface CreditDestinationRepository extends JpaRepository<CreditDestination, String> {
    Optional<CreditDestination> findByName(String name);
}

