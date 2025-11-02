# Sistema de Workflow de Estados para Aplicaciones de Crédito

## Resumen de Implementación

Se ha implementado un sistema completo de gestión de estados para las solicitudes de crédito que incluye validación de transiciones, registro de historial y control de permisos por rol.

### Archivos Creados

1. **ApplicationStatusHistory.java** - Entidad para registrar cambios de estado
2. **ApplicationStatusHistoryRepository.java** - Repositorio para historial
3. **StateWorkflowService.java** - Servicio de lógica de transiciones
4. **StatusChangeRequest.java** - DTO para cambio de estado
5. **StatusHistoryResponse.java** - DTO para respuestas de historial

### Archivos Modificados

1. **CreditApplicationController.java** - Agregados 3 nuevos endpoints

---

## Estados Definidos

El sistema usa el enum `ApplicationStatus` con los siguientes estados:

```java
enum ApplicationStatus {
    DRAFT("Borrador"),
    SUBMITTED("Enviada"),
    UNDER_REVIEW("En Revisión"),
    DOCUMENTS_PENDING("Documentos Pendientes"),
    APPROVED("Aprobada"),
    REJECTED("Rechazada"),
    CANCELLED("Cancelada");
}
```

---

## Diagrama de Transiciones Válidas

```
DRAFT
  ├─> SUBMITTED (Enviar)
  └─> CANCELLED (Cancelar)

SUBMITTED
  ├─> UNDER_REVIEW (Enviar a revisión)
  ├─> DOCUMENTS_PENDING (Marcar documentos pendientes)
  └─> CANCELLED (Cancelar)

UNDER_REVIEW
  ├─> DOCUMENTS_PENDING (Solicitar documentos)
  ├─> APPROVED (Aprobar)
  └─> REJECTED (Rechazar)

DOCUMENTS_PENDING
  ├─> UNDER_REVIEW (Volver a revisión)
  └─> REJECTED (Rechazar por falta de documentos)

APPROVED
  └─> CANCELLED (Cancelar aprobación)

REJECTED
  └─> (Estado final, no hay transiciones)

CANCELLED
  └─> (Estado final, no hay transiciones)
```

---

## Permisos por Rol

### ADMIN / MANAGER
Pueden cambiar a cualquier estado:
- DRAFT, SUBMITTED, UNDER_REVIEW, DOCUMENTS_PENDING, APPROVED, REJECTED, CANCELLED

### ANALYST
Pueden mover solicitudes a:
- UNDER_REVIEW
- DOCUMENTS_PENDING
- APPROVED
- REJECTED

**Validación adicional:** Solo pueden aprobar/rechazar si tienen la solicitud asignada

### APPLICANT
Pueden:
- SUBMITTED (enviar su solicitud)
- CANCELLED (cancelar su solicitud)

---

## Nuevos Endpoints

### 1. Cambiar Estado de una Solicitud

```http
PUT /api/applications/{applicationId}/status
Authorization: Bearer {token}
Content-Type: application/json

{
  "newStatus": "UNDER_REVIEW",
  "comments": "Documentos completos, enviando a revisión",
  "reason": "Todos los documentos verificados"
}
```

**Permisos:** Todos los roles autenticados  
**Validaciones:**
- [x] Verifica que la transición sea válida según el estado actual
- [x] Verifica que el rol tenga permiso para ese estado
- [x] Validación especial para analistas (debe tener asignación)
- [x] Registra en historial

### 2. Obtener Historial de Estados

```http
GET /api/applications/{applicationId}/status-history
Authorization: Bearer {token}
```

**Permisos:** Todos los roles autenticados  
**Respuesta:**
```json
{
  "success": true,
  "message": "Historial obtenido exitosamente",
  "data": [
    {
      "historyId": "uuid",
      "applicationId": "app-123",
      "previousStatus": "SUBMITTED",
      "newStatus": "UNDER_REVIEW",
      "comments": "Documentos completos",
      "changedByRole": "ANALYST",
      "changedBy": "user-uuid",
      "changedAt": "2025-10-27T12:00:00"
    }
  ]
}
```

### 3. Obtener Transiciones Permitidas

```http
GET /api/applications/{applicationId}/allowed-transitions
Authorization: Bearer {token}
```

**Permisos:** Todos los roles autenticados  
**Respuesta:**
```json
{
  "success": true,
  "message": "Transiciones permitidas obtenidas exitosamente",
  "data": ["UNDER_REVIEW", "DOCUMENTS_PENDING", "CANCELLED"]
}
```

### 4. Obtener Estadísticas de Estados

```http
GET /api/applications/statistics
Authorization: Bearer {token}
```

**Permisos:** ANALYST, MANAGER, ADMIN  
**Respuesta:**
```json
{
  "success": true,
  "message": "Estadísticas obtenidas exitosamente",
  "data": {
    "DRAFT": 5,
    "SUBMITTED": 12,
    "UNDER_REVIEW": 8,
    "DOCUMENTS_PENDING": 3,
    "APPROVED": 45,
    "REJECTED": 10,
    "CANCELLED": 2
  }
}
```

---

## Validaciones Implementadas

### 1. Validación de Transición
```java
// Verifica que el nuevo estado sea válido desde el estado actual
isValidTransition(previousStatus, newStatus)
```

### 2. Validación de Rol
```java
// Verifica que el rol tenga permiso para el estado destino
isAllowedForRole(userRole, newStatus)
```

### 3. Validación de Permisos Especiales
```java
// Analistas solo pueden aprobar/rechazar si tienen la solicitud asignada
// Applicants solo pueden enviar o cancelar sus propias solicitudes
validateRolePermission(application, role, fromStatus, toStatus)
```

---

## Registro de Historial

Cada cambio de estado se registra en la tabla `application_status_history` con:

- `history_id` - ID único del registro
- `application_id` - ID de la solicitud
- `previous_status` - Estado anterior
- `new_status` - Estado nuevo
- `comments` - Comentarios del cambio
- `changed_by_role` - Rol del usuario que hizo el cambio
- `changed_by_user` - Usuario que hizo el cambio
- `changed_at` - Timestamp del cambio

---

## Casos de Uso

### 1. Applicant envía una solicitud
```
Estado: DRAFT → SUBMITTED
Rol: APPLICANT
Transición: Permitida
Registro en historial: [x]
```

### 2. Analyst revisa y aprueba
```
Estado: UNDER_REVIEW → APPROVED
Rol: ANALYST
Validación: Debe tener asignación [x]
Transición: Permitida
Registro en historial: [x]
```

### 3. Analyst solicita más documentos
```
Estado: UNDER_REVIEW → DOCUMENTS_PENDING
Rol: ANALYST
Comentarios: "Falta DNI actualizado"
Transición: Permitida
Registro en historial: [x]
```

### 4. Intentar transición inválida
```
Estado: REJECTED → APPROVED
Resultado: ERROR - Transición inválida
Mensaje: "Transición inválida de 'REJECTED' a 'APPROVED'. Transiciones permitidas: []"
```

### 5. Applicant intenta aprobar
```
Estado: SUBMITTED → APPROVED
Rol: APPLICANT
Resultado: ERROR - Sin permisos
Mensaje: "El rol 'APPLICANT' no tiene permiso para cambiar al estado 'APPROVED'"
```

---

## Beneficios Implementados

[x] **Control de flujo:** Solo transiciones válidas permitidas  
[x] **Seguridad:** Permisos por rol estrictos  
[x] **Auditoría:** Historial completo de todos los cambios  
[x] **Trazabilidad:** Quién, cuándo y por qué cambió el estado  
[x] **Validaciones:** Múltiples capas de validación  
[x] **Flexibilidad:** Configuración centralizada de transiciones  

---

## Próximos Pasos Recomendados

1. [ ] Implementar notificaciones automáticas al cambiar estado
2. [ ] Validar documentos requeridos antes de aprobar
3. [ ] Auto-asignar analistas disponibles
4. [ ] Agregar dashboard de métricas por estado
5. [ ] Implementar timeouts automáticos (expirar solicitudes viejas)
6. [ ] Agregar estados intermedios (PRE_APPROVED, etc.)

---

## Testing

Para probar los nuevos endpoints:

```bash
# Cambiar estado
curl -X PUT http://localhost:8080/api/applications/{appId}/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "UNDER_REVIEW", "comments": "Iniciando revisión"}'

# Ver historial
curl http://localhost:8080/api/applications/{appId}/status-history \
  -H "Authorization: Bearer {token}"

# Ver transiciones permitidas
curl http://localhost:8080/api/applications/{appId}/allowed-transitions \
  -H "Authorization: Bearer {token}"

# Ver estadísticas
curl http://localhost:8080/api/applications/statistics \
  -H "Authorization: Bearer {token}"
```

