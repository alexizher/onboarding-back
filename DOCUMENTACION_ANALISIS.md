# Análisis de Documentación - Categorización de Archivos .md

## Categorización Propuesta

### MANTENER EN ARCHIVOS SEPARADOS (Documentación Técnica Específica)

Estos archivos contienen documentación técnica detallada que es mejor mantener separada:

1. **onboarding-pymes/FUNCIONALIDADES_COMPLETAS.md** (511 líneas)
   - **Razón**: Documentación completa de todos los endpoints (109 endpoints)
   - **Uso**: Referencia técnica para desarrolladores
   - **Recomendación**: Mantener separado, agregar link en README

2. **onboarding-pymes/AUTH_SYSTEM_MISSING.md** (437 líneas)
   - **Razón**: Análisis detallado de funcionalidades de autenticación implementadas/faltantes
   - **Uso**: Referencia para seguimiento de implementación de auth
   - **Recomendación**: Mantener separado, agregar link en README

3. **onboarding-pymes/FUNCIONALIDADES_FALTANTES.md** (402 líneas)
   - **Razón**: Análisis comparativo requisitos vs implementado
   - **Uso**: Referencia para planificación
   - **Recomendación**: Mantener separado, agregar link en README

4. **onboarding-pymes/SSE_ANGULAR_GUIDE.md** (319 líneas)
   - **Razón**: Guía técnica específica para integración SSE con Angular
   - **Uso**: Referencia para desarrolladores frontend
   - **Recomendación**: Mantener separado, agregar link en README

5. **WORKFLOW_ESTADOS.md** (317 líneas)
   - **Razón**: Documentación técnica del sistema de workflow
   - **Uso**: Referencia para desarrolladores
   - **Recomendación**: Mantener separado, agregar link en README

6. **GIT_BRANCH_MANAGEMENT.md** (305 líneas)
   - **Razón**: Guía de gestión de ramas Git
   - **Uso**: Referencia para desarrollo colaborativo
   - **Recomendación**: Mantener separado, agregar link en README

7. **onboarding-pymes/SECURITY_LOGS_RECOMMENDATIONS.md** (278 líneas)
   - **Razón**: Recomendaciones técnicas de seguridad
   - **Uso**: Referencia para mejoras de seguridad
   - **Recomendación**: Mantener separado

8. **onboarding-pymes/AUTH_SYSTEM_COMPLETE.md** (254 líneas)
   - **Razón**: Documentación completa del sistema de auth
   - **Uso**: Referencia técnica completa
   - **Recomendación**: Mantener separado, agregar link en README

9. **onboarding-pymes/BLOCKED_TOKENS_OPTIONS.md** (176 líneas)
   - **Razón**: Documentación técnica sobre opciones de token blacklisting
   - **Uso**: Referencia técnica
   - **Recomendación**: Mantener separado o consolidar en AUTH_SYSTEM_COMPLETE.md

10. **onboarding-pymes/MODULOS_EVALUACION.md** (159 líneas)
    - **Razón**: Evaluación técnica de módulos
    - **Uso**: Referencia para desarrollo
    - **Recomendación**: Mantener separado

11. **onboarding-pymes/tests/README.md**
    - **Razón**: Documentación de tests
    - **Uso**: Referencia para ejecución de tests
    - **Recomendación**: Mantener separado, agregar link en README

---

### CONSOLIDAR EN README (Información General)

Estos archivos contienen información que debería estar en el README principal:

1. **README.md** (247 líneas) - ACTUALIZAR con:
   - [x] Resumen actualizado del sistema de autenticación
   - [x] Enlaces a documentación técnica
   - [x] Estado actual del proyecto
   - [x] Configuración actualizada (JWT 30 min, sesiones cortas, etc.)
   - [x] Lista de endpoints principales (resumen, detalles en FUNCIONALIDADES_COMPLETAS.md)

---

### ARCHIVAR O ELIMINAR (Documentación Temporal/Obsoleta)

Estos archivos son históricos o temporales y pueden archivarse o eliminarse:

1. **CORRECCIONES_CRITICAS.md** (243 líneas)
   - **Razón**: Historial de correcciones ya implementadas
   - **Recomendación**: Archivar o eliminar (ya están corregidas)

2. **ANALISIS_SISTEMA.md** (228 líneas)
   - **Razón**: Análisis inicial del sistema
   - **Recomendación**: Archivar (información útil pero histórica)

3. **RESUMEN_IMPLEMENTACION.md** (137 líneas)
   - **Razón**: Historial de implementación específica
   - **Recomendación**: Archivar (información histórica)

4. **VALIDACION_BUILD.md** (104 líneas)
   - **Razón**: Validación de build ya completada
   - **Recomendación**: Archivar o eliminar (ya validado)

5. **TESTING_SUMMARY.md** (100 líneas)
   - **Razón**: Resumen de tests (información histórica)
   - **Recomendación**: Archivar o consolidar en tests/README.md

6. **TEMP_ANALISIS_WORKFLOW.md** (60 líneas)
   - **Razón**: Archivo temporal (nombre lo indica)
   - **Recomendación**: Eliminar (información ya consolidada en WORKFLOW_ESTADOS.md)

7. **onboarding-pymes/HELP.md** (42 líneas)
   - **Razón**: Ayuda temporal o específica
   - **Recomendación**: Revisar contenido, archivar si es obsoleto o consolidar en README

8. **onboarding-pymes/MVP_EVALUATION.md** (230 líneas)
   - **Razón**: Evaluación del MVP
   - **Recomendación**: Archivar (evaluación histórica) o consolidar resumen en README

---

## Recomendación Final

### Estructura Propuesta de Documentación:

```
/
├── README.md (ACTUALIZAR)
│   ├── Descripción general
│   ├── Configuración actualizada
│   ├── Estado del proyecto
│   └── Enlaces a documentación técnica
│
├── docs/ (NUEVO - Crear carpeta)
│   ├── technical/
│   │   ├── FUNCIONALIDADES_COMPLETAS.md
│   │   ├── AUTH_SYSTEM_COMPLETE.md
│   │   ├── AUTH_SYSTEM_MISSING.md
│   │   ├── FUNCIONALIDADES_FALTANTES.md
│   │   ├── WORKFLOW_ESTADOS.md
│   │   └── MODULOS_EVALUACION.md
│   │
│   ├── guides/
│   │   ├── GIT_BRANCH_MANAGEMENT.md
│   │   └── SSE_ANGULAR_GUIDE.md
│   │
│   └── security/
│       ├── SECURITY_LOGS_RECOMMENDATIONS.md
│       └── BLOCKED_TOKENS_OPTIONS.md
│
└── archive/ (NUEVO - Crear carpeta)
    ├── CORRECCIONES_CRITICAS.md
    ├── ANALISIS_SISTEMA.md
    ├── RESUMEN_IMPLEMENTACION.md
    ├── VALIDACION_BUILD.md
    ├── TESTING_SUMMARY.md
    ├── TEMP_ANALISIS_WORKFLOW.md
    ├── MVP_EVALUATION.md
    └── HELP.md
```

### Acciones Sugeridas:

1. [x] Actualizar README.md con información actualizada y enlaces
2. [x] Crear carpeta `docs/` para organizar documentación técnica
3. [x] Crear carpeta `archive/` para documentación histórica
4. [x] Mover archivos a sus carpetas correspondientes
5. [ ] Eliminar TEMP_ANALISIS_WORKFLOW.md (temporal, ya consolidado)
