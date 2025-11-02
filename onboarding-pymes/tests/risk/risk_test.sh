#!/bin/bash

# Script de pruebas para módulo de Evaluación de Riesgo
# Endpoints: /api/risk

# Cargar funciones comunes
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

print_section "TEST: Módulo de Evaluación de Riesgo"

# Obtener tokens
echo -e "${BLUE}Obteniendo tokens...${NC}"
ADMIN_TOKEN=$(get_token "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
ANALYST_TOKEN=$(ensure_user "analyst_risk_$(date +%s)@example.com" "Pass@123" "$ADMIN_TOKEN" "Test Analyst Risk" "analystrisk" "ROLE_ANALYST")

if [ -z "$ADMIN_TOKEN" ] || [ -z "$ANALYST_TOKEN" ]; then
    echo -e "${RED}Error: No se pudieron obtener los tokens necesarios${NC}"
    exit 1
fi

show_result 0 "Tokens obtenidos"
echo ""

# Crear usuario APPLICANT y solicitud de crédito
echo -e "${BLUE}Creando usuario APPLICANT y solicitud de crédito...${NC}"
APPLICANT_EMAIL="applicant_risk_$(date +%s)@example.com"
APPLICANT_TOKEN=$(ensure_user "$APPLICANT_EMAIL" "Pass@123" "$ADMIN_TOKEN" "Test Applicant Risk" "applicantrisk" "ROLE_APPLICANT")

# Crear solicitud de crédito completa
APPLICATION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/draft" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{
    "companyName": "Empresa Risk Test",
    "cuit": "20-22222222-2",
    "amountRequested": 100000.00,
    "monthlyIncome": 50000.00,
    "monthlyExpenses": 30000.00,
    "creditMonths": 12,
    "acceptTerms": true
  }')

APPLICATION_ID=$(echo "$APPLICATION_RESPONSE" | grep -o '"applicationId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$APPLICATION_ID" ]; then
    echo -e "${RED}Error: No se pudo crear solicitud de crédito${NC}"
    exit 1
fi

# Completar la solicitud
curl -s -X PUT "$BASE_URL/api/applications/$APPLICATION_ID/complete" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{}' > /dev/null

show_result 0 "Solicitud creada y completada: $APPLICATION_ID"
echo ""

# 1. Evaluación automática de riesgo
echo -e "${BLUE}1. Evaluación automática de riesgo...${NC}"
RISK_AUTO=$(curl -s -X POST "$BASE_URL/api/risk/assess/$APPLICATION_ID" \
  -H "Authorization: Bearer $ANALYST_TOKEN")

RISK_ASSESSMENT_ID=$(echo "$RISK_AUTO" | grep -o '"assessmentId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$RISK_ASSESSMENT_ID" ]; then
    show_result 0 "Evaluación automática realizada con ID: $RISK_ASSESSMENT_ID"
    RISK_LEVEL=$(echo "$RISK_AUTO" | grep -o '"riskLevel":"[^"]*"' | cut -d'"' -f4)
    RISK_SCORE=$(echo "$RISK_AUTO" | grep -o '"riskScore":[^,}]*' | cut -d':' -f2)
    echo "Nivel de riesgo: $RISK_LEVEL"
    echo "Puntuación: $RISK_SCORE"
    echo "Respuesta:"
    show_json "$RISK_AUTO"
else
    show_result 1 "Error al realizar evaluación automática"
    show_json "$RISK_AUTO"
fi
echo ""

# 2. Evaluación manual de riesgo
echo -e "${BLUE}2. Evaluación manual de riesgo...${NC}"
ANALYST_ID=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"analyst_risk_$(date +%s)@example.com\", \"password\": \"Pass@123\"}" 2>/dev/null | \
  grep -o '"userId":"[^"]*"' | cut -d'"' -f4)

RISK_MANUAL=$(curl -s -X POST "$BASE_URL/api/risk/assess/$APPLICATION_ID/manual" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -d "{
    \"assessedByUserId\": \"$ANALYST_ID\",
    \"score\": 75.5,
    \"level\": \"MEDIUM\",
    \"comments\": \"Evaluación manual de prueba\"
  }")

MANUAL_ID=$(echo "$RISK_MANUAL" | grep -o '"assessmentId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$MANUAL_ID" ]; then
    show_result 0 "Evaluación manual realizada con ID: $MANUAL_ID"
else
    show_result 1 "Error al realizar evaluación manual"
    show_json "$RISK_MANUAL"
fi
echo ""

# 3. Obtener evaluación más reciente
echo -e "${BLUE}3. Obtener evaluación más reciente...${NC}"
RISK_LATEST=$(curl -s -X GET "$BASE_URL/api/risk/application/$APPLICATION_ID/latest" \
  -H "Authorization: Bearer $ANALYST_TOKEN")

if echo "$RISK_LATEST" | grep -q '"success":true'; then
    show_result 0 "Evaluación más reciente obtenida exitosamente"
    echo "Respuesta:"
    show_json "$RISK_LATEST"
else
    show_result 1 "Error al obtener evaluación más reciente"
    show_json "$RISK_LATEST"
fi
echo ""

# 4. Obtener todas las evaluaciones
echo -e "${BLUE}4. Obtener todas las evaluaciones...${NC}"
RISK_ALL=$(curl -s -X GET "$BASE_URL/api/risk/application/$APPLICATION_ID" \
  -H "Authorization: Bearer $ANALYST_TOKEN")

if echo "$RISK_ALL" | grep -q '"success":true'; then
    show_result 0 "Evaluaciones obtenidas exitosamente"
    ASSESSMENTS_COUNT=$(echo "$RISK_ALL" | grep -o '"assessmentId":"[^"]*"' | wc -l)
    echo "Evaluaciones encontradas: $ASSESSMENTS_COUNT"
else
    show_result 1 "Error al obtener evaluaciones"
    show_json "$RISK_ALL"
fi
echo ""

# 5. Obtener estadísticas de riesgo
echo -e "${BLUE}5. Obtener estadísticas de riesgo...${NC}"
RISK_STATS=$(curl -s -X GET "$BASE_URL/api/risk/statistics" \
  -H "Authorization: Bearer $ANALYST_TOKEN")

if echo "$RISK_STATS" | grep -q '"success":true'; then
    show_result 0 "Estadísticas de riesgo obtenidas exitosamente"
    echo "Respuesta:"
    show_json "$RISK_STATS"
else
    show_result 1 "Error al obtener estadísticas"
    show_json "$RISK_STATS"
fi
echo ""

echo -e "${BLUE}==========================================${NC}"
echo -e "${GREEN}  PRUEBAS DE EVALUACIÓN DE RIESGO COMPLETADAS${NC}"
echo -e "${BLUE}==========================================${NC}"

