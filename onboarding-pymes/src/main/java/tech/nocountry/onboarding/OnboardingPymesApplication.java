package tech.nocountry.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("tech.nocountry.onboarding.entities")
@EnableJpaRepositories("tech.nocountry.onboarding.repositories")
public class OnboardingPymesApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnboardingPymesApplication.class, args);
    }
}