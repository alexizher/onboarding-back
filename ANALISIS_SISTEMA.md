# Análisis del Sistema - Crédito, Documentos y Autenticación

## Estado Actual de Implementación

### 1. Sistema de Aplicaciones de Crédito (CreditApplications)

#### **Funcionalidades Implementadas:**

**CRUD Completo:**
- [x] Crear solicitud de crédito (`POST /api/applications`)
- [x] Obtener solicitud por ID (`GET /api/applications/{id}`)
- [x] Actualizar solicitud (`PUT /api/applications/{id}`)
- [x] Eliminar solicitud (`DELETE /api/applications/{id}`)

**Consultas:**
- [x] Obtener mis solicitudes (`GET /api/applications/my-applications`)
- [x] Obtener solicitudes por estado (`GET /api/applications/status/{status}`)

**Seguridad:**
- [x] Autenticación basada en SecurityContext
- [x] Autorización por roles (APPLICANT, ANALYST, MANAGER, ADMIN)
- [x] Validación de que el usuario autenticado sea el propietario

**Campos de la aplicación:**
- [x] Información de empresa (nombre, CUIT, dirección)
- [x] Información financiera (monto, ingresos, gastos, deudas)
- [x] Términos del crédito (plazo en meses)
- [x] Relaciones con categorías, profesiones, destino, ubicación
- [x] Asignación a analista

#### **Lo que falta o necesita mejoras:**

1. **Validaciones de negocio:**
   - [ ] Validar que el monto solicitado sea razonable según ingresos
   - [ ] Validar capacidad de pago (deuda-to-income ratio)
   - [ ] Validar documentos mínimos requeridos antes de aprobar
   - [ ] Calcular score crediticio

2. **Workflow de estados:**
   - [ ] Transiciones de estado validadas (pending → under_review → approved/rejected)
   - [ ] Notificaciones al cambiar de estado
   - [ ] Auditoría de cambios de estado

3. **Asignación:**
   - [ ] Auto-asignar a analista disponible
   - [ ] Historial de asignaciones
   - [ ] Balanceador de carga para analistas

4. **Validación de propiedad:**
   - [ ] Aplicantes solo pueden ver/modificar sus propias solicitudes
   - Implementado parcialmente (necesita refuerzo en endpoints)

---

### 2. Sistema de Documentos (Documents)

#### **Funcionalidades Implementadas:**

**Operaciones:**
- [x] Subir documento (`POST /api/documents/upload`)
- [x] Obtener documento por ID (`GET /api/documents/{id}`)
- [x] Obtener documentos de una aplicación (`GET /api/documents/application/{id}`)
- [x] Obtener mis documentos (`GET /api/documents/my-documents`)
- [x] Verificar documento (`PUT /api/documents/{id}/verify`)

**Características:**
- [x] Almacenamiento en sistema de archivos
- [x] Hash SHA-256 para integridad
- [x] Validación de existencia de aplicación
- [x] Manejo de archivos en formato Base64

#### **Lo que falta o necesita mejoras:**

1. **Seguridad:**
   - [ ] **FALTA CRÍTICO**: No hay `@PreAuthorize` en DocumentController
   - [ ] Validar que el usuario solo pueda subir documentos de sus propias aplicaciones
   - [ ] Solo analistas/admin pueden verificar documentos
   - [ ] Validar tipos de archivo permitidos (PDF, JPG, PNG)
   - [ ] Validar tamaño máximo de archivo

2. **Funcionalidades:**
   - [ ] Descargar documento (GET /api/documents/{id}/download)
   - [ ] Eliminar documento (DELETE /api/documents/{id})
   - [ ] Obtener documentos por tipo de documento
   - [ ] Obtener documentos pendientes de verificación
   - [ ] Bulk upload (múltiples documentos)

3. **Validaciones:**
   - [ ] Validar que el tipo de documento esté permitido para la aplicación
   - [ ] Detectar duplicados por hash
   - [ ] Validar formato del archivo (PDF válido, etc.)

4. **Almacenamiento:**
   - [ ] Usar almacenamiento en la nube (S3, Google Cloud Storage)
   - [ ] Compresión de imágenes
   - [ ] Backup automático

---

### 3. Integración con Autenticación

#### **Implementado correctamente:**
- [x] Extracción de userId desde SecurityContext
- [x] Uso de JWT para autenticación
- [x] Roles en la base de datos
- [x] Filtro JwtAuthenticationFilter

#### **Problemas identificados:**

1. **DocumentController:**
   - [ ] **CRÍTICO**: Falta anotaciones `@PreAuthorize` en todos los endpoints
   - El endpoint de verificación debe ser solo para ANALYST, MANAGER, ADMIN

2. **Validación de propiedad:**
   - No valida que un usuario solo pueda acceder a sus propios recursos
   - El método `getCurrentUserId()` no valida permisos adicionales
   - No hay validación en el servicio de que un usuario solo acceda a sus aplicaciones

3. **Roles necesarios:**
   - [ ] No hay distinción entre ver y modificar (solo lectura vs escritura)
   - [ ] No hay permisos específicos por campo (solo status, solo documentos, etc.)

---

## Problemas Críticos a Resolver

### **Prioridad ALTA:**

1. **DocumentController sin seguridad** - Todos los endpoints son públicos
2. **Falta validación de propiedad** - Un usuario podría acceder a aplicaciones de otros
3. **Falta descarga de documentos** - No se puede bajar el archivo original
4. **No hay límites de tamaño de archivo** - Podría consumir todo el disco

### **Prioridad MEDIA:**

1. Workflow de estados con validaciones
2. Notificaciones de cambio de estado
3. Filtrado por rol en listados
4. Auditoría de acciones

### **Prioridad BAJA:**

1. Auto-asignación de analistas
2. Compresión de archivos
3. Almacenamiento en la nube
4. Score crediticio automático

---

## Recomendaciones de Implementación

### 1. Seguridad en DocumentController

```java
@PostMapping("/upload")
@PreAuthorize("hasRole('APPLICANT')")
public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(...)

@GetMapping("/{documentId}")
@PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(...)

@GetMapping("/application/{applicationId}")
@PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByApplication(...)

@PutMapping("/{documentId}/verify")
@PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
public ResponseEntity<ApiResponse<DocumentResponse>> verifyDocument(...)
```

### 2. Validación de Propiedad

```java
// En ApplicationService
public ApplicationResponse getApplicationById(String applicationId, String userId, String userRole) {
    CreditApplication application = applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
    
    // Solo APPLICANT puede ver solo sus solicitudes
    if ("APPLICANT".equals(userRole) && !application.getUser().getUserId().equals(userId)) {
        throw new RuntimeException("No tiene permiso para ver esta solicitud");
    }
    
    return mapToResponse(application);
}
```

### 3. Validaciones de Archivo

```java
private void validateFile(DocumentUploadRequest request) {
    // Validar tipo MIME
    List<String> allowedMimeTypes = Arrays.asList("application/pdf", "image/jpeg", "image/png");
    if (!allowedMimeTypes.contains(request.getMimeType())) {
        throw new RuntimeException("Tipo de archivo no permitido");
    }
    
    // Validar tamaño (ej: máximo 10MB)
    int maxSize = 10 * 1024 * 1024; // 10MB
    byte[] fileBytes = Base64.getDecoder().decode(request.getFileContent());
    if (fileBytes.length > maxSize) {
        throw new RuntimeException("Archivo muy grande. Máximo 10MB");
    }
}
```

---

## Resumen

### **Funcional:**
- CRUD completo de aplicaciones
- Subida de documentos
- Autenticación básica
- Relaciones entre entidades

### **Necesita atención:**
- Seguridad en DocumentController
- Validación de propiedad de recursos
- Descarga de archivos
- Validaciones de negocio

### **No implementado:**
- Workflow completo de estados
- Notificaciones
- Asignación automática
- Auditoría detallada
