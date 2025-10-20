package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.Onboarding;

import java.util.Optional;

@Repository
public interface OnboardingRepository extends JpaRepository<Onboarding, Long> {
    Optional<Onboarding> findByUserUserId(Long userId);

    @Modifying
    @Query(value = "CALL update_onboarding_step(:userId, :step)", nativeQuery = true)
    void updateOnboardingStep(@Param("userId") Long userId, @Param("step") int step);
}
