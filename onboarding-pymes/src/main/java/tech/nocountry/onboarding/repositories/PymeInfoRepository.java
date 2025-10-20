package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.PymeInfo;

import java.util.Optional;

@Repository
public interface PymeInfoRepository extends JpaRepository<PymeInfo, Long> {

    Optional<PymeInfo> findByUserUserId(Long userId);
}
