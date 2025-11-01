package tech.nocountry.onboarding.modules.catalogs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.*;
import tech.nocountry.onboarding.modules.catalogs.dto.*;
import tech.nocountry.onboarding.repositories.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CatalogService {

    private final BusinessCategoryRepository businessCategoryRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final ProfessionRepository professionRepository;
    private final CreditDestinationRepository creditDestinationRepository;
    private final DepartmentRepository departmentRepository;
    private final CityRepository cityRepository;

    /*
    * Business Categories
    */

    @Transactional(readOnly = true)
    public List<BusinessCategoryResponse> getAllBusinessCategories() {
        return businessCategoryRepository.findAll().stream()
                .map(this::mapToBusinessCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BusinessCategoryResponse getBusinessCategoryById(String categoryId) {
        BusinessCategory category = businessCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría de negocio no encontrada"));
        return mapToBusinessCategoryResponse(category);
    }

    @Transactional
    public BusinessCategoryResponse createBusinessCategory(BusinessCategoryRequest request) {
        log.info("Creating business category: {}", request.getName());

        if (businessCategoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        }

        BusinessCategory category = BusinessCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .riskLevel(request.getRiskLevel())
                .build();

        BusinessCategory saved = businessCategoryRepository.save(category);
        log.info("Business category created: {}", saved.getCategoryId());
        return mapToBusinessCategoryResponse(saved);
    }

    @Transactional
    public BusinessCategoryResponse updateBusinessCategory(String categoryId, BusinessCategoryRequest request) {
        log.info("Updating business category: {}", categoryId);

        BusinessCategory category = businessCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría de negocio no encontrada"));

        // Validar nombre único si cambió
        if (!category.getName().equals(request.getName()) && 
            businessCategoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setRiskLevel(request.getRiskLevel());

        BusinessCategory saved = businessCategoryRepository.save(category);
        log.info("Business category updated: {}", saved.getCategoryId());
        return mapToBusinessCategoryResponse(saved);
    }

    @Transactional
    public void deleteBusinessCategory(String categoryId) {
        log.info("Deleting business category: {}", categoryId);

        BusinessCategory category = businessCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría de negocio no encontrada"));

        businessCategoryRepository.delete(category);
        log.info("Business category deleted: {}", categoryId);
    }

    /*
    * Document Types
    */

    @Transactional(readOnly = true)
    public List<DocumentTypeResponse> getAllDocumentTypes() {
        return documentTypeRepository.findAll().stream()
                .map(this::mapToDocumentTypeResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentTypeResponse getDocumentTypeById(String documentTypeId) {
        DocumentType documentType = documentTypeRepository.findById(documentTypeId)
                .orElseThrow(() -> new RuntimeException("Tipo de documento no encontrado"));
        return mapToDocumentTypeResponse(documentType);
    }

    @Transactional
    public DocumentTypeResponse createDocumentType(DocumentTypeRequest request) {
        log.info("Creating document type: {}", request.getName());

        if (documentTypeRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe un tipo de documento con ese nombre");
        }

        DocumentType documentType = DocumentType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isRequired(request.getIsRequired() != null ? request.getIsRequired() : false)
                .build();

        DocumentType saved = documentTypeRepository.save(documentType);
        log.info("Document type created: {}", saved.getDocumentTypeId());
        return mapToDocumentTypeResponse(saved);
    }

    @Transactional
    public DocumentTypeResponse updateDocumentType(String documentTypeId, DocumentTypeRequest request) {
        log.info("Updating document type: {}", documentTypeId);

        DocumentType documentType = documentTypeRepository.findById(documentTypeId)
                .orElseThrow(() -> new RuntimeException("Tipo de documento no encontrado"));

        if (!documentType.getName().equals(request.getName()) && 
            documentTypeRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe un tipo de documento con ese nombre");
        }

        documentType.setName(request.getName());
        documentType.setDescription(request.getDescription());
        if (request.getIsRequired() != null) {
            documentType.setIsRequired(request.getIsRequired());
        }

        DocumentType saved = documentTypeRepository.save(documentType);
        log.info("Document type updated: {}", saved.getDocumentTypeId());
        return mapToDocumentTypeResponse(saved);
    }

    @Transactional
    public void deleteDocumentType(String documentTypeId) {
        log.info("Deleting document type: {}", documentTypeId);

        DocumentType documentType = documentTypeRepository.findById(documentTypeId)
                .orElseThrow(() -> new RuntimeException("Tipo de documento no encontrado"));

        documentTypeRepository.delete(documentType);
        log.info("Document type deleted: {}", documentTypeId);
    }

    /*
    * Professions
    */

    @Transactional(readOnly = true)
    public List<ProfessionResponse> getAllProfessions() {
        return professionRepository.findAll().stream()
                .map(this::mapToProfessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProfessionResponse getProfessionById(String professionId) {
        Profession profession = professionRepository.findById(professionId)
                .orElseThrow(() -> new RuntimeException("Profesión no encontrada"));
        return mapToProfessionResponse(profession);
    }

    @Transactional
    public ProfessionResponse createProfession(ProfessionRequest request) {
        log.info("Creating profession: {}", request.getName());

        if (professionRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe una profesión con ese nombre");
        }

        Profession profession = Profession.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Profession saved = professionRepository.save(profession);
        log.info("Profession created: {}", saved.getProfessionId());
        return mapToProfessionResponse(saved);
    }

    @Transactional
    public ProfessionResponse updateProfession(String professionId, ProfessionRequest request) {
        log.info("Updating profession: {}", professionId);

        Profession profession = professionRepository.findById(professionId)
                .orElseThrow(() -> new RuntimeException("Profesión no encontrada"));

        if (!profession.getName().equals(request.getName()) && 
            professionRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe una profesión con ese nombre");
        }

        profession.setName(request.getName());
        profession.setDescription(request.getDescription());

        Profession saved = professionRepository.save(profession);
        log.info("Profession updated: {}", saved.getProfessionId());
        return mapToProfessionResponse(saved);
    }

    @Transactional
    public void deleteProfession(String professionId) {
        log.info("Deleting profession: {}", professionId);

        Profession profession = professionRepository.findById(professionId)
                .orElseThrow(() -> new RuntimeException("Profesión no encontrada"));

        professionRepository.delete(profession);
        log.info("Profession deleted: {}", professionId);
    }

    /*
    * Credit Destinations
    */

    @Transactional(readOnly = true)
    public List<CreditDestinationResponse> getAllCreditDestinations() {
        return creditDestinationRepository.findAll().stream()
                .map(this::mapToCreditDestinationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CreditDestinationResponse getCreditDestinationById(String destinationId) {
        CreditDestination destination = creditDestinationRepository.findById(destinationId)
                .orElseThrow(() -> new RuntimeException("Destino de crédito no encontrado"));
        return mapToCreditDestinationResponse(destination);
    }

    @Transactional
    public CreditDestinationResponse createCreditDestination(CreditDestinationRequest request) {
        log.info("Creating credit destination: {}", request.getName());

        if (creditDestinationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe un destino de crédito con ese nombre");
        }

        CreditDestination destination = CreditDestination.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        CreditDestination saved = creditDestinationRepository.save(destination);
        log.info("Credit destination created: {}", saved.getDestinationId());
        return mapToCreditDestinationResponse(saved);
    }

    @Transactional
    public CreditDestinationResponse updateCreditDestination(String destinationId, CreditDestinationRequest request) {
        log.info("Updating credit destination: {}", destinationId);

        CreditDestination destination = creditDestinationRepository.findById(destinationId)
                .orElseThrow(() -> new RuntimeException("Destino de crédito no encontrado"));

        if (!destination.getName().equals(request.getName()) && 
            creditDestinationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe un destino de crédito con ese nombre");
        }

        destination.setName(request.getName());
        destination.setDescription(request.getDescription());

        CreditDestination saved = creditDestinationRepository.save(destination);
        log.info("Credit destination updated: {}", saved.getDestinationId());
        return mapToCreditDestinationResponse(saved);
    }

    @Transactional
    public void deleteCreditDestination(String destinationId) {
        log.info("Deleting credit destination: {}", destinationId);

        CreditDestination destination = creditDestinationRepository.findById(destinationId)
                .orElseThrow(() -> new RuntimeException("Destino de crédito no encontrado"));

        creditDestinationRepository.delete(destination);
        log.info("Credit destination deleted: {}", destinationId);
    }

    /*
    * Mapping Methods
    */

    private BusinessCategoryResponse mapToBusinessCategoryResponse(BusinessCategory category) {
        return BusinessCategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .riskLevel(category.getRiskLevel())
                .build();
    }

    private DocumentTypeResponse mapToDocumentTypeResponse(DocumentType documentType) {
        return DocumentTypeResponse.builder()
                .documentTypeId(documentType.getDocumentTypeId())
                .name(documentType.getName())
                .description(documentType.getDescription())
                .isRequired(documentType.getIsRequired())
                .build();
    }

    private ProfessionResponse mapToProfessionResponse(Profession profession) {
        return ProfessionResponse.builder()
                .professionId(profession.getProfessionId())
                .name(profession.getName())
                .description(profession.getDescription())
                .build();
    }

    private CreditDestinationResponse mapToCreditDestinationResponse(CreditDestination destination) {
        return CreditDestinationResponse.builder()
                .destinationId(destination.getDestinationId())
                .name(destination.getName())
                .description(destination.getDescription())
                .build();
    }

    /*
    * Departments
    */

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDepartmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(String departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));
        return mapToDepartmentResponse(department);
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        log.info("Creating department: {}", request.getName());

        if (departmentRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe un departamento con ese nombre");
        }

        Department department = Department.builder()
                .name(request.getName())
                .code(request.getCode())
                .build();

        Department saved = departmentRepository.save(department);
        log.info("Department created: {}", saved.getDepartmentId());
        return mapToDepartmentResponse(saved);
    }

    @Transactional
    public DepartmentResponse updateDepartment(String departmentId, DepartmentRequest request) {
        log.info("Updating department: {}", departmentId);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        if (!department.getName().equals(request.getName()) && 
            departmentRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe un departamento con ese nombre");
        }

        department.setName(request.getName());
        department.setCode(request.getCode());

        Department saved = departmentRepository.save(department);
        log.info("Department updated: {}", saved.getDepartmentId());
        return mapToDepartmentResponse(saved);
    }

    @Transactional
    public void deleteDepartment(String departmentId) {
        log.info("Deleting department: {}", departmentId);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        // Verificar que no tenga ciudades asociadas
        List<City> cities = cityRepository.findByDepartmentId(departmentId);
        if (!cities.isEmpty()) {
            throw new RuntimeException("No se puede eliminar el departamento porque tiene ciudades asociadas");
        }

        departmentRepository.delete(department);
        log.info("Department deleted: {}", departmentId);
    }

    /*
    * Cities
    */

    @Transactional(readOnly = true)
    public List<CityResponse> getAllCities() {
        return cityRepository.findAll().stream()
                .map(this::mapToCityResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CityResponse> getCitiesByDepartment(String departmentId) {
        return cityRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToCityResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CityResponse getCityById(String cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new RuntimeException("Ciudad no encontrada"));
        return mapToCityResponse(city);
    }

    @Transactional
    public CityResponse createCity(CityRequest request) {
        log.info("Creating city: {} for department: {}", request.getName(), request.getDepartmentId());

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        City city = City.builder()
                .department(department)
                .name(request.getName())
                .code(request.getCode())
                .build();

        City saved = cityRepository.save(city);
        log.info("City created: {}", saved.getCityId());
        return mapToCityResponse(saved);
    }

    @Transactional
    public CityResponse updateCity(String cityId, CityRequest request) {
        log.info("Updating city: {}", cityId);

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new RuntimeException("Ciudad no encontrada"));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        city.setDepartment(department);
        city.setName(request.getName());
        city.setCode(request.getCode());

        City saved = cityRepository.save(city);
        log.info("City updated: {}", saved.getCityId());
        return mapToCityResponse(saved);
    }

    @Transactional
    public void deleteCity(String cityId) {
        log.info("Deleting city: {}", cityId);

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new RuntimeException("Ciudad no encontrada"));

        cityRepository.delete(city);
        log.info("City deleted: {}", cityId);
    }

    /*
    * Mapping Methods
    */

    private DepartmentResponse mapToDepartmentResponse(Department department) {
        return DepartmentResponse.builder()
                .departmentId(department.getDepartmentId())
                .name(department.getName())
                .code(department.getCode())
                .build();
    }

    private CityResponse mapToCityResponse(City city) {
        String departmentId = null;
        String departmentName = null;
        try {
            if (city.getDepartment() != null) {
                departmentId = city.getDepartment().getDepartmentId();
                departmentName = city.getDepartment().getName();
            }
        } catch (Exception e) {
            log.warn("Error accessing department: {}", e.getMessage());
        }

        return CityResponse.builder()
                .cityId(city.getCityId())
                .departmentId(departmentId)
                .departmentName(departmentName)
                .name(city.getName())
                .code(city.getCode())
                .build();
    }
}

