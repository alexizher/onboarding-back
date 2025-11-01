package tech.nocountry.onboarding.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tech.nocountry.onboarding.entities.DocumentType;
import tech.nocountry.onboarding.entities.ApplicationState;
import tech.nocountry.onboarding.repositories.DocumentTypeRepository;
import tech.nocountry.onboarding.repositories.ApplicationStateRepository;
import tech.nocountry.onboarding.services.RoleService;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleService roleService;
    
    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    @Autowired
    private ApplicationStateRepository applicationStateRepository;

    @Override
    public void run(String... args) throws Exception {
        // Crear roles por defecto al iniciar la aplicación
        roleService.createDefaultRoles();
        System.out.println("Roles por defecto creados exitosamente");
        
        // Crear tipos de documentos por defecto
        createDefaultDocumentTypes();
        System.out.println("Tipos de documentos por defecto creados exitosamente");

        // Crear estados de aplicación por defecto (catálogo)
        createDefaultApplicationStates();
        System.out.println("Estados de aplicación por defecto creados exitosamente");
    }
    
    private void createDefaultDocumentTypes() {
        List<DocumentType> defaultTypes = Arrays.asList(
            DocumentType.builder()
                .name("DNI")
                .description("Documento Nacional de Identidad")
                .isRequired(true)
                .build(),
            DocumentType.builder()
                .name("Constancia de Inscripción")
                .description("Constancia de inscripción en AFIP")
                .isRequired(true)
                .build(),
            DocumentType.builder()
                .name("Constancia de Ingresos Brutos")
                .description("Constancia de ingresos brutos actualizada")
                .isRequired(true)
                .build(),
            DocumentType.builder()
                .name("Balance General")
                .description("Balance general de los últimos 12 meses")
                .isRequired(false)
                .build(),
            DocumentType.builder()
                .name("Estado de Cuenta Bancario")
                .description("Estado de cuenta bancario de los últimos 3 meses")
                .isRequired(true)
                .build(),
            DocumentType.builder()
                .name("Comprobante de Domicilio")
                .description("Factura de servicio público (últimos 3 meses)")
                .isRequired(true)
                .build()
        );

        for (DocumentType docType : defaultTypes) {
            if (!documentTypeRepository.findByName(docType.getName()).isPresent()) {
                documentTypeRepository.save(docType);
            }
        }
    }

    private void createDefaultApplicationStates() {
        List<ApplicationState> defaultStates = Arrays.asList(
            ApplicationState.builder().name("PENDING").description("Pendiente").stepOrder(1).build(),
            ApplicationState.builder().name("DRAFT").description("Borrador").stepOrder(2).build(),
            ApplicationState.builder().name("SUBMITTED").description("Enviada").stepOrder(2).build(),
            ApplicationState.builder().name("UNDER_REVIEW").description("En revisión").stepOrder(3).build(),
            ApplicationState.builder().name("DOCUMENTS_PENDING").description("Documentación pendiente").stepOrder(4).build(),
            ApplicationState.builder().name("APPROVED").description("Aprobada").stepOrder(5).build(),
            ApplicationState.builder().name("REJECTED").description("Rechazada").stepOrder(6).build(),
            ApplicationState.builder().name("CANCELLED").description("Cancelada").stepOrder(7).build()
        );

        for (ApplicationState state : defaultStates) {
            if (applicationStateRepository.findByName(state.getName()).isEmpty()) {
                applicationStateRepository.save(state);
            }
        }
    }
}
