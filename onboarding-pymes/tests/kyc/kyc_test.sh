#!/bin/bash

# Script de pruebas para módulo de KYC
# Endpoints: /api/kyc

# Cargar funciones comunes
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

print_section "TEST: Módulo de KYC"

# Obtener tokens
echo -e "${BLUE}Obteniendo tokens...${NC}"
ADMIN_TOKEN=$(get_token "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
ANALYST_TOKEN=$(ensure_user "analyst_kyc_$(date +%s)@example.com" "Pass@123" "$ADMIN_TOKEN" "Test Analyst KYC" "analystkyc" "ROLE_ANALYST")

if [ -z "$ADMIN_TOKEN" ] || [ -z "$ANALYST_TOKEN" ]; then
    echo -e "${RED}Error: No se pudieron obtener los tokens necesarios${NC}"
    exit 1
fi

show_result 0 "Tokens obtenidos"
echo ""

# Crear usuario APPLICANT y solicitud de crédito
echo -e "${BLUE}Creando usuario APPLICANT y solicitud de crédito...${NC}"
APPLICANT_EMAIL="applicant_kyc_$(date +%s)@example.com"
APPLICANT_TOKEN=$(ensure_user "$APPLICANT_EMAIL" "Pass@123" "$ADMIN_TOKEN" "Test Applicant KYC" "applicantkyc" "ROLE_APPLICANT")

APPLICANT_ID=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$APPLICANT_EMAIL\", \"password\": \"Pass@123\"}" | \
  grep -o '"userId":"[^"]*"' | cut -d'"' -f4)

# Crear solicitud de crédito
APPLICATION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/draft" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{
    "companyName": "Empresa KYC Test",
    "cuit": "20-11111111-1",
    "amountRequested": 50000.00,
    "acceptTerms": true
  }')

APPLICATION_ID=$(echo "$APPLICATION_RESPONSE" | grep -o '"applicationId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$APPLICATION_ID" ]; then
    echo -e "${RED}Error: No se pudo crear solicitud de crédito${NC}"
    exit 1
fi

show_result 0 "Solicitud creada: $APPLICATION_ID"
echo ""

# 1. Iniciar verificación KYC
echo -e "${BLUE}1. Iniciar verificación KYC...${NC}"
KYC_VERIFY_RESPONSE=$(curl -s -X POST "$BASE_URL/api/kyc/verify" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -d "{
    \"applicationId\": \"$APPLICATION_ID\",
    \"verificationType\": \"IDENTITY\",
    \"provider\": \"MOCK\"
  }")

KYC_ID=$(echo "$KYC_VERIFY_RESPONSE" | grep -o '"verificationId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$KYC_ID" ]; then
    show_result 0 "Verificación KYC iniciada con ID: $KYC_ID"
    echo "Respuesta:"
    show_json "$KYC_VERIFY_RESPONSE"
else
    show_result 1 "Error al iniciar verificación KYC"
    show_json "$KYC_VERIFY_RESPONSE"
fi
echo ""

# 2. Obtener verificaciones por aplicación
echo -e "${BLUE}2. Obtener verificaciones por aplicación...${NC}"
KYC_LIST=$(curl -s -X GET "$BASE_URL/api/kyc/application/$APPLICATION_ID" \
  -H "Authorization: Bearer $ANALYST_TOKEN")

if echo "$KYC_LIST" | grep -q '"success":true'; then
    show_result 0 "Verificaciones obtenidas exitosamente"
    KYC_COUNT=$(echo "$KYC_LIST" | grep -o '"verificationId":"[^"]*"' | wc -l)
    echo "Verificaciones encontradas: $KYC_COUNT"
else
    show_result 1 "Error al obtener verificaciones"
    show_json "$KYC_LIST"
fi
echo ""

# 3. Obtener verificaciones por usuario
echo -e "${BLUE}3. Obtener verificaciones por usuario...${NC}"
KYC_USER=$(curl -s -X GET "$BASE_URL/api/kyc/user/$APPLICANT_ID" \
  -H "Authorization: Bearer $ANALYST_TOKEN")

if echo "$KYC_USER" | grep -q '"success":true'; then
    show_result 0 "Verificaciones por usuario obtenidas exitosamente"
else
    show_result 1 "Error al obtener verificaciones por usuario"
    show_json "$KYC_USER"
fi
echo ""

# 4. Obtener verificación más reciente
if [ -n "$APPLICATION_ID" ]; then
    echo -e "${BLUE}4. Obtener verificación más reciente...${NC}"
    KYC_LATEST=$(curl -s -X GET "$BASE_URL/api/kyc/application/$APPLICATION_ID/latest?verificationType=IDENTITY" \
      -H "Authorization: Bearer $ANALYST_TOKEN")

    if echo "$KYC_LATEST" | grep -q '"success":true'; then
        show_result 0 "Verificación más reciente obtenida exitosamente"
    else
        show_result 1 "Error al obtener verificación más reciente"
        show_json "$KYC_LATEST"
    fi
    echo ""
fi

# 5. Obtener estadísticas KYC
echo -e "${BLUE}5. Obtener estadísticas KYC...${NC}"
KYC_STATS=$(curl -s -X GET "$BASE_URL/api/kyc/statistics" \
  -H "Authorization: Bearer $ANALYST_TOKEN")

if echo "$KYC_STATS" | grep -q '"success":true'; then
    show_result 0 "Estadísticas KYC obtenidas exitosamente"
    echo "Respuesta:"
    show_json "$KYC_STATS"
else
    show_result 1 "Error al obtener estadísticas"
    show_json "$KYC_STATS"
fi
echo ""

echo -e "${BLUE}==========================================${NC}"
echo -e "${GREEN}  PRUEBAS DE KYC COMPLETADAS${NC}"
echo -e "${BLUE}==========================================${NC}"

