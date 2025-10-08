package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.AdressInfo;

import java.util.Optional;

@Repository
public interface AdressInfoRepository extends JpaRepository<AdressInfo, Long> {

    Optional<AdressInfo> findByUserId(Long userId);
}
