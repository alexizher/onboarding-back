package tech.nocountry.onboarding.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tech.nocountry.onboarding.services.RoleService;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleService roleService;

    @Override
    public void run(String... args) throws Exception {
        // Crear roles por defecto al iniciar la aplicación
        roleService.createDefaultRoles();
        System.out.println("Roles por defecto creados exitosamente");
    }
}
