# Sistema de Autenticación Completo

## Resumen de Endpoints

### **AuthController** (`/api/auth`)

#### Endpoints Públicos:
1. [x] `GET /api/auth/test` - Test de conectividad
2. [x] `POST /api/auth/register` - Registro de nuevos usuarios
3. [x] `POST /api/auth/login` - Login de usuarios
4. [x] `GET /api/auth/check-email` - Verificar si un email está registrado
5. [x] `GET /api/auth/check-username` - Verificar si un username está en uso

#### Endpoints de Debug (pueden removerse en producción):
6. [x] `POST /api/auth/test-bcrypt` - Test de BCrypt
7. [x] `POST /api/auth/test-login` - Test de login

---

### **SecurityController** (`/api/security`)

#### Endpoints Públicos:
1. [x] `POST /api/security/password-reset/request` - Solicitar reset de contraseña
2. [x] `POST /api/security/password-reset/confirm` - Confirmar reset de contraseña con token
3. [x] `POST /api/security/validate-password` - Validar fortaleza de contraseña

#### Endpoints Protegidos (requieren autenticación):

**Gestión de Contraseñas:**
4. [x] `POST /api/security/change-password` - Cambiar contraseña de usuario autenticado
   - **Roles:** APPLICANT, ANALYST, MANAGER, ADMIN

**Gestión de Sesiones:**
5. [x] `POST /api/security/logout` - Cerrar sesión (invalida token y lo agrega a blacklist)
   - **Roles:** APPLICANT, ANALYST, MANAGER, ADMIN

6. [x] `GET /api/security/sessions` - Obtener sesiones activas del usuario
   - **Roles:** APPLICANT, ANALYST, MANAGER, ADMIN

7. [x] `DELETE /api/security/sessions/{sessionId}` - Invalidar una sesión específica
   - **Roles:** APPLICANT, ANALYST, MANAGER, ADMIN

8. [x] `POST /api/security/sessions/close-others` - Cerrar todas las demás sesiones (excepto la actual)
   - **Roles:** APPLICANT, ANALYST, MANAGER, ADMIN

**Gestión de Blacklist:**

9. [x] `POST /api/security/blacklist` - Bloquear un usuario
   - **Roles:** ANALYST, MANAGER, ADMIN
   - **Body:** `{ "userId": "...", "applicationId": "...", "reason": "..." }`

10. [x] `POST /api/security/blacklist/{userId}/unblacklist` - Desbloquear un usuario
    - **Roles:** MANAGER, ADMIN
    - **Body:** `{ "reason": "..." }`

11. [x] `GET /api/security/blacklist/{userId}` - Verificar si un usuario está bloqueado
    - **Roles:** ANALYST, MANAGER, ADMIN

12. [x] `GET /api/security/blacklist/{userId}/history` - Obtener historial de bloqueos de un usuario
    - **Roles:** ANALYST, MANAGER, ADMIN

**Logs y Auditoría:**

13. [x] `GET /api/security/logs` - Obtener logs de seguridad del usuario actual
    - **Roles:** APPLICANT, ANALYST, MANAGER, ADMIN

14. [x] `GET /api/security/login-attempts` - Obtener intentos de login recientes
    - **Roles:** APPLICANT, ANALYST, MANAGER, ADMIN
    - **Query Params:** `email` (opcional)

15. [x] `GET /api/security/tokens/blacklisted` - Obtener tokens blacklisted del usuario actual
    - **Roles:** APPLICANT, ANALYST, MANAGER, ADMIN

16. [x] `GET /api/security/report` - Obtener reporte de seguridad (solo ADMIN)
    - **Roles:** ADMIN

---

## Características de Seguridad Implementadas

### 1. **Autenticación JWT**
- [x] Tokens con JWT ID (`jti`) único para tracking
- [x] Tokens agregados a blacklist al hacer logout
- [x] Validación de tokens blacklisted en cada request
- [x] Tokens con expiración configurable (24h por defecto)

### 2. **Gestión de Sesiones**
- [x] Sesiones guardadas en BD con hash SHA-256 del token
- [x] Tracking de IP y User Agent
- [x] Invalidación de sesiones específicas
- [x] Cerrar todas las demás sesiones
- [x] Límite de 3 sesiones activas por usuario
- [x] Limpieza automática de sesiones expiradas

### 3. **Rate Limiting**
- [x] Tracking de intentos de login por IP
- [x] Tracking de intentos de login por email
- [x] Bloqueo temporal después de X intentos fallidos
- [x] Registro de todos los intentos en `login_attempts`

### 4. **Password Reset con Seguridad**
- [x] Tokens seguros (32 bytes, Base64 URL-safe)
- [x] Expiración de tokens (1 hora)
- [x] Bloqueo de tokens después de intentos fallidos
- [x] Cooldown después de intentos fallidos
- [x] Tracking de IP y User Agent
- [x] Validación de origen (IP/UA diferente = warning)
- [x] Registro de uso del token
- [x] Invalidación de tokens anteriores al generar uno nuevo

### 5. **Client Blacklist**
- [x] Bloqueo de usuarios para prevenir creación de aplicaciones
- [x] Historial de bloqueos/desbloqueos
- [x] Bloqueo específico por aplicación (opcional)
- [x] Tracking de quién bloqueó/desbloqueó y por qué

### 6. **Token Blacklist**
- [x] Revocación de tokens JWT al hacer logout
- [x] Tracking de tokens blacklisted por usuario
- [x] Limpieza automática de tokens antiguos (opcional)

### 7. **Security Logs**
- [x] Registro de todos los eventos de seguridad
- [x] Categorización por severidad (LOW, MEDIUM, HIGH, CRITICAL)
- [x] Tracking de IP, User Agent, y detalles
- [x] Reportes de seguridad para ADMIN

### 8. **Password Security**
- [x] Validación de fortaleza de contraseñas
- [x] Hash BCrypt con salt automático
- [x] Cambio de contraseña invalida todas las sesiones
- [x] Validación de contraseña actual antes de cambiar

---

## Tablas de Base de Datos Utilizadas

### Tablas Principales:
1. [x] `users` - Usuarios del sistema
2. [x] `roles` - Roles y permisos
3. [x] `user_sessions` - Sesiones activas de usuarios
4. [x] `login_attempts` - Intentos de login
5. [x] `password_reset_tokens` - Tokens de recuperación de contraseña
6. [x] `security_logs` - Logs de seguridad
7. [x] `token_blacklist` - Tokens JWT revocados
8. [x] `client_blacklist` - Usuarios bloqueados

---

## Flujos de Autenticación

### **1. Registro de Usuario:**
```
POST /api/auth/register
  → Valida email/username disponibles
  → Crea usuario con rol APPLICANT por defecto
  → Genera token JWT
  → Crea sesión
  → Registra en security_logs
```

### **2. Login:**
```
POST /api/auth/login
  → Valida rate limiting (IP y email)
  → Verifica credenciales
  → Valida usuario activo
  → Genera token JWT con jti
  → Crea sesión en BD
  → Registra login attempt
  → Registra en security_logs
```

### **3. Logout:**
```
POST /api/security/logout
  → Invalida sesión en BD
  → Agrega token a token_blacklist
  → Registra en security_logs
```

### **4. Password Reset:**
```
1. POST /api/security/password-reset/request
   → Valida email existe
   → Verifica límite de intentos
   → Genera token seguro
   → Invalida tokens anteriores
   → Registra en security_logs

2. POST /api/security/password-reset/confirm
   → Valida token (no expirado, no usado, no bloqueado)
   → Verifica intentos fallidos (bloquea si > 3)
   → Cambia contraseña
   → Marca token como usado
   → Invalida todas las sesiones
   → Registra en security_logs
```

### **5. Cambio de Contraseña:**
```
POST /api/security/change-password
  → Valida contraseña actual
  → Valida fortaleza de nueva contraseña
  → Cambia contraseña
  → Invalida todas las sesiones
  → Registra en security_logs
```

### **6. Bloquear Usuario:**
```
POST /api/security/blacklist
  → Valida usuario existe
  → Verifica que no esté ya bloqueado
  → Crea entrada en client_blacklist
  → Registra en security_logs
  → Usuario no puede crear aplicaciones
```

---

## Medidas de Seguridad

### **Protección contra Ataques:**
- [x] **Brute Force:** Rate limiting por IP y email
- [x] **Token Replay:** Token blacklist al logout
- [x] **Session Hijacking:** Tracking de IP/UA, invalidar sesiones
- [x] **Token Theft:** Blacklist de tokens comprometidos
- [x] **Password Reset Abuse:** Bloqueo de tokens, cooldown, límite de intentos

### **Auditoría:**
- [x] Todos los eventos críticos registrados
- [x] Tracking de IP y User Agent
- [x] Historial completo de bloqueos
- [x] Reportes de seguridad para administradores

### **Validaciones:**
- [x] Usuarios bloqueados no pueden crear aplicaciones
- [x] Tokens blacklisted no pueden autenticar
- [x] Sesiones expiradas automáticamente invalidadas
- [x] Tokens de reset con protección contra abuse

---

## Sistema Completado

**Estado:** Sistema de autenticación completo y funcional con todas las características de seguridad implementadas.

**Próximos pasos recomendados:**
1. Agregar refresh tokens (opcional, para mejorar UX)
2. Implementar 2FA (Two-Factor Authentication) para mayor seguridad
3. Agregar geolocalización para detectar accesos inusuales
4. Implementar notificaciones por email/SMS para eventos de seguridad

