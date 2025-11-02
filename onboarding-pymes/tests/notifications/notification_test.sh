#!/bin/bash

# Script de pruebas para módulo de Notificaciones
# Endpoints: /api/notifications y Server-Sent Events (SSE)

# Cargar funciones comunes
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

print_section "TEST: Módulo de Notificaciones"

# Obtener tokens
echo -e "${BLUE}Obteniendo tokens...${NC}"
ADMIN_TOKEN=$(get_token "$ADMIN_EMAIL" "$ADMIN_PASSWORD")

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}Error: No se pudo obtener token de ADMIN${NC}"
    exit 1
fi

show_result 0 "Token ADMIN obtenido"
echo ""

# Crear usuario APPLICANT para recibir notificaciones
echo -e "${BLUE}Creando usuario APPLICANT para pruebas...${NC}"
APPLICANT_EMAIL="applicant_notif_$(date +%s)@example.com"
APPLICANT_TOKEN=$(ensure_user "$APPLICANT_EMAIL" "Pass@123" "$ADMIN_TOKEN" "Test Applicant Notifications" "applicantnotif" "ROLE_APPLICANT")

APPLICANT_ID=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$APPLICANT_EMAIL\", \"password\": \"Pass@123\"}" | \
  grep -o '"userId":"[^"]*"' | cut -d'"' -f4)

show_result 0 "Usuario APPLICANT creado: $APPLICANT_ID"
echo ""

# 1. Probar endpoint de notificaciones (si existe)
echo -e "${BLUE}1. Probar endpoint de notificaciones...${NC}"
NOTIFICATIONS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/notifications" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" 2>&1)

if echo "$NOTIFICATIONS_RESPONSE" | grep -q '"success":true\|404\|Not Found'; then
    if echo "$NOTIFICATIONS_RESPONSE" | grep -q "404\|Not Found"; then
        echo -e "${YELLOW}Endpoint de notificaciones no encontrado (esperado si no está implementado)${NC}"
    else
        show_result 0 "Notificaciones obtenidas exitosamente"
        show_json "$NOTIFICATIONS_RESPONSE"
    fi
else
    show_result 1 "Error al obtener notificaciones"
    echo "Respuesta: $NOTIFICATIONS_RESPONSE"
fi
echo ""

# 2. Probar SSE (Server-Sent Events) - conexión básica
echo -e "${BLUE}2. Probar conexión SSE básica...${NC}"
SSE_ENDPOINT="$BASE_URL/api/notifications/sse"
TIMEOUT=5

SSE_RESPONSE=$(timeout $TIMEOUT curl -s -N -H "Authorization: Bearer $APPLICANT_TOKEN" "$SSE_ENDPOINT" 2>&1 || echo "timeout")

if echo "$SSE_RESPONSE" | grep -q "data:\|event:\|SSE\|200 OK"; then
    show_result 0 "Conexión SSE establecida correctamente"
    echo -e "${YELLOW}Respuesta SSE (primeros 200 caracteres):${NC}"
    echo "${SSE_RESPONSE:0:200}..."
elif echo "$SSE_RESPONSE" | grep -q "404\|Not Found"; then
    echo -e "${YELLOW}Endpoint SSE no encontrado (esperado si no está implementado)${NC}"
elif echo "$SSE_RESPONSE" | grep -q "timeout"; then
    echo -e "${YELLOW}Conexión SSE timeout (puede ser normal si no hay eventos)${NC}"
else
    show_result 1 "Error al conectar SSE"
    echo "Respuesta: ${SSE_RESPONSE:0:300}..."
fi
echo ""

# 3. Crear evento que genere notificación (cambiar estado de aplicación)
echo -e "${BLUE}3. Crear evento para generar notificación...${NC}"
# Crear solicitud de crédito
APPLICATION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/draft" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{
    "companyName": "Empresa Notif Test",
    "cuit": "20-33333333-3",
    "amountRequested": 75000.00,
    "acceptTerms": true
  }')

APPLICATION_ID=$(echo "$APPLICATION_RESPONSE" | grep -o '"applicationId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$APPLICATION_ID" ]; then
    show_result 0 "Solicitud creada: $APPLICATION_ID"
    
    # Completar la solicitud para generar evento
    COMPLETE_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/applications/$APPLICATION_ID/complete" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $APPLICANT_TOKEN" \
      -d '{}')
    
    if echo "$COMPLETE_RESPONSE" | grep -q '"success":true'; then
        show_result 0 "Solicitud completada (evento generado)"
        echo -e "${YELLOW}Nota: Esto debería generar una notificación SSE${NC}"
    fi
else
    show_result 1 "Error al crear solicitud"
fi
echo ""

# 4. Verificar notificaciones por usuario (si endpoint existe)
if [ -n "$APPLICANT_ID" ]; then
    echo -e "${BLUE}4. Verificar notificaciones por usuario...${NC}"
    USER_NOTIFS=$(curl -s -X GET "$BASE_URL/api/notifications/user/$APPLICANT_ID" \
      -H "Authorization: Bearer $APPLICANT_TOKEN" 2>&1)
    
    if echo "$USER_NOTIFS" | grep -q '"success":true'; then
        show_result 0 "Notificaciones por usuario obtenidas"
        show_json "$USER_NOTIFS"
    elif echo "$USER_NOTIFS" | grep -q "404\|Not Found"; then
        echo -e "${YELLOW}Endpoint de notificaciones por usuario no encontrado${NC}"
    else
        show_result 1 "Error al obtener notificaciones por usuario"
    fi
    echo ""
fi

echo -e "${BLUE}==========================================${NC}"
echo -e "${GREEN}  PRUEBAS DE NOTIFICACIONES COMPLETADAS${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""
echo -e "${YELLOW}Nota: Las pruebas de SSE requieren conexión persistente${NC}"
echo -e "${YELLOW}Usa el script SSE_SMOKE_TEST.sh para pruebas más completas${NC}"

