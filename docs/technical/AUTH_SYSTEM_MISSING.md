# Funcionalidades Faltantes en el Sistema de Autenticación

## Fecha de Análisis
Análisis realizado comparando el sistema actual con mejores prácticas y requisitos de seguridad empresarial.

---

## LO QUE ESTÁ IMPLEMENTADO

### Autenticación Básica
- [x] JWT Authentication (HS256, **30 min expiración** - Sistema Bancario)
- [x] Login con email/password
- [x] Registro de usuarios
- [x] Password hashing con BCrypt
- [x] Validación de fortaleza de contraseñas
- [x] **Email Verification** - Verificación de emails con tokens (60 min expiración)
- [x] **Refresh Tokens** - Renovación de tokens (30 min expiración - Sistema Bancario)

### Seguridad
- [x] Rate limiting por IP y email
- [x] Session management con hash SHA-256
- [x] Token blacklisting
- [x] Client blacklisting
- [x] Security audit logs
- [x] Password reset con tokens seguros
- [x] Bloqueo temporal por IP/email
- [x] **Bloqueo progresivo de cuentas** (2h → 4h → 8h → ...) después de 3 intentos fallidos
- [x] **Password History** - Prevención de reutilización de últimas 5 contraseñas
- [x] **Session Timeout por Inactividad** - 15 minutos sin actividad cierra sesión automáticamente

### Gestión
- [x] Gestión de sesiones múltiples
- [x] Invalidación de sesiones específicas
- [x] Cerrar otras sesiones
- [x] Tracking de IP y User Agent
- [x] **Duración de sesiones cortas** - 30 minutos (sistema bancario requiere tiempos cortos)

---

## LO QUE FALTA

### [x] **IMPLEMENTADO** (Seguridad Crítica)

#### 1. **Bloqueo Progresivo de Cuentas** [x]
**Estado:** [x] **IMPLEMENTADO** (2025-01-XX)

**Implementado:**
- [x] Campo `failedLoginAttempts` en la entidad `User`
- [x] Campo `lockoutUntil` para fecha/hora de desbloqueo
- [x] Campo `lockoutLevel` para nivel de bloqueo progresivo
- [x] Bloqueo después de 3 intentos fallidos: **2 horas** (nivel 1)
- [x] Si intentan durante bloqueo: **4 horas** (nivel 2)
- [x] Si vuelven a intentar: **8 horas** (nivel 3)
- [x] Y así sucesivamente: 16h, 32h... (máximo 24h)
- [x] Contador de intentos fallidos por usuario
- [x] Endpoint para desbloquear cuenta manualmente (MANAGER, ADMIN)
- [x] Endpoint para verificar estado de bloqueo (ANALYST, MANAGER, ADMIN)
- [x] Reseteo automático al login exitoso
- [x] Incremento progresivo de duración si intentan durante bloqueo

**Archivos:**
- [x] `AccountLockoutService.java` - Servicio con lógica de bloqueo progresivo
- [x] `User.java` - Campos agregados (`failedLoginAttempts`, `lockoutUntil`, `lockoutLevel`)
- [x] `AuthService.java` - Integrado en login
- [x] `SecurityController.java` - Endpoints `/accounts/{userId}/unlock` y `/accounts/{userId}/lockout-status`

**Impacto:** [x] **RESUELTO** - Protección contra ataques persistentes con bloqueo progresivo
**Esfuerzo:** [x] Completado

---

#### 2. **Email Verification** [x]
**Estado:** [x] **IMPLEMENTADO** (2025-11-02)

**Implementado:**
- [x] Campo `emailVerified` y `emailVerifiedAt` en la entidad `User`
- [x] Entidad `EmailVerificationToken` para tokens de verificación
- [x] Generación de token de verificación al registrarse
- [x] Endpoint `POST /api/auth/verify-email` para verificar email
- [x] Endpoint `POST /api/auth/resend-verification` para reenviar token
- [x] Tokens de verificación expiran en 60 minutos
- [x] Invalidación de tokens anteriores al generar uno nuevo
- [x] Tracking de IP y User Agent en tokens
- [x] Logs de seguridad para eventos de verificación

**Archivos:**
- [x] `EmailVerificationToken.java` - Entidad para tokens de verificación
- [x] `EmailVerificationTokenRepository.java` - Repositorio
- [x] `EmailVerificationService.java` - Servicio con lógica de verificación
- [x] `User.java` - Campos `emailVerified` y `emailVerifiedAt` agregados
- [x] `AuthService.java` - Integrado en registro de usuarios
- [x] `AuthController.java` - Endpoints `/verify-email` y `/resend-verification`

**Pendiente (Opcional):**
- Envío automático de email con link de verificación (requiere integración con servicio de email)
- Bloquear funcionalidades hasta verificar email (opcional)

**Impacto:** [x] **RESUELTO** - Mejora seguridad y reduce spam/usuarios falsos
**Esfuerzo:** [x] Completado

---

#### 3. **Password History** [x]
**Estado:** [x] **IMPLEMENTADO** (2025-11-02)

**Implementado:**
- [x] Tabla `password_history` para almacenar contraseñas anteriores
- [x] Guardar hash de contraseña en historial al cambiar contraseña
- [x] Validar que nueva contraseña no esté en historial (últimas 5 contraseñas)
- [x] Validación integrada en `changePassword()` y `resetPasswordWithToken()`
- [x] Limpieza automática del historial (mantener solo últimas 5)
- [x] Logs de seguridad para intentos de reutilización de contraseñas

**Archivos:**
- [x] `PasswordHistory.java` - Entidad para historial de contraseñas
- [x] `PasswordHistoryRepository.java` - Repositorio con queries
- [x] `PasswordHistoryService.java` - Servicio con lógica de historial
- [x] `AuthService.java` - Integrado en cambio y reset de contraseñas

**Pendiente (Opcional):**
- Endpoint para ver historial de cambios de contraseña (ADMIN)

**Impacto:** [x] **RESUELTO** - Previene reutilización de contraseñas comprometidas
**Esfuerzo:** [x] Completado

---

### **MEDIA PRIORIDAD** (Mejoras de UX y Seguridad)

#### 4. **Refresh Tokens** [x]
**Estado:** [x] **IMPLEMENTADO** (2025-11-02) - **Duración Corta para Sistema Bancario**

**Implementado:**
- [x] Entidad `RefreshToken` para almacenar refresh tokens
- [x] Generar refresh token al hacer login (duración: **30 minutos** - Sistema Bancario)
- [x] Guardar refresh tokens en BD con hash SHA-256
- [x] Endpoint `POST /api/auth/refresh-token` para renovar access token
- [x] Endpoint `POST /api/auth/revoke-token` para revocar refresh token
- [x] Validación de refresh tokens (expiración, revocación)
- [x] Tracking de refresh tokens por dispositivo/sesión (deviceId)
- [x] Tracking de IP y User Agent en refresh tokens
- [x] Limpieza automática de tokens expirados

**Archivos:**
- [x] `RefreshToken.java` - Entidad para refresh tokens
- [x] `RefreshTokenRepository.java` - Repositorio con queries
- [x] `RefreshTokenService.java` - Servicio con lógica de refresh tokens
- [x] `AuthService.java` - Integrado en login
- [x] `AuthController.java` - Endpoints `/refresh-token` y `/revoke-token`

**Configuración Especial (Sistema Bancario):**
- Duración corta: **30 minutos** (en lugar de 7-30 días) para mayor seguridad
- Sin rotación automática de refresh tokens (se mantiene el mismo hasta expirar)

**Pendiente (Opcional):**
- Rotación de refresh tokens (generar nuevo al usar)
- Invalidar refresh tokens al hacer logout (actualmente solo se pueden revocar manualmente)

**Impacto:** [x] **RESUELTO** - Mejora UX con tiempos cortos adecuados para sistema bancario
**Esfuerzo:** [x] Completado

---

#### 5. **Two-Factor Authentication (2FA)**
**Estado:** **NO IMPLEMENTADO**

**Problema Actual:**
- Solo autenticación con password
- No hay capa adicional de seguridad para cuentas sensibles

**Lo que falta:**
- Campo `twoFactorEnabled` y `twoFactorSecret` en `User`
- Endpoint `POST /api/auth/enable-2fa` para habilitar 2FA
- Endpoint `POST /api/auth/disable-2fa` para deshabilitar (requiere password)
- Generar QR code para configurar TOTP (Google Authenticator, Authy)
- Endpoint `POST /api/auth/verify-2fa` para verificar código durante login
- Opcional: SMS 2FA como alternativa a TOTP
- Backup codes para recuperación

**Impacto:** **MEDIO** - Seguridad adicional para cuentas sensibles (ANALYST, MANAGER, ADMIN)
**Esfuerzo:** 4-5 días (incluye biblioteca TOTP)
**Complejidad:** Alta

---

#### 6. **Session Timeout Configurable** [x]
**Estado:** [x] **IMPLEMENTADO** (2025-11-02) - **Configurado para Sistema Bancario**

**Implementado:**
- [x] Configuración de timeout de inactividad: **15 minutos** (sistema bancario)
- [x] Duración de sesiones: **30 minutos** (0.5 horas)
- [x] Actualizar `lastActivity` en `user_sessions` con cada request
- [x] Invalidar sesiones inactivas automáticamente
- [x] Verificación de inactividad en `SessionService.validateSession()`
- [x] Limpieza automática de sesiones expiradas e inactivas (cada 5 minutos)
- [x] Configuración en `application.properties` (`session.inactivity-timeout-minutes`)
- [x] Validación en `JwtAuthenticationFilter` para rechazar sesiones expiradas

**Archivos:**
- [x] `SessionService.java` - Lógica de timeout de inactividad
- [x] `SessionProperties.java` - Configuración de `inactivityTimeoutMinutes`
- [x] `JwtAuthenticationFilter.java` - Validación de sesiones activas
- [x] `application.properties` - Configuración de timeouts

**Configuración Especial (Sistema Bancario):**
- JWT expiration: **30 minutos** (1800000 ms)
- Session duration: **30 minutos** (0.5 horas)
- Inactivity timeout: **15 minutos**
- Sin opción de "Remember Me" (sistema bancario requiere tiempos cortos)

**Pendiente (Opcional):**
- Notificar al usuario antes de expirar sesión (requiere frontend)
- Opción de "Remember Me" (no aplicable para sistema bancario)

**Impacto:** [x] **RESUELTO** - Mejora seguridad con timeouts cortos adecuados para sistema bancario
**Esfuerzo:** [x] Completado

---

#### 7. **Device Tracking**
**Estado:** **NO IMPLEMENTADO**

**Problema Actual:**
- Solo se guarda IP y User Agent
- No hay identificación única de dispositivos

**Lo que falta:**
- Generar device fingerprint (combinación de características del navegador)
- Guardar device ID o fingerprint en `user_sessions`
- Lista de "dispositivos confiables" por usuario
- Notificaciones cuando se inicia sesión desde dispositivo nuevo
- Opción de marcar/desmarcar dispositivo como confiable
- Endpoint `GET /api/security/devices` para listar dispositivos activos

**Impacto:** **MEDIO** - Detecta accesos sospechosos
**Esfuerzo:** 2-3 días
**Complejidad:** Media

---

#### 9. **Email/SMS Notifications** [x]
**Estado:** [x] **IMPLEMENTADO (Modo Mock - Sin servicios externos)**

**Implementado:**
- [x] `EmailNotification.java` y `SmsNotification.java` - Entidades para almacenar notificaciones
- [x] `NotificationService.java` - Servicio para gestionar notificaciones Email/SMS
- [x] Modo Mock: Guarda notificaciones en BD sin enviarlas realmente (sin servicios externos)
- [x] Integrado en eventos de seguridad:
  - Registro de nuevo usuario
  - Login desde dispositivo/ubicación nueva
  - Cambio de contraseña
  - Bloqueo de cuenta
  - Actividad sospechosa
  - Verificación de email
  - Recuperación de contraseña
- [x] `NotificationManagementController.java` - Endpoints para gestión de notificaciones:
  - `GET /api/notifications-management/emails` - Obtener notificaciones de email del usuario
  - `GET /api/notifications-management/emails/unread` - Obtener notificaciones no leídas
  - `PUT /api/notifications-management/emails/{notificationId}/read` - Marcar como leída
  - `GET /api/notifications-management/emails/unread/count` - Contador de no leídas
  - `GET /api/notifications-management/sms` - Obtener notificaciones de SMS del usuario
- [x] Configuración en `application.properties`:
  - `notification.email.enabled=true`
  - `notification.sms.enabled=false`
  - `notification.mock.enabled=true` (modo mock activado)
  - `notification.max-retries=3`

**Nota:** En producción, se puede integrar con servicios externos (JavaMailSender, Twilio, AWS SNS, etc.) cambiando `notification.mock.enabled=false` y agregando la implementación real.

---

### **BAJA PRIORIDAD** (Nice-to-Have)

#### 8. **OAuth/Social Login** **NO APLICABLE PARA SISTEMAS BANCARIOS**
**Estado:** **NO IMPLEMENTADO** - **NO RECOMENDADO PARA SISTEMAS BANCARIOS**

**Razones por las que NO se usa en sistemas bancarios:**
- **Seguridad y Regulaciones**: Requieren control total sobre la autenticación
- **KYC/AML**: Necesitan identificación verificada, no solo una cuenta social
- **Auditabilidad**: Deben cumplir con regulaciones estrictas (Ley de Protección de Datos Financieros)
- **Control de Riesgo**: No pueden depender de terceros para autenticación crítica
- **Verificación de Identidad**: Las cuentas sociales no cumplen requisitos bancarios de identificación

**Lo que falta (si se implementara para otros tipos de sistemas):**
- Integración con OAuth 2.0 providers (Google, Facebook, Microsoft)
- Endpoint `POST /api/auth/oauth/{provider}` para login social
- Vincular cuenta social con cuenta existente
- Opción de registrar con cuenta social

**Impacto:** **BAJO** - NO recomendado para sistemas bancarios
**Esfuerzo:** 3-4 días por proveedor
**Complejidad:** Media

**Recomendación:** **NO IMPLEMENTAR** para sistemas bancarios. OAuth 2.0 sí puede usarse para:
- [x] Open Banking (conexión entre instituciones financieras)
- [x] [x] APIs para terceros con control total (no para login de usuarios finales)

---

---

#### 9. **Geolocation Tracking**
**Estado:** **NO IMPLEMENTADO**

**Lo que falta:**
- Detectar geolocalización por IP (usando servicio externo)
- Guardar país/ciudad en `login_attempts` y `user_sessions`
- Notificar al usuario cuando se inicia sesión desde ubicación inusual
- Endpoint para ver historial de ubicaciones de login

**Impacto:** **BAJO** - Detecta accesos sospechosos pero requiere servicio externo
**Esfuerzo:** 2-3 días (incluye integración con servicio de geolocalización)
**Complejidad:** Media

---

#### 10. **Account Recovery Avanzado**
**Estado:** **PARCIALMENTE IMPLEMENTADO**

**Problema Actual:**
- Solo password reset con tokens
- No hay recuperación de cuenta bloqueada
- No hay preguntas de seguridad

**Lo que falta:**
- Preguntas de seguridad opcionales para recuperación
- Recuperación de cuenta bloqueada (vía email de ADMIN)
- Opción de recuperar cuenta mediante email y DNI
- Historial de recuperaciones de cuenta

**Impacto:** **BAJO** - Mejora recuperación pero no es crítico
**Esfuerzo:** 2-3 días
**Complejidad:** Media

---

#### 12. **Login Activity Dashboard**
**Estado:** **NO IMPLEMENTADO**

**Lo que falta:**
- Endpoint `GET /api/security/activity` con actividad reciente
- Dashboard visual de intentos de login, cambios de contraseña, etc.
- Gráficos de actividad por día/semana
- Exportar historial de seguridad (PDF/CSV)

**Impacto:** **BAJO** - Mejora visibilidad pero no es crítico
**Esfuerzo:** 2-3 días
**Complejidad:** Media

---

## RESUMEN DE PRIORIDADES

### [x] **IMPLEMENTADO**
1. [x] **Bloqueo Progresivo de Cuentas** - Bloqueo escalonado (2h, 4h, 8h, etc.) después de múltiples intentos
2. [x] **Email Verification** - Validación de emails con tokens (60 min expiración)
3. [x] **Password History** - Prevención de reutilización de últimas 5 contraseñas
4. [x] **Refresh Tokens** - Renovación de tokens con duración corta (30 min - Sistema Bancario)
5. [x] **Session Timeout Configurable** - Timeout de inactividad (15 min) y duración corta (30 min - Sistema Bancario)

### **CRÍTICO** (Debe implementarse)
_No hay funcionalidades críticas pendientes_

### **IMPORTANTE** (Recomendado)
1. **Two-Factor Authentication** - Seguridad adicional para cuentas sensibles

### **OPCIONAL** (Nice-to-Have)
1. Device Tracking
2. OAuth/Social Login
3. Geolocation Tracking
4. Email/SMS Notifications
5. Account Recovery Avanzado
6. Login Activity Dashboard

---

## RECOMENDACIÓN

**Para MVP/Producción inicial:**
1. [x] **Bloqueo Progresivo de Cuentas** (crítico) - **IMPLEMENTADO**
2. [x] **Email Verification** (crítico) - **IMPLEMENTADO**
3. [x] **Password History** (recomendado) - **IMPLEMENTADO**
4. [x] **Refresh Tokens** (recomendado) - **IMPLEMENTADO** (duración corta para sistema bancario)
5. [x] **Session Timeout Configurable** (recomendado) - **IMPLEMENTADO** (timeout de inactividad)

**Configuración para Sistema Bancario:**
- JWT tokens: **30 minutos** de duración
- Refresh tokens: **30 minutos** de duración
- Sesiones: **30 minutos** de duración
- Timeout de inactividad: **15 minutos**
- Si el usuario no hace clic en "continuar con la sesión", debe iniciar login nuevamente

**Para versiones futuras:**
- Two-Factor Authentication (para cuentas ADMIN/MANAGER)
- Device Tracking
- Geolocation Tracking
- Email/SMS Notifications

---

## NOTAS ADICIONALES

### Mejoras Menores (Quick Wins)
- [x] Agregar campo `failedLoginAttempts` en `User` para tracking por cuenta - **IMPLEMENTADO**
- [x] Agregar campo `lockoutUntil` en `User` para bloqueo temporal - **IMPLEMENTADO** (como `lockoutUntil`)
- [x] Mejorar mensajes de error (no revelar si email existe o no) - **IMPLEMENTADO**
- [x] Agregar sistema de CAPTCHA requerido en login después de X intentos fallidos - **IMPLEMENTADO**

**Implementado:**
- [x] `CaptchaService.java` - Servicio para verificar si se requiere CAPTCHA basado en intentos fallidos
- [x] Endpoint `GET /api/auth/captcha-required` para verificar si se requiere CAPTCHA
- [x] Validación de CAPTCHA en login si se requiere (después de 3 intentos fallidos en 15 minutos)
- [x] Mensajes de error genéricos que no revelan si el email existe o no en password reset
- [x] Configuración en `application.properties` (`security.captcha.required-after-attempts`, `security.captcha.window-minutes`)

**Pendiente (Opcional):**
- Validación real de CAPTCHA con servicio externo (Google reCAPTCHA, hCaptcha, etc.) - Actualmente solo se verifica que se proporcione si es requerido

### Mejoras de Arquitectura
- Separar `AuthService` en múltiples servicios (LoginService, RegistrationService, etc.)
- Implementar Event-Driven Architecture para eventos de seguridad
- Agregar Circuit Breaker para servicios externos (email, SMS)

---

**Última actualización:** 2025-11-02

## ACTUALIZACIONES RECIENTES

### 2025-11-02 - Implementación Completa de Funcionalidades Críticas
- [x] **Email Verification**: Sistema completo de verificación de emails con tokens
- [x] **Password History**: Prevención de reutilización de últimas 5 contraseñas
- [x] **Refresh Tokens**: Implementación con duración corta (30 min) para sistema bancario
- [x] **Session Timeout**: Timeout de inactividad (15 min) y duración corta de sesiones (30 min)
- [x] **Ajustes para Sistema Bancario**: Todas las duraciones reducidas a tiempos cortos para cumplir requisitos bancarios
- [x] **Limpieza de código**: Corrección de todos los problemas de linter

