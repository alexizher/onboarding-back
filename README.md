# Plataforma Web de Onboarding de Créditos para PYMES

## Descripción del Proyecto

**Plataforma Web de Onboarding de Créditos para PYMES** es una aplicación fintech diseñada para permitir que las pequeñas y medianas empresas (PYMES) soliciten créditos de manera digital, con procesos menos burocráticos y tiempos de aprobación reducidos.

### Características Principales
- **Vertical:** Web App
- **Sector de Negocio:** Fintech
- **Tecnología:** Spring Boot 3.5.7 + Angular 18+ + MySQL 8.0

## Objetivo

Desarrollar una aplicación web en la que las PYMES puedan:
- [x] **Solicitar créditos** de manera digital con formulario dinámico que guarda avances
- [x] **Cargar documentos** requeridos de forma segura
- [x] **Firmar digitalmente** los documentos necesarios
- [x] **Conocer el estado** de su solicitud en tiempo real mediante notificaciones
- [x] **Acceder a paneles administrativos** con filtros y tareas para operadores

### Necesidad del Cliente

Las PYMES requieren financiación rápida y procesos de solicitud de crédito menos burocráticos. La plataforma permite:
- Recopilar y validar información de manera digital
- Reducir tiempos de aprobación mediante automatización
- Mejorar la experiencia del usuario con interfaz intuitiva

## Arquitectura del Sistema

### Backend (Spring Boot)
- **Framework:** Spring Boot 3.5.6
- **Base de Datos:** MySQL 8.0
- **Autenticación:** JWT (JSON Web Tokens)
- **Seguridad:** Spring Security con roles
- **Validación:** Bean Validation (Jakarta)
- **API:** RESTful

### Frontend (Angular)
- **Framework:** Angular 18+
- **Puerto:** 4200
- **Comunicación:** HTTP Client con JWT

### Base de Datos
- **Motor:** MySQL 8.0
- **Puerto:** 3306
- **Admin:** phpMyAdmin (Puerto 8081)

## Sistema de Autenticación y Seguridad

### Autenticación JWT (Sistema Bancario - Duraciones Cortas)
- **Algoritmo:** HS256
- **Expiración:** 30 minutos (1800000 ms) - Sistema bancario requiere tiempos cortos
- **Refresh Token:** 30 minutos
- **Headers:** `Authorization: Bearer <token>`
- **Sesiones:** Máximo 30 minutos de duración, timeout de inactividad: 15 minutos

### Características de Seguridad Implementadas
- [x] **Email Verification** - Verificación de emails con tokens (60 min expiración)
- [x] **Password History** - Prevención de reutilización de últimas 5 contraseñas
- [x] **Refresh Tokens** - Renovación de tokens sin re-autenticación
- [x] **Session Timeout** - Cierre automático después de 15 min de inactividad
- [x] **Progressive Account Lockout** - Bloqueo progresivo (2h → 4h → 8h) después de 3 intentos fallidos
- [x] **Rate Limiting** - Protección contra fuerza bruta (5 intentos por IP, 3 por email)
- [x] **Token Blacklisting** - Revocación de tokens JWT
- [x] **Client Blacklisting** - Bloqueo de usuarios maliciosos
- [x] **Session Management** - Gestión de sesiones múltiples con hash SHA-256
- [x] **CAPTCHA** - Requerido después de 3 intentos fallidos
- [x] **Security Audit Logs** - Registro completo de eventos de seguridad
- [x] **Password Reset Seguro** - Tokens con validación de IP/User Agent

### Roles del Sistema
1. **ADMIN** - Administrador del sistema
   - Gestión completa de usuarios y roles
   - Acceso a estadísticas y métricas
   - Desbloqueo de cuentas
   - Gestión de blacklists

2. **MANAGER** - Gerente
   - Supervisión de solicitudes
   - Aprobación de créditos
   - Acceso a estadísticas
   - Desbloqueo de cuentas

3. **ANALYST** - Analista de créditos
   - Revisión y análisis de solicitudes
   - Evaluación de riesgo
   - Actualización de estados KYC
   - Asignación de solicitudes

4. **APPLICANT** - Cliente (PYME)
   - Registro y perfil
   - Solicitud de créditos con guardado de borradores
   - Carga de documentos
   - Consulta de estado en tiempo real

## Instalación y Configuración

### Prerrequisitos
- Java 17+
- Maven 3.5.6+
- MySQL 8.0+
- Node.js 18+ (para frontend)
- Podman/Docker (opcional)

### Variables de Entorno
Crear archivo `.env` basado en `env.example`:

```bash
# Base de Datos
DB_HOST=localhost
DB_PORT=3306
DB_NAME=onboarding_db
DB_USER=onboarding_user
DB_PASSWORD=onboarding_password
DB_ROOT_PASSWORD=onboarding_password

# phpMyAdmin
PHPMYADMIN_PORT=8081

# JWT (Sistema Bancario - Duraciones Cortas)
JWT_SECRET=tu_clave_secreta_muy_larga_y_segura
JWT_EXPIRATION=1800000
# 30 minutos (1800000 ms) - Sistema bancario requiere tiempos cortos
JWT_REFRESH_EXPIRATION=1800000

# Session Configuration (Sistema Bancario)
SESSION_DURATION_HOURS=0.5
# 30 minutos - Sistema bancario requiere sesiones cortas
SESSION_INACTIVITY_TIMEOUT_MINUTES=15
# Timeout de inactividad: 15 minutos sin actividad = cierre de sesión

# Password Policy
PASSWORD_HISTORY_SIZE=5
# Guardar últimas 5 contraseñas para prevenir reutilización

# Email Verification
EMAIL_VERIFICATION_TOKEN_EXPIRATION_MINUTES=60
```

### Ejecución

#### 1. Base de Datos
```bash
# Con Podman
podman-compose up -d

# Con Docker
docker-compose up -d
```

#### 2. Backend
```bash
# Cargar variables de entorno
source .env

# Ejecutar aplicación
./mvnw spring-boot:run
```

#### 3. Frontend
```bash
# Instalar dependencias
npm install

# Ejecutar servidor de desarrollo
ng serve
```



### Health Check
- `GET /actuator/health` - Estado de la aplicación

## Seguridad y Validaciones

### Validaciones de Entrada
- **Contraseñas:** Mínimo 8 caracteres, mayúsculas, minúsculas, números (historial de últimas 5)
- **Email:** Formato válido, único y verificado
- **NIT:** Único por empresa
- **Roles:** Validación de permisos por endpoint con `@PreAuthorize`
- **Archivos:** Validación de tipo y tamaño en carga de documentos

### Headers de Seguridad
- **HSTS:** Strict-Transport-Security (31536000 segundos)
- **X-Frame-Options:** DENY
- **X-Content-Type-Options:** nosniff
- **CORS:** Configurado para Angular (localhost:4200)
- **Security Headers:** Configuración completa de Spring Security

### Protecciones Implementadas
- Rate Limiting en login (5 intentos por IP, 3 por email)
- Bloqueo progresivo de cuentas (2h → 4h → 8h)
- Token blacklisting para revocación
- Client blacklisting para usuarios bloqueados
- Session tracking con hash SHA-256
- Security audit logs completos

## Entidades del Dominio

### User (Usuario)
- Información personal y de contacto
- Estado KYC y nivel de riesgo
- Relación con empresa y roles

### Company (Empresa)
- Datos de la PYME
- Información fiscal y comercial
- Tipo de empresa

### Role (Rol)
- Definición de roles del sistema
- Permisos asociados

### CreditApplication (Solicitud de Crédito)
- Detalles de la solicitud
- Estado y montos
- Fechas y términos

### Document (Documento)
- Archivos subidos
- Estado de verificación
- Firma digital

### KYCVerification (Verificación KYC)
- Proceso de verificación
- Proveedores externos
- Estado de cumplimiento

## 🔧 Desarrollo

### Estructura de Branches
```
master
├── backend
   ├── feature/login-roles
   ├── feature/credit-applications
   └── feature/documents

```

### Convenciones de Commits
- **feat:** Nueva funcionalidad
- **fix:** Corrección de bugs
- **docs:** Documentación
- **style:** Formato de código
- **refactor:** Refactorización
- **test:** Pruebas
- **chore:** Tareas de mantenimiento

### Ejemplo de Commit
```bash
git commit -m "feat: implementar sistema de autenticación JWT con roles

- Agregar AuthController con endpoints de login/register
- Implementar JwtUtil para manejo de tokens
- Crear sistema de roles (ADMIN, ANALYST, OPERATOR, CLIENT)
- Configurar Spring Security con filtros JWT
- Agregar validaciones de contraseña personalizadas"
```

## Estado Actual del Proyecto

### Funcionalidades Must-Have Implementadas
- [x] **Registro y autenticación segura** - Sistema completo con JWT, email verification, password history
- [x] **Sistema de roles y permisos** - 4 roles (ADMIN, MANAGER, ANALYST, APPLICANT) con control de acceso
- [x] **Formulario dinámico con guardado de avances** - Borradores persistentes, validación condicional
- [x] **Carga de documentos** - Sistema completo con validación y verificación
- [x] **Panel de administración** - Paneles para analistas y administradores con filtros y estadísticas

### Funcionalidades Nice-to-Have Implementadas
- [x] **Pre-evaluación de riesgo** - Sistema automático de evaluación basado en reglas
- [x] **Notificaciones en tiempo real** - Server-Sent Events (SSE) para actualizaciones en vivo
- [x] **Dashboard con métricas** - Estadísticas completas de solicitudes y usuarios

### Métricas del Sistema
- **109 Endpoints** implementados en 12 controladores funcionales
- **Sistema de Autenticación Completo** con 16 endpoints de seguridad
- **Workflow de Estados** con validación de transiciones
- **Sistema de Notificaciones** SSE implementado
- **KYC/AML** con evaluación automática de riesgo

### Próximas Funcionalidades (Opcionales)
- [ ] Integración con sistemas de contabilidad de las PYMES
- [ ] Chat de soporte (bot o humano)
- [ ] Two-Factor Authentication (2FA) - Opcional para sistemas bancarios
- [ ] Integración con servicios externos de Email/SMS (actualmente modo mock)

## Documentación

### Documentación Técnica
- **[Funcionalidades Completas](docs/technical/FUNCIONALIDADES_COMPLETAS.md)** - Lista completa de 109 endpoints implementados
- **[Sistema de Autenticación](docs/technical/AUTH_SYSTEM_COMPLETE.md)** - Documentación completa del sistema de auth
- **[Funcionalidades Faltantes](docs/technical/FUNCIONALIDADES_FALTANTES.md)** - Análisis de requisitos vs implementado
- **[Workflow de Estados](docs/technical/WORKFLOW_ESTADOS.md)** - Sistema de gestión de estados de solicitudes
- **[Evaluación de Módulos](docs/technical/MODULOS_EVALUACION.md)** - Evaluación técnica de cada módulo

### Guías de Desarrollo
- **[Gestión de Ramas Git](docs/guides/GIT_BRANCH_MANAGEMENT.md)** - Guía para mantener ramas actualizadas
- **[Integración SSE con Angular](docs/guides/SSE_ANGULAR_GUIDE.md)** - Guía para integrar notificaciones en tiempo real

### Seguridad
- **[Recomendaciones de Security Logs](docs/security/SECURITY_LOGS_RECOMMENDATIONS.md)** - Mejores prácticas de logging
- **[Opciones de Token Blacklisting](docs/security/BLOCKED_TOKENS_OPTIONS.md)** - Análisis de implementación

### Tests
- **[Documentación de Tests](onboarding-pymes/tests/README.md)** - Guía para ejecutar tests de todos los módulos

### Archivo Histórico
- **[Archivo Histórico](archive/)** - Documentación histórica útil para auditorías y revisión

## Testing

El proyecto incluye scripts de prueba organizados por módulos en `onboarding-pymes/tests/`:

- `auth/` - Tests de autenticación y seguridad
- `users/` - Tests de gestión de usuarios
- `applications/` - Tests de solicitudes de crédito
- `documents/` - Tests de carga de documentos
- `kyc/` - Tests de verificación KYC
- `risk/` - Tests de evaluación de riesgo
- `notifications/` - Tests de notificaciones SSE
- `integration/` - Tests de integración end-to-end

Ver [documentación de tests](onboarding-pymes/tests/README.md) para más detalles.

## Desarrollo

### Estructura de Branches
```
developer (main development branch)
├── feature/authentication-system
├── feature/user-management
├── feature/credit-applications
└── feature/documents
```

### Convenciones de Commits
- **feat:** Nueva funcionalidad
- **fix:** Corrección de bugs
- **docs:** Documentación
- **style:** Formato de código
- **refactor:** Refactorización
- **test:** Pruebas
- **chore:** Tareas de mantenimiento

### Ejemplo de Commit
```bash
git commit -m "feat: implementar sistema de autenticación JWT con roles

- Agregar AuthController con endpoints de login/register
- Implementar JwtService para manejo de tokens
- Crear sistema de roles (ADMIN, MANAGER, ANALYST, APPLICANT)
- Configurar Spring Security con filtros JWT
- Agregar validaciones de contraseña personalizadas"
```

## Contribución

1. Fork del repositorio
2. Crear rama feature: `git checkout -b feature/nueva-funcionalidad`
3. Commit de cambios: `git commit -m 'feat: agregar nueva funcionalidad'`
4. Push a la rama: `git push origin feature/nueva-funcionalidad`
5. Crear Pull Request hacia `developer`

**Importante:** Ver [Guía de Gestión de Ramas](docs/guides/GIT_BRANCH_MANAGEMENT.md) para mantener ramas actualizadas.

## Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## Soporte

Para soporte técnico o consultas:
- **Documentación Técnica:** Ver carpeta `docs/`
- **Issues:** [GitHub Issues]