# Opciones de Implementación: Bloqueo de Tokens

## Situación Actual

El bloqueo del token está **directamente en la tabla `password_reset_tokens`** con los campos:
- `is_blocked` (BOOLEAN)
- `blocked_at` (DATETIME)
- `blocked_reason` (VARCHAR)
- `cooldown_until` (DATETIME)
- `failed_attempts` (INT)

## Opciones de Diseño

### **Opción 1: Bloqueo en la misma tabla (ACTUAL)**

**Estructura:**
```sql
password_reset_tokens
  - token
  - user_id
  - is_blocked
  - blocked_at
  - blocked_reason
  - ...
```

**Ventajas:**
- [x] Simple y directo
- [x] Estado del token en un solo lugar
- [x] No requiere joins para verificar bloqueo
- [x] Mejor rendimiento

**Desventajas:**
- ❌ No hay vista centralizada de todos los bloqueos
- ❌ Dificulta ver estadísticas generales de bloqueos

---

### **Opción 2: Tabla separada `token_blocks`** 📋

**Estructura:**
```sql
password_reset_tokens
  - token
  - user_id
  - ...
  (sin campos de bloqueo)

token_blocks
  - block_id (PK)
  - token (FK)
  - user_id
  - blocked_at
  - blocked_reason
  - cooldown_until
  - failed_attempts
  - unblocked_at (nullable)
  - unblock_reason (nullable)
```

**Ventajas:**
- [x] Separación de responsabilidades
- [x] Historial de bloqueos (puede tener múltiples bloqueos por token)
- [x] Más fácil desbloquear sin afectar el token

**Desventajas:**
- Requiere JOIN para verificar bloqueo
- Más complejo de mantener
- Riesgo de inconsistencias (bloqueo en una tabla, token en otra)

---

### **Opción 3: Tabla genérica `security_blocks`**

**Estructura:**
```sql
security_blocks
  - block_id (PK)
  - block_type (ENUM: 'TOKEN', 'IP', 'EMAIL', 'USER', 'ACCOUNT')
  - entity_id (VARCHAR) -- token, IP, email, user_id
  - user_id (FK, nullable)
  - blocked_at
  - blocked_reason
  - blocked_until (nullable) -- expiración del bloqueo
  - unblocked_at (nullable)
  - unblock_reason (nullable)
  - is_active (BOOLEAN)
  - severity (VARCHAR: 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
  - metadata (JSON) -- información adicional según tipo
```

**Ventajas:**
- [x] Vista centralizada de TODOS los bloqueos
- [x] Fácil consultar: "¿Qué IPs están bloqueadas?"
- [x] Auditoría completa de bloqueos/desbloqueos
- [x] Estadísticas globales
- [x] Escalable para futuros tipos de bloqueo

**Desventajas:**
- Más complejo de implementar
- Requiere lógica para diferentes tipos de bloqueo
- JOINs más complejos

**Ejemplo de queries:**
```sql
-- Tokens bloqueados
SELECT * FROM security_blocks WHERE block_type = 'TOKEN' AND is_active = true;

-- IPs bloqueadas
SELECT * FROM security_blocks WHERE block_type = 'IP' AND is_active = true;

-- Todo bloqueado para un usuario
SELECT * FROM security_blocks WHERE user_id = 'xxx' AND is_active = true;
```

---

## Recomendación

### **Para tokens de reset de contraseña: Opción 1 (Actual)**

**Razones:**
1. El bloqueo es estado del token mismo (como `used`, `expires_at`)
2. Un token solo puede estar bloqueado una vez
3. Mejor rendimiento (no requiere JOIN)
4. Más simple de mantener

### **Para bloqueos generales del sistema: Opción 3 (Futuro)**

**Cuándo considerar Opción 3:**
- Si necesitas bloqueo permanente de IPs
- Si quieres estadísticas centralizadas de bloqueos
- Si planeas agregar más tipos de bloqueo (device, fingerprint, etc.)

---

## Implementación Híbrida (Recomendada)

**Mantener bloqueo en `password_reset_tokens`** + **Registrar en `security_logs`**:

```java
// Al bloquear un token
resetToken.setIsBlocked(true);
resetToken.setBlockedAt(LocalDateTime.now());
resetToken.setBlockedReason("MAX_FAILED_ATTEMPTS");

// Registrar en security_logs
securityAuditService.logSecurityEvent(
    resetToken.getUserId(),
    "TOKEN_BLOCKED",
    ipAddress,
    userAgent,
    "Token bloqueado: " + resetToken.getBlockedReason(),
    "HIGH"
);
```

**Ventajas:**
- [x] Bloqueo rápido en la misma tabla (sin JOIN)
- [x] Auditoría completa en security_logs
- [x] Fácil consultar bloqueos recientes desde security_logs
- [x] Lo mejor de ambos mundos

---

## Decisión Final

**Recomendación: Mantener Opción 1 (actual) + logging mejorado**

1. El bloqueo del token queda en `password_reset_tokens`
2. Cada bloqueo se registra en `security_logs` con evento `TOKEN_BLOCKED`
3. Para consultas: usar `security_logs` para auditoría
4. Para validación: usar campo `is_blocked` en `password_reset_tokens`

Si en el futuro necesitas una tabla centralizada de bloqueos, se puede agregar `security_blocks` sin afectar el sistema actual.

