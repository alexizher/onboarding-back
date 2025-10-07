# Onboarding Credit Platform

## 📋 Descripción del Proyecto

**Plataforma Web de Onboarding de Créditos para PYMES**

- **Vertical:** Web App
- **Sector de Negocio:** Fintech
- **Tecnología:** Spring Boot + Angular + MySQL

## 🎯 Objetivo

Desarrollar una aplicación web en la que las PYMES puedan:
- Solicitar créditos de manera digital
- Cargar documentos requeridos
- Firmar digitalmente
- Conocer el estado de su solicitud en tiempo real
- Acceder a paneles administrativos con filtros y tareas

## 🏗️ Arquitectura del Sistema

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

## 🔐 Sistema de Autenticación y Roles

### Autenticación JWT
- **Algoritmo:** HS256
- **Expiración:** 24 horas (configurable)
- **Headers:** `Authorization: Bearer <token>`

### Roles del Sistema
1. **ADMIN** - Administrador del sistema
   - Gestión completa de usuarios
   - Asignación de roles
   - Acceso a estadísticas
   - Eliminación de usuarios

2. **ANALYST** - Analista de créditos
   - Revisión de solicitudes
   - Evaluación de riesgo
   - Acceso a estadísticas
   - Actualización de estados KYC

3. **OPERATOR** - Operador
   - Gestión de solicitudes
   - Actualización de estados
   - Consulta de usuarios

4. **CLIENT** - Cliente (PYME)
   - Registro y perfil
   - Solicitud de créditos
   - Carga de documentos
   - Consulta de estado

## 🚀 Instalación y Configuración

### Prerrequisitos
- Java 17+
- Maven 3.5.6+
- MySQL 8.0+
- Node.js 18+ (para frontend)
- Podman/Docker (opcional)

### Variables de Entorno
Crear archivo `.env` basado en `.env.example`:

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

# JWT
JWT_SECRET=tu_clave_secreta_muy_larga_y_segura
JWT_EXPIRATION=86400000

# Aplicación
APP_ENV=development
APP_DEBUG=true
APP_URL=http://localhost:8080
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

## 🛡️ Seguridad

### Validaciones
- **Contraseñas:** Mínimo 8 caracteres, mayúsculas, minúsculas, números y símbolos
- **Email:** Formato válido y único
- **NIT:** Único por empresa
- **Roles:** Validación de permisos por endpoint

### Headers de Seguridad
- **HSTS:** Strict-Transport-Security
- **X-Frame-Options:** DENY
- **X-Content-Type-Options:** nosniff
- **CORS:** Configurado para Angular (localhost:4200)

## 📊 Entidades del Dominio

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

## 📝 Próximas Funcionalidades

### Must-have
- [ ] Registro de usuario y autenticación segura
- [ ] Sistema de roles y permisos
- [ ] Formulario dinámico que guarde avances
- [ ] Carga de documentos y firma digital
- [ ] Panel de administración para revisar solicitudes

### Nice-to-have
- [ ] Pre-evaluación de riesgo basada en IA
- [ ] Integración con sistemas de contabilidad
- [ ] Chat de soporte (bot o humano)
- [ ] Notificaciones en tiempo real
- [ ] Dashboard con métricas avanzadas

## 🤝 Contribución

1. Fork del repositorio
2. Crear rama feature: `git checkout -b feature/nueva-funcionalidad`
3. Commit de cambios: `git commit -m 'feat: agregar nueva funcionalidad'`
4. Push a la rama: `git push origin feature/nueva-funcionalidad`
5. Crear Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## 📞 Soporte

Para soporte técnico o consultas:
- **Documentación:** [Wiki del proyecto]
- **Issues:** [GitHub Issues]