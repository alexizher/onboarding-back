package tech.nocountry.onboarding.modules.catalogs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.dto.ApiResponse;
import tech.nocountry.onboarding.modules.catalogs.dto.*;
import tech.nocountry.onboarding.modules.catalogs.service.CatalogService;

import java.util.List;

@RestController
@RequestMapping("/api/catalogs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CatalogController {

    private final CatalogService catalogService;

    /*
    * Business Categories
    */

    @GetMapping("/business-categories")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BusinessCategoryResponse>>> getAllBusinessCategories() {
        try {
            List<BusinessCategoryResponse> categories = catalogService.getAllBusinessCategories();
            return ResponseEntity.ok(
                ApiResponse.<List<BusinessCategoryResponse>>builder()
                    .success(true)
                    .message("Categorías de negocio obtenidas exitosamente")
                    .data(categories)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting business categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<BusinessCategoryResponse>>builder()
                    .success(false)
                    .message("Error al obtener categorías: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/business-categories/{categoryId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BusinessCategoryResponse>> getBusinessCategoryById(
            @PathVariable String categoryId) {
        try {
            BusinessCategoryResponse category = catalogService.getBusinessCategoryById(categoryId);
            return ResponseEntity.ok(
                ApiResponse.<BusinessCategoryResponse>builder()
                    .success(true)
                    .message("Categoría de negocio obtenida exitosamente")
                    .data(category)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<BusinessCategoryResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting business category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<BusinessCategoryResponse>builder()
                    .success(false)
                    .message("Error al obtener categoría: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/business-categories")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BusinessCategoryResponse>> createBusinessCategory(
            @Valid @RequestBody BusinessCategoryRequest request) {
        try {
            BusinessCategoryResponse category = catalogService.createBusinessCategory(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BusinessCategoryResponse>builder()
                    .success(true)
                    .message("Categoría de negocio creada exitosamente")
                    .data(category)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<BusinessCategoryResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error creating business category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<BusinessCategoryResponse>builder()
                    .success(false)
                    .message("Error al crear categoría: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/business-categories/{categoryId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BusinessCategoryResponse>> updateBusinessCategory(
            @PathVariable String categoryId,
            @Valid @RequestBody BusinessCategoryRequest request) {
        try {
            BusinessCategoryResponse category = catalogService.updateBusinessCategory(categoryId, request);
            return ResponseEntity.ok(
                ApiResponse.<BusinessCategoryResponse>builder()
                    .success(true)
                    .message("Categoría de negocio actualizada exitosamente")
                    .data(category)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<BusinessCategoryResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error updating business category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<BusinessCategoryResponse>builder()
                    .success(false)
                    .message("Error al actualizar categoría: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/business-categories/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBusinessCategory(@PathVariable String categoryId) {
        try {
            catalogService.deleteBusinessCategory(categoryId);
            return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                    .success(true)
                    .message("Categoría de negocio eliminada exitosamente")
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error deleting business category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error al eliminar categoría: " + e.getMessage())
                    .build());
        }
    }

    /*
    * Document Types
    */

    @GetMapping("/document-types")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentTypeResponse>>> getAllDocumentTypes() {
        try {
            List<DocumentTypeResponse> documentTypes = catalogService.getAllDocumentTypes();
            return ResponseEntity.ok(
                ApiResponse.<List<DocumentTypeResponse>>builder()
                    .success(true)
                    .message("Tipos de documento obtenidos exitosamente")
                    .data(documentTypes)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting document types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<DocumentTypeResponse>>builder()
                    .success(false)
                    .message("Error al obtener tipos de documento: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/document-types/{documentTypeId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentTypeResponse>> getDocumentTypeById(
            @PathVariable String documentTypeId) {
        try {
            DocumentTypeResponse documentType = catalogService.getDocumentTypeById(documentTypeId);
            return ResponseEntity.ok(
                ApiResponse.<DocumentTypeResponse>builder()
                    .success(true)
                    .message("Tipo de documento obtenido exitosamente")
                    .data(documentType)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<DocumentTypeResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting document type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<DocumentTypeResponse>builder()
                    .success(false)
                    .message("Error al obtener tipo de documento: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/document-types")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentTypeResponse>> createDocumentType(
            @Valid @RequestBody DocumentTypeRequest request) {
        try {
            DocumentTypeResponse documentType = catalogService.createDocumentType(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<DocumentTypeResponse>builder()
                    .success(true)
                    .message("Tipo de documento creado exitosamente")
                    .data(documentType)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<DocumentTypeResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error creating document type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<DocumentTypeResponse>builder()
                    .success(false)
                    .message("Error al crear tipo de documento: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/document-types/{documentTypeId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentTypeResponse>> updateDocumentType(
            @PathVariable String documentTypeId,
            @Valid @RequestBody DocumentTypeRequest request) {
        try {
            DocumentTypeResponse documentType = catalogService.updateDocumentType(documentTypeId, request);
            return ResponseEntity.ok(
                ApiResponse.<DocumentTypeResponse>builder()
                    .success(true)
                    .message("Tipo de documento actualizado exitosamente")
                    .data(documentType)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<DocumentTypeResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error updating document type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<DocumentTypeResponse>builder()
                    .success(false)
                    .message("Error al actualizar tipo de documento: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/document-types/{documentTypeId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentType(@PathVariable String documentTypeId) {
        try {
            catalogService.deleteDocumentType(documentTypeId);
            return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                    .success(true)
                    .message("Tipo de documento eliminado exitosamente")
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error deleting document type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error al eliminar tipo de documento: " + e.getMessage())
                    .build());
        }
    }

    /*
    * Professions
    */

    @GetMapping("/professions")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ProfessionResponse>>> getAllProfessions() {
        try {
            List<ProfessionResponse> professions = catalogService.getAllProfessions();
            return ResponseEntity.ok(
                ApiResponse.<List<ProfessionResponse>>builder()
                    .success(true)
                    .message("Profesiones obtenidas exitosamente")
                    .data(professions)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting professions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<ProfessionResponse>>builder()
                    .success(false)
                    .message("Error al obtener profesiones: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/professions/{professionId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProfessionResponse>> getProfessionById(
            @PathVariable String professionId) {
        try {
            ProfessionResponse profession = catalogService.getProfessionById(professionId);
            return ResponseEntity.ok(
                ApiResponse.<ProfessionResponse>builder()
                    .success(true)
                    .message("Profesión obtenida exitosamente")
                    .data(profession)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<ProfessionResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting profession", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ProfessionResponse>builder()
                    .success(false)
                    .message("Error al obtener profesión: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/professions")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProfessionResponse>> createProfession(
            @Valid @RequestBody ProfessionRequest request) {
        try {
            ProfessionResponse profession = catalogService.createProfession(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ProfessionResponse>builder()
                    .success(true)
                    .message("Profesión creada exitosamente")
                    .data(profession)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<ProfessionResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error creating profession", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ProfessionResponse>builder()
                    .success(false)
                    .message("Error al crear profesión: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/professions/{professionId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProfessionResponse>> updateProfession(
            @PathVariable String professionId,
            @Valid @RequestBody ProfessionRequest request) {
        try {
            ProfessionResponse profession = catalogService.updateProfession(professionId, request);
            return ResponseEntity.ok(
                ApiResponse.<ProfessionResponse>builder()
                    .success(true)
                    .message("Profesión actualizada exitosamente")
                    .data(profession)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<ProfessionResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error updating profession", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ProfessionResponse>builder()
                    .success(false)
                    .message("Error al actualizar profesión: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/professions/{professionId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProfession(@PathVariable String professionId) {
        try {
            catalogService.deleteProfession(professionId);
            return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                    .success(true)
                    .message("Profesión eliminada exitosamente")
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error deleting profession", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error al eliminar profesión: " + e.getMessage())
                    .build());
        }
    }

    /*
    * Credit Destinations
    */

    @GetMapping("/credit-destinations")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CreditDestinationResponse>>> getAllCreditDestinations() {
        try {
            List<CreditDestinationResponse> destinations = catalogService.getAllCreditDestinations();
            return ResponseEntity.ok(
                ApiResponse.<List<CreditDestinationResponse>>builder()
                    .success(true)
                    .message("Destinos de crédito obtenidos exitosamente")
                    .data(destinations)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting credit destinations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<CreditDestinationResponse>>builder()
                    .success(false)
                    .message("Error al obtener destinos de crédito: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/credit-destinations/{destinationId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CreditDestinationResponse>> getCreditDestinationById(
            @PathVariable String destinationId) {
        try {
            CreditDestinationResponse destination = catalogService.getCreditDestinationById(destinationId);
            return ResponseEntity.ok(
                ApiResponse.<CreditDestinationResponse>builder()
                    .success(true)
                    .message("Destino de crédito obtenido exitosamente")
                    .data(destination)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<CreditDestinationResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting credit destination", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<CreditDestinationResponse>builder()
                    .success(false)
                    .message("Error al obtener destino de crédito: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/credit-destinations")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CreditDestinationResponse>> createCreditDestination(
            @Valid @RequestBody CreditDestinationRequest request) {
        try {
            CreditDestinationResponse destination = catalogService.createCreditDestination(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CreditDestinationResponse>builder()
                    .success(true)
                    .message("Destino de crédito creado exitosamente")
                    .data(destination)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<CreditDestinationResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error creating credit destination", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<CreditDestinationResponse>builder()
                    .success(false)
                    .message("Error al crear destino de crédito: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/credit-destinations/{destinationId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CreditDestinationResponse>> updateCreditDestination(
            @PathVariable String destinationId,
            @Valid @RequestBody CreditDestinationRequest request) {
        try {
            CreditDestinationResponse destination = catalogService.updateCreditDestination(destinationId, request);
            return ResponseEntity.ok(
                ApiResponse.<CreditDestinationResponse>builder()
                    .success(true)
                    .message("Destino de crédito actualizado exitosamente")
                    .data(destination)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<CreditDestinationResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error updating credit destination", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<CreditDestinationResponse>builder()
                    .success(false)
                    .message("Error al actualizar destino de crédito: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/credit-destinations/{destinationId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCreditDestination(@PathVariable String destinationId) {
        try {
            catalogService.deleteCreditDestination(destinationId);
            return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                    .success(true)
                    .message("Destino de crédito eliminado exitosamente")
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error deleting credit destination", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error al eliminar destino de crédito: " + e.getMessage())
                    .build());
        }
    }

    /*
    * Departments
    */

    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAllDepartments() {
        try {
            List<DepartmentResponse> departments = catalogService.getAllDepartments();
            return ResponseEntity.ok(
                ApiResponse.<List<DepartmentResponse>>builder()
                    .success(true)
                    .message("Departamentos obtenidos exitosamente")
                    .data(departments)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting departments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<DepartmentResponse>>builder()
                    .success(false)
                    .message("Error al obtener departamentos: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/departments/{departmentId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(
            @PathVariable String departmentId) {
        try {
            DepartmentResponse department = catalogService.getDepartmentById(departmentId);
            return ResponseEntity.ok(
                ApiResponse.<DepartmentResponse>builder()
                    .success(true)
                    .message("Departamento obtenido exitosamente")
                    .data(department)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<DepartmentResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting department", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<DepartmentResponse>builder()
                    .success(false)
                    .message("Error al obtener departamento: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {
        try {
            DepartmentResponse department = catalogService.createDepartment(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<DepartmentResponse>builder()
                    .success(true)
                    .message("Departamento creado exitosamente")
                    .data(department)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<DepartmentResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error creating department", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<DepartmentResponse>builder()
                    .success(false)
                    .message("Error al crear departamento: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/departments/{departmentId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable String departmentId,
            @Valid @RequestBody DepartmentRequest request) {
        try {
            DepartmentResponse department = catalogService.updateDepartment(departmentId, request);
            return ResponseEntity.ok(
                ApiResponse.<DepartmentResponse>builder()
                    .success(true)
                    .message("Departamento actualizado exitosamente")
                    .data(department)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<DepartmentResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error updating department", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<DepartmentResponse>builder()
                    .success(false)
                    .message("Error al actualizar departamento: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/departments/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable String departmentId) {
        try {
            catalogService.deleteDepartment(departmentId);
            return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                    .success(true)
                    .message("Departamento eliminado exitosamente")
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error deleting department", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error al eliminar departamento: " + e.getMessage())
                    .build());
        }
    }

    /*
    * Cities
    */

    @GetMapping("/cities")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getAllCities() {
        try {
            List<CityResponse> cities = catalogService.getAllCities();
            return ResponseEntity.ok(
                ApiResponse.<List<CityResponse>>builder()
                    .success(true)
                    .message("Ciudades obtenidas exitosamente")
                    .data(cities)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting cities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<CityResponse>>builder()
                    .success(false)
                    .message("Error al obtener ciudades: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/departments/{departmentId}/cities")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCitiesByDepartment(
            @PathVariable String departmentId) {
        try {
            List<CityResponse> cities = catalogService.getCitiesByDepartment(departmentId);
            return ResponseEntity.ok(
                ApiResponse.<List<CityResponse>>builder()
                    .success(true)
                    .message("Ciudades del departamento obtenidas exitosamente")
                    .data(cities)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting cities by department", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<CityResponse>>builder()
                    .success(false)
                    .message("Error al obtener ciudades: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/cities/{cityId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CityResponse>> getCityById(
            @PathVariable String cityId) {
        try {
            CityResponse city = catalogService.getCityById(cityId);
            return ResponseEntity.ok(
                ApiResponse.<CityResponse>builder()
                    .success(true)
                    .message("Ciudad obtenida exitosamente")
                    .data(city)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<CityResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting city", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<CityResponse>builder()
                    .success(false)
                    .message("Error al obtener ciudad: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/cities")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CityResponse>> createCity(
            @Valid @RequestBody CityRequest request) {
        try {
            CityResponse city = catalogService.createCity(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CityResponse>builder()
                    .success(true)
                    .message("Ciudad creada exitosamente")
                    .data(city)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<CityResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error creating city", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<CityResponse>builder()
                    .success(false)
                    .message("Error al crear ciudad: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/cities/{cityId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CityResponse>> updateCity(
            @PathVariable String cityId,
            @Valid @RequestBody CityRequest request) {
        try {
            CityResponse city = catalogService.updateCity(cityId, request);
            return ResponseEntity.ok(
                ApiResponse.<CityResponse>builder()
                    .success(true)
                    .message("Ciudad actualizada exitosamente")
                    .data(city)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<CityResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error updating city", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<CityResponse>builder()
                    .success(false)
                    .message("Error al actualizar ciudad: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/cities/{cityId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCity(@PathVariable String cityId) {
        try {
            catalogService.deleteCity(cityId);
            return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                    .success(true)
                    .message("Ciudad eliminada exitosamente")
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error deleting city", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error al eliminar ciudad: " + e.getMessage())
                    .build());
        }
    }
}

