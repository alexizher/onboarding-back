# Scripts de Pruebas por Módulo

Este directorio contiene los scripts de pruebas organizados por módulo del sistema.

## Estructura de Directorios

```
tests/
├── auth/              # Autenticación y autorización
│   └── auth_test.sh   ✅ Script de pruebas de autenticación
├── users/             # Gestión de usuarios
│   └── user_management_test.sh ✅ Script de pruebas de gestión de usuarios
├── applications/      # Solicitudes de crédito
│   ├── draft_form_test.sh ✅ Script de pruebas de formularios dinámicos
│   └── analyst_panel_test.sh ✅ Script de pruebas del panel de analistas
├── documents/         # Gestión de documentos
│   └── document_panel_test.sh ✅ Script de pruebas del panel de documentos
├── signatures/        # Firmas digitales
│   └── digital_signature_test.sh ✅ Script de pruebas de firmas digitales
├── catalogs/          # Catálogos (categorías, profesiones, destinos, etc.)
│   └── catalog_test.sh ✅ Script de pruebas de catálogos
├── kyc/               # Verificación KYC
│   └── kyc_test.sh   ✅ Script de pruebas de verificación KYC
├── risk/              # Evaluación de riesgo
│   └── risk_test.sh   ✅ Script de pruebas de evaluación de riesgo
├── notifications/     # Notificaciones
│   └── notification_test.sh ✅ Script de pruebas de notificaciones SSE
├── integration/       # Pruebas de integración
│   └── integration_test.sh ✅ Script de pruebas end-to-end
├── common.sh          # ✅ Funciones comunes para todos los scripts
└── README.md          # ✅ Este archivo
```

## Convenciones de Nombres

- `{MODULE}_test.sh` - Script principal de pruebas del módulo
- `{MODULE}_{feature}_test.sh` - Pruebas específicas de una funcionalidad

## Ejecución de Pruebas

### Antes de ejecutar las pruebas

1. Vaciar las tablas de la base de datos:
```bash
./scripts/clear_unused_tables.sh
```

2. Asegurarse de que la aplicación esté ejecutándose en `http://localhost:8080`

### Ejecutar todas las pruebas de un módulo

```bash
cd tests/{module}
./{module}_test.sh
```

### Ejecutar una prueba específica

```bash
cd tests/{module}
./{module}_{feature}_test.sh
```

## Dependencias entre Módulos

Algunos módulos dependen de otros:

1. **auth** - Base para todos los demás módulos (necesita crear usuarios)
2. **users** - Depende de **auth**
3. **catalogs** - Base para **applications** y **documents**
4. **applications** - Depende de **auth**, **users**, **catalogs**
5. **documents** - Depende de **auth**, **users**, **applications**
6. **signatures** - Depende de **auth**, **users**, **documents**
7. **kyc** - Depende de **auth**, **users**, **applications**
8. **risk** - Depende de **auth**, **users**, **applications**

## Orden Recomendado de Ejecución

1. `auth/` - Autenticación y roles base
2. `users/` - Gestión de usuarios
3. `catalogs/` - Catálogos del sistema
4. `applications/` - Solicitudes de crédito
5. `documents/` - Gestión de documentos
6. `signatures/` - Firmas digitales
7. `kyc/` - Verificación KYC
8. `risk/` - Evaluación de riesgo
9. `notifications/` - Notificaciones
10. `integration/` - Pruebas de integración end-to-end

## Variables de Entorno Comunes

Todos los scripts deben usar estas variables base:

```bash
BASE_URL="http://localhost:8080"
ADMIN_EMAIL="admin@example.com"
ADMIN_PASSWORD="Admin123!@#"
```

## Funciones Comunes

Los scripts pueden usar funciones comunes definidas en `common.sh`:

- `show_result` - Muestra resultado de prueba (éxito/fallo)
- `show_json` - Muestra JSON de forma legible
- `get_token` - Obtiene token de autenticación
- `create_user` - Crea un usuario con rol específico

