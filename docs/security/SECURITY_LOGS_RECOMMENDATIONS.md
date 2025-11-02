# Recomendaciones para Registros de Seguridad

## Análisis de lo que se guarda actualmente vs lo que deberíamos guardar

### 1. `security_logs` - Logs de Seguridad

#### **Actualmente se guarda:**
- `log_id`: UUID único
- `user_id`: ID del usuario (nullable - puede ser null si no hay usuario)
- `event_type`: Tipo de evento (LOGIN_SUCCESS, LOGIN_FAILED, PASSWORD_CHANGE, etc.)
- `ip_address`: Dirección IP del cliente
- `user_agent`: User Agent del navegador/cliente
- `details`: Descripción detallada del evento (TEXT)
- `timestamp`: Fecha y hora del evento
- `severity`: Nivel de severidad (LOW, MEDIUM, HIGH, CRITICAL)

#### **Lo que DEBERÍAMOS agregar:**

1. **`request_id` o `trace_id`** (VARCHAR 100)
   - Para rastrear requests específicos en logs distribuidos
   - Útil para debugging y correlación de eventos

2. **`endpoint` o `url`** (VARCHAR 500)
   - URL/endpoint que se accedió
   - Ej: `/api/auth/login`, `/api/users/123`
   - Útil para análisis de patrones de acceso

3. **`http_method`** (VARCHAR 10)
   - Método HTTP (GET, POST, PUT, DELETE, etc.)
   - Útil para entender el tipo de operación

4. **`status_code`** (INT)
   - Código de respuesta HTTP (200, 401, 403, 500, etc.)
   - Útil para identificar errores y patrones de fallos

5. **`response_time_ms`** (INT)
   - Tiempo de respuesta en milisegundos
   - Útil para detectar problemas de performance o ataques de timing

6. **`country_code`** (VARCHAR 2) - Opcional
   - Código ISO del país basado en IP
   - Útil para detectar accesos desde ubicaciones inusuales

7. **`risk_score`** (INT, 0-100) - Opcional
   - Puntaje de riesgo del evento
   - Calculado basado en múltiples factores

---

### 2. `password_reset_tokens` - Tokens de Recuperación

#### **Actualmente se guarda:**
- `token`: Token único
- `user_id`: ID del usuario
- `expires_at`: Fecha de expiración
- `used`: Boolean si fue usado
- `created_at`: Fecha de creación
- `ip_address`: IP desde donde se solicitó
- `user_agent`: User Agent del cliente

#### **Lo que DEBERÍAMOS agregar (Seguridad contra uso malintencionado):**

1. **`used_at`** (DATETIME) - Nullable
   - Fecha/hora exacta cuando se usó el token
   - Actualmente solo sabemos si fue usado, no cuándo

2. **`used_from_ip`** (VARCHAR 45) - Nullable
   - IP desde donde se usó el token (puede ser diferente a donde se generó)
   - Útil para detectar si el token fue usado desde otra ubicación
   - **CRÍTICO**: Validar coincidencia o bloquear si es muy diferente

3. **`used_from_user_agent`** (TEXT) - Nullable
   - User Agent desde donde se usó el token
   - Útil para detectar cambios de dispositivo
   - **CRÍTICO**: Validar coincidencia o marcar como sospechoso

4. **`is_blocked`** (BOOLEAN) - **NUEVO - CRÍTICO**
   - Bloquear token si hay múltiples intentos fallidos
   - Bloquear si se detecta actividad sospechosa
   - **Protege contra brute force**

5. **`failed_attempts`** (INT) - **NUEVO - CRÍTICO**
   - Contador de intentos fallidos de usar el token
   - Incrementar en cada validación fallida
   - Bloquear después de X intentos (ej: 3-5)

6. **`last_attempt_at`** (DATETIME) - Nullable - **NUEVO**
   - Última vez que se intentó usar el token
   - Útil para detectar patrones de ataque

7. **`blocked_at`** (DATETIME) - Nullable - **NUEVO**
   - Cuándo se bloqueó el token
   - Razón del bloqueo

8. **`blocked_reason`** (VARCHAR 255) - Nullable - **NUEVO**
   - Razón del bloqueo: "MAX_FAILED_ATTEMPTS", "IP_MISMATCH", "SUSPICIOUS_ACTIVITY"

9. **`max_attempts`** (INT, default: 3) - Opcional
   - Límite máximo de intentos antes de bloquear
   - Permite configurar por token

10. **`cooldown_until`** (DATETIME) - Nullable - **NUEVO**
    - Cooldown después de intentos fallidos
    - Evita brute force continuo
    - Ej: después de 3 intentos fallidos, esperar 15 minutos

11. **`request_source`** (VARCHAR 50) - Opcional
    - Origen de la solicitud: "web", "mobile", "api"
    - Útil para análisis de uso por plataforma

12. **`email_sent`** (BOOLEAN)
    - Si el email con el token fue enviado exitosamente
    - Útil para debugging de problemas de email

13. **`email_sent_at`** (DATETIME) - Nullable
    - Cuándo se envió el email
    - Útil para medir tiempo de entrega

14. **`validation_count`** (INT) - **NUEVO**
    - Contador de veces que se validó el token (sin usarlo)
    - Útil para detectar tokens que están siendo "probados"

#### **Medidas de seguridad CRÍTICAS que faltan:**

1. **Rate Limiting por Token:**
   - Limitar intentos de validación/uso del token
   - Bloquear token después de X intentos fallidos
   - Implementar cooldown después de intentos fallidos

2. **Validación de Origen:**
   - Validar IP de uso vs IP de generación (opcional, con warnings)
   - Validar User Agent de uso vs User Agent de generación
   - Marcar como sospechoso si hay cambios significativos

3. **Tracking de Intentos:**
   - Registrar cada intento de uso del token (válido o inválido)
   - Guardar en `security_logs` o tabla separada
   - Detectar patrones de ataque

4. **Notificación al Usuario:**
   - Notificar cuando se usa el token exitosamente
   - Notificar cuando hay intentos sospechosos
   - Email/SMS de alerta

5. **Bloqueo Proactivo:**
   - Bloquear token si se detecta actividad sospechosa
   - Invalidar todos los tokens del usuario si hay múltiples fallos
   - Bloquear cuenta temporalmente si hay demasiados intentos

6. **Expiración Dinámica:**
   - Reducir tiempo de expiración si hay intentos fallidos
   - Invalidar inmediatamente si se detecta compromiso

---

### 3. `login_attempts` - Intentos de Login

#### **Actualmente se guarda:**
- `attempt_id`: UUID único
- `ip_address`: IP del intento
- `email`: Email usado en el intento
- `attempt_time`: Fecha/hora del intento
- `successful`: Boolean si fue exitoso
- `failure_reason`: Razón del fallo (nullable)
- `user_agent`: User Agent del cliente

#### **Lo que DEBERÍAMOS agregar:**

1. **`user_id`** (VARCHAR 36) - Nullable
   - ID del usuario cuando se identifica correctamente
   - Actualmente solo guardamos el email, pero el user_id facilita queries
   - Nullable porque puede fallar antes de identificar al usuario

2. **`username`** (VARCHAR 100) - Nullable
   - Username usado en el intento (si aplica)
   - Algunos sistemas permiten login con username o email

3. **`country_code`** (VARCHAR 2) - Opcional
   - País basado en IP geolocalizada
   - Útil para detectar accesos desde ubicaciones inusuales

4. **`session_id`** (VARCHAR 100) - Nullable
   - ID de sesión si el login fue exitoso y se creó sesión
   - Permite correlacionar intentos exitosos con sesiones

5. **`device_fingerprint`** (VARCHAR 255) - Opcional
   - Hash del fingerprint del dispositivo
   - Útil para detectar uso desde dispositivos conocidos vs nuevos

6. **`risk_score`** (INT, 0-100) - Opcional
   - Puntaje de riesgo del intento
   - Basado en: IP conocida, device fingerprint, ubicación, etc.

7. **`is_suspicious`** (BOOLEAN)
   - Flag que indica si el intento fue marcado como sospechoso
   - Basado en múltiples factores: IP desconocida, ubicación inusual, etc.

---

## Eventos adicionales que deberíamos registrar en `security_logs`

### Eventos de Autenticación:
- `LOGOUT` - Cuando un usuario cierra sesión
- `SESSION_EXPIRED` - Cuando una sesión expira
- `SESSION_INVALIDATED` - Cuando una sesión se invalida manualmente
- `MULTIPLE_SESSIONS_DETECTED` - Cuando un usuario tiene múltiples sesiones activas

### Eventos de Autorización:
- `PERMISSION_DENIED` - Intentos de acceso no autorizado
- `ROLE_CHANGED` - Cuando se cambia el rol de un usuario
- `PRIVILEGE_ESCALATION_ATTEMPT` - Intentos sospechosos de escalación de privilegios

### Eventos de Datos Sensibles:
- `SENSITIVE_DATA_ACCESSED` - Acceso a datos sensibles (documentos, aplicaciones)
- `BULK_DATA_EXPORT` - Exportación masiva de datos
- `DATA_MODIFIED` - Modificaciones críticas de datos

### Eventos de Aplicaciones:
- `APPLICATION_CREATED` - Nueva solicitud de crédito creada
- `APPLICATION_STATUS_CHANGED` - Cambio de estado de aplicación
- `APPLICATION_APPROVED` - Aprobación de aplicación
- `APPLICATION_REJECTED` - Rechazo de aplicación

### Eventos de Documentos:
- `DOCUMENT_UPLOADED` - Subida de documento
- `DOCUMENT_DOWNLOADED` - Descarga de documento
- `DOCUMENT_DELETED` - Eliminación de documento
- `DOCUMENT_SIGNED` - Documento firmado digitalmente

### Eventos del Sistema:
- `SYSTEM_ERROR` - Errores del sistema
- `CONFIGURATION_CHANGED` - Cambios en configuración
- `BACKUP_CREATED` - Creación de backup
- `SYSTEM_MAINTENANCE` - Eventos de mantenimiento

---

## Recomendaciones de Implementación

### Prioridad ALTA (Implementar primero):
1. Agregar `used_at` y `used_from_ip` a `password_reset_tokens`
2. Agregar `user_id` a `login_attempts` cuando se identifica
3. Agregar `endpoint` y `http_method` a `security_logs`

### Prioridad MEDIA (Implementar después):
4. Agregar `session_id` a `login_attempts` para correlación
5. Agregar `status_code` a `security_logs`
6. Registrar eventos de logout y sesiones expiradas

### Prioridad BAJA (Nice to have):
7. Agregar geolocalización (country_code)
8. Agregar device fingerprinting
9. Agregar risk scoring

---

## Consultas útiles que se podrían hacer con estos datos adicionales

1. **Detectar patrones sospechosos:**
   - "¿Cuántos intentos de login desde diferentes países en las últimas 24h?"
   - "¿Hay accesos desde IPs conocidas por ataques?"
   - "¿Cambios de contraseña desde ubicaciones inusuales?"

2. **Análisis de comportamiento:**
   - "¿Qué endpoints son más accedidos por cada rol?"
   - "¿Cuánto tiempo promedio toma cada operación?"
   - "¿Hay picos de actividad en ciertos horarios?"

3. **Auditoría y cumplimiento:**
   - "¿Quién accedió a qué datos sensibles y cuándo?"
   - "¿Hay cambios no autorizados en configuraciones?"
   - "¿Se cumplen los SLA de respuesta?"

4. **Detectar vulnerabilidades:**
   - "¿Hay intentos de SQL injection o XSS?"
   - "¿Hay accesos a endpoints no documentados?"
   - "¿Hay patrones de timing attacks?"

