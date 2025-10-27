package tech.nocountry.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("tech.nocountry.onboarding.config")
public class OnboardingPymesApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnboardingPymesApplication.class, args);
    }
}