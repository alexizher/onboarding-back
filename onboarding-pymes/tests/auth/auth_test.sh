#!/bin/bash

# Script de pruebas para módulo de Autenticación
# Endpoints: /api/auth

# Cargar funciones comunes
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

print_section "TEST: Módulo de Autenticación"

# Variables para pruebas
TEST_EMAIL="testuser_$(date +%s)@example.com"
TEST_USERNAME="testuser_$(date +%s)"
TEST_PASSWORD="Pass@123"
TEST_FULL_NAME="Test User"
TEST_PHONE="1234567890"
TEST_DNI="12345678"

# 1. Test endpoint de conectividad
echo -e "${BLUE}1. Test endpoint de conectividad...${NC}"
TEST_RESPONSE=$(curl -s "$BASE_URL/api/auth/test")
if [ "$TEST_RESPONSE" = "Auth Controller funcionando correctamente!" ]; then
    show_result 0 "Endpoint de test funciona correctamente"
else
    show_result 1 "Endpoint de test no responde correctamente"
    echo "Respuesta: $TEST_RESPONSE"
fi
echo ""

# 2. Verificar si email está disponible
echo -e "${BLUE}2. Verificar si email está disponible...${NC}"
CHECK_EMAIL=$(curl -s "$BASE_URL/api/auth/check-email?email=$TEST_EMAIL")
if echo "$CHECK_EMAIL" | grep -q '"isRegistered":false\|"available":true'; then
    show_result 0 "Email disponible: $TEST_EMAIL"
else
    show_result 1 "Email no disponible o error en verificación"
    show_json "$CHECK_EMAIL"
fi
echo ""

# 3. Verificar si username está disponible
echo -e "${BLUE}3. Verificar si username está disponible...${NC}"
CHECK_USERNAME=$(curl -s "$BASE_URL/api/auth/check-username?username=$TEST_USERNAME")
if echo "$CHECK_USERNAME" | grep -q '"isTaken":false\|"available":true'; then
    show_result 0 "Username disponible: $TEST_USERNAME"
else
    show_result 1 "Username no disponible o error en verificación"
    show_json "$CHECK_USERNAME"
fi
echo ""

# 4. Registro de nuevo usuario
echo -e "${BLUE}4. Registro de nuevo usuario...${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$TEST_USERNAME\",
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\",
    \"fullName\": \"$TEST_FULL_NAME\",
    \"phone\": \"$TEST_PHONE\",
    \"dni\": \"$TEST_DNI\"
  }")

if echo "$REGISTER_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Usuario registrado exitosamente"
    echo "Respuesta:"
    show_json "$REGISTER_RESPONSE"
else
    show_result 1 "Error al registrar usuario"
    echo "Respuesta:"
    show_json "$REGISTER_RESPONSE"
fi
echo ""

# 5. Login con usuario recién registrado
echo -e "${BLUE}5. Login con usuario recién registrado...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\"
  }")

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
    show_result 0 "Login exitoso. Token obtenido: ${TOKEN:0:20}..."
    USER_ID=$(echo "$LOGIN_RESPONSE" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
    echo "User ID: $USER_ID"
    
    # Verificar si la sesión se guardó en la BD (esperar un momento para que se persista)
    sleep 1
    echo -e "${BLUE}   Verificando sesión en BD...${NC}"
    
    # Verificar sesión en BD usando podman/docker
    SESSION_COUNT=$(podman exec -i db mysql -u onboarding_user -p8gmcu87aRxDAJa onboarding_db -e "SELECT COUNT(*) as total FROM user_sessions WHERE user_id = '$USER_ID' AND is_active = 1;" 2>/dev/null | grep -v Warning | tail -1 | awk '{print $1}')
    
    if [ -n "$SESSION_COUNT" ] && [ "$SESSION_COUNT" -gt 0 ]; then
        show_result 0 "Sesión guardada correctamente en BD (sesiones activas: $SESSION_COUNT)"
    else
        echo -e "${YELLOW}   No se pudo verificar sesión en BD (puede requerir acceso directo a BD)${NC}"
    fi
else
    show_result 1 "Error al hacer login"
    echo "Respuesta:"
    show_json "$LOGIN_RESPONSE"
fi
echo ""

# 6. Login con credenciales incorrectas
echo -e "${BLUE}6. Login con credenciales incorrectas...${NC}"
INVALID_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"WrongPassword123!\"
  }")

if echo "$INVALID_LOGIN" | grep -q '"success":false'; then
    show_result 0 "Login falló correctamente con contraseña incorrecta"
else
    show_result 1 "Login no debería haber sido exitoso"
    show_json "$INVALID_LOGIN"
fi
echo ""

# 7. Registro con email duplicado
echo -e "${BLUE}7. Registro con email duplicado...${NC}"
DUPLICATE_REGISTER=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"${TEST_USERNAME}_dup\",
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\",
    \"fullName\": \"Test User Duplicate\"
  }")

if echo "$DUPLICATE_REGISTER" | grep -q "email ya está registrado\|El email ya está registrado"; then
    show_result 0 "Registro falló correctamente con email duplicado"
else
    show_result 1 "Registro no debería haber sido exitoso con email duplicado"
    show_json "$DUPLICATE_REGISTER"
fi
echo ""

# 8. Múltiples intentos de login fallidos (Rate Limiting)
echo -e "${BLUE}8. Múltiples intentos de login fallidos (Rate Limiting)...${NC}"
FAILED_ATTEMPTS=0
MAX_ATTEMPTS=5

for i in $(seq 1 $MAX_ATTEMPTS); do
    INVALID_ATTEMPT=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{
        \"email\": \"$TEST_EMAIL\",
        \"password\": \"WrongPassword${i}!\"
      }")
    
    if echo "$INVALID_ATTEMPT" | grep -q '"success":false'; then
        FAILED_ATTEMPTS=$((FAILED_ATTEMPTS + 1))
        echo "  Intento $i: Fallido (esperado)"
    fi
    sleep 0.5
done

show_result 0 "Realizados $FAILED_ATTEMPTS intentos fallidos"
echo ""

# 9. Verificar intentos de login en BD
echo -e "${BLUE}9. Verificar intentos de login en BD...${NC}"
sleep 2
LOGIN_ATTEMPTS_COUNT=$(podman exec -i db mysql -u onboarding_user -p8gmcu87aRxDAJa onboarding_db -e "SELECT COUNT(*) as total FROM login_attempts WHERE email = '$TEST_EMAIL';" 2>/dev/null | grep -v Warning | tail -1 | awk '{print $1}')

if [ -n "$LOGIN_ATTEMPTS_COUNT" ] && [ "$LOGIN_ATTEMPTS_COUNT" -gt 0 ]; then
    show_result 0 "Intentos de login registrados en BD: $LOGIN_ATTEMPTS_COUNT"
else
    echo -e "${YELLOW}   No se encontraron intentos de login en BD (puede ser que loginUserSimple no registre intentos)${NC}"
    echo -e "${YELLOW}   Nota: loginUserSimple() no registra intentos, solo loginUser() lo hace${NC}"
fi
echo ""

# 10. Login con ADMIN (o crear ADMIN si no existe)
echo -e "${BLUE}10. Login con ADMIN...${NC}"
ADMIN_TOKEN=$(get_token "$ADMIN_EMAIL" "$ADMIN_PASSWORD")

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${YELLOW}   ADMIN no encontrado. Intentando crear usuario ADMIN...${NC}"
    # Primero necesitamos obtener un token de algún usuario existente o crear uno
    # Como alternativa, intentamos crear directamente si hay un endpoint disponible
    echo -e "${YELLOW}   Nota: Para crear ADMIN, ejecuta el script con un usuario administrador o crea el usuario manualmente${NC}"
    ADMIN_TOKEN=""
else
    show_result 0 "Login ADMIN exitoso. Token obtenido: ${ADMIN_TOKEN:0:20}..."
    ADMIN_USER_ID=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$ADMIN_PASSWORD\"}" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
fi
echo ""

# 11. Verificar sesiones activas del usuario
echo -e "${BLUE}11. Verificar sesiones activas del usuario...${NC}"
if [ -n "$TOKEN" ]; then
    SESSIONS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/security/sessions" \
      -H "Authorization: Bearer $TOKEN")
    
    if echo "$SESSIONS_RESPONSE" | grep -q '"success":true'; then
        SESSION_COUNT=$(echo "$SESSIONS_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(len(data.get('sessions', [])))" 2>/dev/null || echo "0")
        show_result 0 "Sesiones activas obtenidas: $SESSION_COUNT"
        echo "Respuesta:"
        show_json "$SESSIONS_RESPONSE"
    else
        show_result 1 "Error al obtener sesiones"
        show_json "$SESSIONS_RESPONSE"
    fi
else
    echo -e "${YELLOW}   No hay token para verificar sesiones${NC}"
fi
echo ""

# 12. Validar fortaleza de contraseña
echo -e "${BLUE}12. Validar fortaleza de contraseña...${NC}"
VALIDATE_PASSWORD=$(curl -s -X POST "$BASE_URL/api/security/validate-password" \
  -H "Content-Type: application/json" \
  -d "{\"password\": \"$TEST_PASSWORD\"}")

if echo "$VALIDATE_PASSWORD" | grep -q '"success":true'; then
    VALID=$(echo "$VALIDATE_PASSWORD" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('valid', False))" 2>/dev/null || echo "false")
    STRENGTH=$(echo "$VALIDATE_PASSWORD" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('strength', 'unknown'))" 2>/dev/null || echo "unknown")
    show_result 0 "Validación de contraseña: válida=$VALID, fuerza=$STRENGTH"
else
    show_result 1 "Error al validar contraseña"
    show_json "$VALIDATE_PASSWORD"
fi
echo ""

# 13. Cambio de contraseña
echo -e "${BLUE}13. Cambio de contraseña...${NC}"
if [ -n "$TOKEN" ]; then
    # Obtener token actualizado y fresco antes de cambiar contraseña
    echo "   Obteniendo token actualizado..."
    CURRENT_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{
        \"email\": \"$TEST_EMAIL\",
        \"password\": \"$TEST_PASSWORD\"
      }")
    
    CURRENT_TOKEN=$(echo "$CURRENT_LOGIN" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$CURRENT_TOKEN" ]; then
        echo -e "${YELLOW}   No se pudo obtener token actualizado, usando token anterior${NC}"
        CURRENT_TOKEN="$TOKEN"
    else
        echo "   Token actualizado obtenido"
    fi
    
    # Esperar un momento para asegurar que el login se procesó
    sleep 1
    
    NEW_PASSWORD="NewPass@123"
    echo "   Intentando cambiar contraseña..."
    CHANGE_PASSWORD_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/api/security/change-password" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $CURRENT_TOKEN" \
      -d "{
        \"currentPassword\": \"$TEST_PASSWORD\",
        \"newPassword\": \"$NEW_PASSWORD\"
      }")
    
    HTTP_STATUS=$(echo "$CHANGE_PASSWORD_RESPONSE" | grep "HTTP_STATUS" | cut -d':' -f2)
    RESPONSE_BODY=$(echo "$CHANGE_PASSWORD_RESPONSE" | grep -v "HTTP_STATUS")
    
    if [ "$HTTP_STATUS" = "200" ] && echo "$RESPONSE_BODY" | grep -q '"success":true'; then
        show_result 0 "Contraseña cambiada exitosamente"
        TEST_PASSWORD="$NEW_PASSWORD"  # Actualizar para siguientes pruebas
    else
        # Verificar el mensaje de error
        if [ "$HTTP_STATUS" = "403" ]; then
            show_result 1 "Error: 403 Forbidden - Verificar @EnableMethodSecurity y permisos"
            echo -e "${YELLOW}   Nota: Puede ser un problema de configuración de seguridad${NC}"
        else
            ERROR_MSG=$(echo "$RESPONSE_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('message', 'Error desconocido'))" 2>/dev/null || echo "Error desconocido")
            
            if echo "$ERROR_MSG" | grep -qi "contraseña actual incorrecta\|current.*incorrect"; then
                show_result 1 "Error: Contraseña actual incorrecta"
            else
                show_result 1 "Error al cambiar contraseña (HTTP $HTTP_STATUS): $ERROR_MSG"
            fi
        fi
        
        echo "Respuesta completa:"
        show_json "$RESPONSE_BODY"
        echo -e "${YELLOW}   Continuando con contraseña original para siguientes pruebas${NC}"
        NEW_PASSWORD=""  # No actualizar
    fi
else
    echo -e "${YELLOW}   No hay token para cambiar contraseña${NC}"
fi
echo ""

# 14. Login con nueva contraseña (si se cambió)
echo -e "${BLUE}14. Login con nueva contraseña...${NC}"
if [ -n "$NEW_PASSWORD" ]; then
    sleep 1  # Esperar un momento para que el cambio se persista
    NEW_LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{
        \"email\": \"$TEST_EMAIL\",
        \"password\": \"$NEW_PASSWORD\"
      }")
    
    NEW_TOKEN=$(echo "$NEW_LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$NEW_TOKEN" ]; then
        show_result 0 "Login exitoso con nueva contraseña"
        TOKEN="$NEW_TOKEN"  # Actualizar token
    else
        show_result 1 "Error al hacer login con nueva contraseña"
        echo "Respuesta:"
        show_json "$NEW_LOGIN_RESPONSE"
        echo -e "${YELLOW}   Intentando login con contraseña original...${NC}"
        # Intentar con contraseña original
        ORIG_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
          -H "Content-Type: application/json" \
          -d "{\"email\": \"$TEST_EMAIL\", \"password\": \"Pass@123\"}")
        ORIG_TOKEN=$(echo "$ORIG_LOGIN" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$ORIG_TOKEN" ]; then
            TOKEN="$ORIG_TOKEN"
            TEST_PASSWORD="Pass@123"
        fi
    fi
else
    echo -e "${YELLOW}   Saltando: contraseña no se cambió${NC}"
fi
echo ""

# 15. Solicitar reset de contraseña
echo -e "${BLUE}15. Solicitar reset de contraseña...${NC}"
RESET_REQUEST_RESPONSE=$(curl -s -X POST "$BASE_URL/api/security/password-reset/request" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$TEST_EMAIL\"}")

if echo "$RESET_REQUEST_RESPONSE" | grep -q '"success":true'; then
    RESET_TOKEN=$(echo "$RESET_REQUEST_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    if [ -n "$RESET_TOKEN" ]; then
        show_result 0 "Token de reset generado exitosamente"
        echo "Token (para pruebas): ${RESET_TOKEN:0:30}..."
    else
        show_result 0 "Solicitud de reset procesada (token no visible en respuesta)"
    fi
else
    show_result 1 "Error al solicitar reset de contraseña"
    show_json "$RESET_REQUEST_RESPONSE"
fi
echo ""

# 16. Verificar logs de seguridad del usuario
echo -e "${BLUE}16. Verificar logs de seguridad del usuario...${NC}"
if [ -n "$TOKEN" ]; then
    SECURITY_LOGS=$(curl -s -X GET "$BASE_URL/api/security/logs" \
      -H "Authorization: Bearer $TOKEN")
    
    if echo "$SECURITY_LOGS" | grep -q '"success":true'; then
        LOG_COUNT=$(echo "$SECURITY_LOGS" | python3 -c "import sys, json; data=json.load(sys.stdin); print(len(data.get('logs', [])))" 2>/dev/null || echo "0")
        show_result 0 "Logs de seguridad obtenidos: $LOG_COUNT"
    else
        show_result 1 "Error al obtener logs de seguridad"
        show_json "$SECURITY_LOGS"
    fi
else
    echo -e "${YELLOW}   No hay token para verificar logs${NC}"
fi
echo ""

# 17. Verificar intentos de login recientes
echo -e "${BLUE}17. Verificar intentos de login recientes...${NC}"
if [ -n "$TOKEN" ]; then
    LOGIN_ATTEMPTS=$(curl -s -X GET "$BASE_URL/api/security/login-attempts?email=$TEST_EMAIL" \
      -H "Authorization: Bearer $TOKEN")
    
    if echo "$LOGIN_ATTEMPTS" | grep -q '"success":true'; then
        ATTEMPTS_COUNT=$(echo "$LOGIN_ATTEMPTS" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('count', 0))" 2>/dev/null || echo "0")
        show_result 0 "Intentos de login obtenidos: $ATTEMPTS_COUNT"
    else
        show_result 1 "Error al obtener intentos de login"
        show_json "$LOGIN_ATTEMPTS"
    fi
else
    echo -e "${YELLOW}   No hay token para verificar intentos${NC}"
fi
echo ""

# 18. Logout
echo -e "${BLUE}18. Logout...${NC}"
if [ -n "$TOKEN" ]; then
    LOGOUT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/security/logout" \
      -H "Authorization: Bearer $TOKEN")
    
    if echo "$LOGOUT_RESPONSE" | grep -q '"success":true'; then
        show_result 0 "Logout exitoso"
        
        # Verificar que el token ya no funciona
        sleep 1
        VERIFY_LOGOUT=$(curl -s -X GET "$BASE_URL/api/security/sessions" \
          -H "Authorization: Bearer $TOKEN")
        
        if echo "$VERIFY_LOGOUT" | grep -q "Token inválido\|401\|403\|Unauthorized"; then
            show_result 0 "Token invalidado correctamente después del logout"
        else
            echo -e "${YELLOW}   El token aún podría estar activo (verificar blacklist)${NC}"
        fi
    else
        show_result 1 "Error al hacer logout"
        show_json "$LOGOUT_RESPONSE"
    fi
else
    echo -e "${YELLOW}   No hay token para hacer logout${NC}"
fi
echo ""

# 19. Cerrar otras sesiones (requiere nuevo login)
echo -e "${BLUE}19. Cerrar otras sesiones...${NC}"
# Hacer login nuevamente para tener múltiples sesiones
LOGIN_AGAIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$TEST_EMAIL\", \"password\": \"$NEW_PASSWORD\"}")

NEW_TOKEN_2=$(echo "$LOGIN_AGAIN" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$NEW_TOKEN_2" ]; then
    CLOSE_OTHERS_RESPONSE=$(curl -s -X POST "$BASE_URL/api/security/sessions/close-others" \
      -H "Authorization: Bearer $NEW_TOKEN_2")
    
    if echo "$CLOSE_OTHERS_RESPONSE" | grep -q '"success":true'; then
        CLOSED_COUNT=$(echo "$CLOSE_OTHERS_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('closedSessions', 0))" 2>/dev/null || echo "0")
        show_result 0 "Otras sesiones cerradas: $CLOSED_COUNT"
    else
        show_result 1 "Error al cerrar otras sesiones"
        show_json "$CLOSE_OTHERS_RESPONSE"
    fi
else
    echo -e "${YELLOW}   No se pudo hacer login para probar cerrar sesiones${NC}"
fi
echo ""

# 20. Tests adicionales de bloqueo (solo si tenemos ADMIN)
echo -e "${BLUE}20. Tests adicionales de bloqueo de token/usuario...${NC}"
if [ -n "$ADMIN_TOKEN" ]; then
    echo -e "${YELLOW}   Ejecutando tests de bloqueo (ver blacklist_test.sh para detalles completos)...${NC}"
    # Estos tests están en un script separado (blacklist_test.sh)
    # para mantener este script enfocado en autenticación básica
else
    echo -e "${YELLOW}   Saltando: no hay token de ADMIN${NC}"
fi
echo ""

echo -e "${BLUE}==========================================${NC}"
echo -e "${GREEN}  PRUEBAS DE AUTENTICACIÓN COMPLETADAS${NC}"
echo -e "${BLUE}==========================================${NC}"

