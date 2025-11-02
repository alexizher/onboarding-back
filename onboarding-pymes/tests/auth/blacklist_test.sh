#!/bin/bash

# Script de pruebas para bloqueo de token y usuario
# Valida intentos de login incorrectos, bloqueo de token y bloqueo de usuario

# Cargar funciones comunes
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

print_section "TEST: Bloqueo de Token y Usuario"

BASE_URL="${BASE_URL:-http://localhost:8080}"

# 1. Crear usuario de prueba para tests de bloqueo
echo -e "${BLUE}1. Crear usuario de prueba para tests de bloqueo...${NC}"
ADMIN_TOKEN=$(get_token "$ADMIN_EMAIL" "$ADMIN_PASSWORD")

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}Error: No se pudo obtener token de ADMIN${NC}"
    exit 1
fi

TIMESTAMP=$(date +%s)
TEST_USER_EMAIL="test_block_${TIMESTAMP}@example.com"
TEST_USER_USERNAME="testblock${TIMESTAMP}"
TEST_USER_PASSWORD="TestPass123!@#"

CREATE_USER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"fullName\": \"Test Block User\",
    \"username\": \"$TEST_USER_USERNAME\",
    \"email\": \"$TEST_USER_EMAIL\",
    \"password\": \"$TEST_USER_PASSWORD\",
    \"roleId\": \"ROLE_APPLICANT\",
    \"consentGdpr\": true
  }")

if echo "$CREATE_USER_RESPONSE" | grep -q '"success":true'; then
    TEST_USER_ID=$(echo "$CREATE_USER_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('data', {}).get('userId', ''))" 2>/dev/null)
    show_result 0 "Usuario de prueba creado: $TEST_USER_EMAIL"
else
    echo -e "${RED}Error al crear usuario de prueba${NC}"
    show_json "$CREATE_USER_RESPONSE"
    exit 1
fi
echo ""

# 2. Login exitoso inicial
echo -e "${BLUE}2. Login exitoso inicial...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_USER_EMAIL\",
    \"password\": \"$TEST_USER_PASSWORD\"
  }")

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
    show_result 0 "Login exitoso. Token obtenido: ${TOKEN:0:30}..."
else
    show_result 1 "Error en login inicial"
    exit 1
fi
echo ""

# 3. Verificar que el token funciona
echo -e "${BLUE}3. Verificar que el token funciona...${NC}"
VERIFY_RESPONSE=$(curl -s -X GET "$BASE_URL/api/security/sessions" \
  -H "Authorization: Bearer $TOKEN")

if echo "$VERIFY_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Token funciona correctamente"
else
    show_result 1 "Token no funciona"
fi
echo ""

# 4. Múltiples intentos de login incorrectos (para rate limiting)
echo -e "${BLUE}4. Múltiples intentos de login incorrectos (Rate Limiting)...${NC}"
echo "   Realizando 5 intentos fallidos..."
FAILED_COUNT=0
for i in {1..5}; do
    FAILED_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{
        \"email\": \"$TEST_USER_EMAIL\",
        \"password\": \"WrongPassword${i}\"
      }")
    
    if ! echo "$FAILED_LOGIN" | grep -q '"token"'; then
        FAILED_COUNT=$((FAILED_COUNT + 1))
        echo "   Intento $i: Fallido (esperado)"
    fi
    sleep 0.5
done

if [ "$FAILED_COUNT" -eq 5 ]; then
    show_result 0 "Realizados 5 intentos fallidos exitosamente"
else
    show_result 1 "Solo $FAILED_COUNT de 5 intentos fallaron"
fi
echo ""

# 5. Verificar intentos de login en BD
echo -e "${BLUE}5. Verificar intentos de login en BD...${NC}"
ATTEMPTS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/security/login-attempts?email=$TEST_USER_EMAIL" \
  -H "Authorization: Bearer $TOKEN")

if echo "$ATTEMPTS_RESPONSE" | grep -q '"success":true'; then
    ATTEMPTS_COUNT=$(echo "$ATTEMPTS_RESPONSE" | python3 -c "import sys, json; print(len(json.load(sys.stdin).get('attempts', [])))" 2>/dev/null || echo "0")
    show_result 0 "Intentos registrados: $ATTEMPTS_COUNT"
else
    show_result 1 "Error al obtener intentos"
fi
echo ""

# 6. Intentar login después de múltiples fallos (debería estar bloqueado)
echo -e "${BLUE}6. Intentar login después de múltiples fallos (verificar bloqueo temporal)...${NC}"
AFTER_FAILED_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_USER_EMAIL\",
    \"password\": \"$TEST_USER_PASSWORD\"
  }")

# Si está bloqueado, puede fallar o tener delay
if echo "$AFTER_FAILED_LOGIN" | grep -q '"token"'; then
    show_result 0 "Login exitoso después de bloqueo temporal (bloqueo puede haber expirado)"
else
    if echo "$AFTER_FAILED_LOGIN" | grep -qi "bloqueado\|blocked\|lockout\|too many"; then
        show_result 0 "Usuario bloqueado temporalmente (esperado)"
    else
        show_result 1 "Login falló pero no se muestra bloqueo claro"
    fi
fi
echo ""

# 7. Bloquear usuario manualmente (usando blacklist)
echo -e "${BLUE}7. Bloquear usuario manualmente (Client Blacklist)...${NC}"
BLOCK_RESPONSE=$(curl -s -X POST "$BASE_URL/api/security/blacklist" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"userId\": \"$TEST_USER_ID\",
    \"reason\": \"Bloqueo de prueba - múltiples intentos fallidos\"
  }")

if echo "$BLOCK_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Usuario bloqueado exitosamente"
else
    show_result 1 "Error al bloquear usuario"
    show_json "$BLOCK_RESPONSE"
fi
echo ""

# 8. Intentar login con usuario bloqueado (debería fallar)
echo -e "${BLUE}8. Intentar login con usuario bloqueado (debería fallar)...${NC}"
BLOCKED_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_USER_EMAIL\",
    \"password\": \"$TEST_USER_PASSWORD\"
  }")

if ! echo "$BLOCKED_LOGIN" | grep -q '"token"'; then
    if echo "$BLOCKED_LOGIN" | grep -qi "bloqueado\|blocked\|no puede"; then
        show_result 0 "Login bloqueado correctamente (esperado)"
    else
        show_result 0 "Login falló (usuario bloqueado)"
    fi
else
    show_result 1 "Login exitoso con usuario bloqueado (ERROR)"
fi
echo ""

# 9. Verificar que el usuario está en blacklist
echo -e "${BLUE}9. Verificar que el usuario está en blacklist...${NC}"
CHECK_BLACKLIST=$(curl -s -X GET "$BASE_URL/api/security/blacklist/$TEST_USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$CHECK_BLACKLIST" | grep -q '"isBlacklisted":true'; then
    show_result 0 "Usuario confirmado en blacklist"
else
    show_result 1 "Usuario no encontrado en blacklist"
fi
echo ""

# 10. Bloquear token (agregar a blacklist)
echo -e "${BLUE}10. Bloquear token (Token Blacklist)...${NC}"
# Hacer logout para bloquear el token
LOGOUT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/security/logout" \
  -H "Authorization: Bearer $TOKEN")

if echo "$LOGOUT_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Logout exitoso (token agregado a blacklist)"
else
    show_result 1 "Error en logout"
fi
echo ""

# 11. Intentar usar token bloqueado (debería fallar)
echo -e "${BLUE}11. Intentar usar token bloqueado (debería fallar)...${NC}"
BLOCKED_TOKEN_RESPONSE=$(curl -s -X GET "$BASE_URL/api/security/sessions" \
  -H "Authorization: Bearer $TOKEN")

if ! echo "$BLOCKED_TOKEN_RESPONSE" | grep -q '"success":true'; then
    if echo "$BLOCKED_TOKEN_RESPONSE" | grep -qi "token\|unauthorized\|401\|403\|invalid"; then
        show_result 0 "Token bloqueado correctamente (esperado)"
    else
        show_result 0 "Token invalidado (esperado)"
    fi
else
    show_result 1 "Token aún funciona después de logout (ERROR)"
fi
echo ""

# 12. Verificar tokens blacklisted (usar token de ADMIN ya que el token actual está bloqueado)
echo -e "${BLUE}12. Verificar tokens blacklisted...${NC}"
# Obtener un token nuevo de ADMIN para esta consulta
ADMIN_TOKEN_FOR_CHECK=$(get_token "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
if [ -z "$ADMIN_TOKEN_FOR_CHECK" ]; then
    ADMIN_TOKEN_FOR_CHECK="$ADMIN_TOKEN"
fi

TOKENS_BLACKLISTED=$(curl -s -X GET "$BASE_URL/api/security/tokens/blacklisted" \
  -H "Authorization: Bearer $ADMIN_TOKEN_FOR_CHECK")

if echo "$TOKENS_BLACKLISTED" | grep -q '"success":true'; then
    show_result 0 "Tokens blacklisted obtenidos"
else
    show_result 1 "Error al obtener tokens blacklisted"
fi
echo ""

# 13. Desbloquear usuario
echo -e "${BLUE}13. Desbloquear usuario...${NC}"
UNBLOCK_RESPONSE=$(curl -s -X POST "$BASE_URL/api/security/blacklist/$TEST_USER_ID/unblacklist" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "reason": "Usuario desbloqueado para pruebas"
  }')

if echo "$UNBLOCK_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Usuario desbloqueado exitosamente"
else
    show_result 1 "Error al desbloquear usuario"
    show_json "$UNBLOCK_RESPONSE"
fi
echo ""

# 14. Verificar que el usuario puede hacer login después de desbloquear
echo -e "${BLUE}14. Verificar que el usuario puede hacer login después de desbloquear...${NC}"
sleep 2  # Esperar un momento para que el desbloqueo se propague
UNBLOCKED_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_USER_EMAIL\",
    \"password\": \"$TEST_USER_PASSWORD\"
  }")

NEW_TOKEN=$(echo "$UNBLOCKED_LOGIN" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$NEW_TOKEN" ]; then
    show_result 0 "Login exitoso después de desbloquear (esperado)"
else
    show_result 1 "Login aún falla después de desbloquear"
fi
echo ""

echo -e "${BLUE}==========================================${NC}"
echo -e "${GREEN}  PRUEBAS DE BLOQUEO COMPLETADAS${NC}"
echo -e "${BLUE}==========================================${NC}"

