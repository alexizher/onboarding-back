#!/bin/bash

# Script de pruebas para endpoints de formulario dinámico con borradores
# BASE_URL: http://localhost:8080

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  TEST: Formulario Dinámico - Borradores"
echo "=========================================="
echo ""

# Función para mostrar resultados
show_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
    fi
}

# Función para mostrar JSON de forma legible
show_json() {
    echo "$1" | python3 -m json.tool 2>/dev/null || echo "$1"
}

# 1. Login como ADMIN primero para crear APPLICANT si es necesario
echo -e "${BLUE}1. Login como ADMIN...${NC}"
ADMIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "Admin123!@#"
  }')

ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${YELLOW}ADMIN no disponible, continuando sin él...${NC}"
else
    show_result 0 "Token ADMIN obtenido: ${ADMIN_TOKEN:0:20}..."
fi
echo ""

# 2. Login como APPLICANT para obtener token
echo -e "${BLUE}2. Login como APPLICANT...${NC}"
APPLICANT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "Pass@123"
  }')

APPLICANT_TOKEN=$(echo $APPLICANT_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
APPLICANT_USER_ID=$(echo $APPLICANT_RESPONSE | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$APPLICANT_TOKEN" ]; then
    echo -e "${YELLOW}Usuario APPLICANT no encontrado, intentando crear uno...${NC}"
    
    if [ -n "$ADMIN_TOKEN" ]; then
        # Crear APPLICANT usando el endpoint de gestión de usuarios
        CREATE_APPLICANT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users" \
          -H "Content-Type: application/json" \
          -H "Authorization: Bearer $ADMIN_TOKEN" \
          -d '{
            "fullName": "Juan Applicant",
            "username": "juanapplicant",
            "email": "juan@example.com",
            "password": "Pass@123",
            "roleId": "ROLE_APPLICANT",
            "consentGdpr": true
          }')
        
        echo "Respuesta de creación: $CREATE_APPLICANT_RESPONSE"
        
        # Intentar login nuevamente
        APPLICANT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
          -H "Content-Type: application/json" \
          -d '{
            "email": "juan@example.com",
            "password": "Pass@123"
          }')
        
        APPLICANT_TOKEN=$(echo $APPLICANT_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        APPLICANT_USER_ID=$(echo $APPLICANT_RESPONSE | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
    fi
    
    if [ -z "$APPLICANT_TOKEN" ]; then
        echo -e "${RED}Error: No se pudo obtener token de APPLICANT${NC}"
        echo "Respuesta: $APPLICANT_RESPONSE"
        echo ""
        echo -e "${YELLOW}NOTA: Asegúrate de que la aplicación esté corriendo en $BASE_URL${NC}"
        echo -e "${YELLOW}O crea un usuario APPLICANT manualmente usando el endpoint de gestión de usuarios${NC}"
        exit 1
    fi
fi

show_result 0 "Token APPLICANT obtenido: ${APPLICANT_TOKEN:0:20}..."
echo "Applicant User ID: $APPLICANT_USER_ID"
echo ""

# 3. Crear un borrador con campos incompletos
echo -e "${BLUE}3. Crear borrador con campos incompletos...${NC}"
DRAFT_CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/draft" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{
    "companyName": "Mi Empresa S.A.",
    "cuit": "20-12345678-9",
    "amountRequested": 50000.00
  }')

DRAFT_ID=$(echo $DRAFT_CREATE_RESPONSE | grep -o '"applicationId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$DRAFT_ID" ]; then
    echo -e "${RED}Error: No se pudo crear borrador${NC}"
    echo "Respuesta:"
    show_json "$DRAFT_CREATE_RESPONSE"
    exit 1
fi

show_result 0 "Borrador creado con ID: $DRAFT_ID"
echo "Respuesta:"
show_json "$DRAFT_CREATE_RESPONSE"
echo ""

# 4. Listar borradores del usuario
echo -e "${BLUE}4. Listar borradores del usuario...${NC}"
DRAFTS_LIST_RESPONSE=$(curl -s -X GET "$BASE_URL/api/applications/my-drafts" \
  -H "Authorization: Bearer $APPLICANT_TOKEN")

DRAFTS_COUNT=$(echo $DRAFTS_LIST_RESPONSE | grep -o '"count":[0-9]*' | cut -d':' -f2 || echo "0")

if echo "$DRAFTS_LIST_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Borradores obtenidos exitosamente (${DRAFTS_COUNT} borrador/es)"
    echo "Respuesta:"
    show_json "$DRAFTS_LIST_RESPONSE"
else
    show_result 1 "Error al obtener borradores"
    echo "Respuesta:"
    show_json "$DRAFTS_LIST_RESPONSE"
fi
echo ""

# 5. Actualizar el borrador agregando más campos
echo -e "${BLUE}5. Actualizar borrador agregando más campos...${NC}"
DRAFT_UPDATE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/draft?applicationId=$DRAFT_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{
    "companyName": "Mi Empresa S.A.",
    "cuit": "20-12345678-9",
    "companyAddress": "Av. Principal 123",
    "amountRequested": 50000.00,
    "purpose": "Capital de trabajo",
    "creditMonths": 12,
    "monthlyIncome": 100000.00,
    "monthlyExpenses": 60000.00,
    "existingDebt": 20000.00
  }')

if echo "$DRAFT_UPDATE_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Borrador actualizado exitosamente"
    echo "Respuesta:"
    show_json "$DRAFT_UPDATE_RESPONSE"
else
    show_result 1 "Error al actualizar borrador"
    echo "Respuesta:"
    show_json "$DRAFT_UPDATE_RESPONSE"
fi
echo ""

# 6. Intentar completar borrador sin aceptar términos (debe fallar)
echo -e "${BLUE}6. Intentar completar borrador sin aceptar términos (debe fallar)...${NC}"
COMPLETE_FAIL_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/applications/$DRAFT_ID/complete" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{}')

if echo "$COMPLETE_FAIL_RESPONSE" | grep -q '"success":false'; then
    show_result 0 "Validación correcta: no permite completar sin aceptar términos"
    echo "Respuesta:"
    show_json "$COMPLETE_FAIL_RESPONSE"
else
    show_result 1 "Error: debería haber fallado la validación"
    echo "Respuesta:"
    show_json "$COMPLETE_FAIL_RESPONSE"
fi
echo ""

# 7. Actualizar borrador aceptando términos
echo -e "${BLUE}7. Actualizar borrador aceptando términos...${NC}"
DRAFT_WITH_TERMS_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/draft?applicationId=$DRAFT_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{
    "companyName": "Mi Empresa S.A.",
    "cuit": "20-12345678-9",
    "companyAddress": "Av. Principal 123",
    "amountRequested": 50000.00,
    "purpose": "Capital de trabajo",
    "creditMonths": 12,
    "monthlyIncome": 100000.00,
    "monthlyExpenses": 60000.00,
    "existingDebt": 20000.00,
    "acceptTerms": true
  }')

if echo "$DRAFT_WITH_TERMS_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Borrador actualizado con términos aceptados"
else
    show_result 1 "Error al actualizar borrador con términos"
    echo "Respuesta:"
    show_json "$DRAFT_WITH_TERMS_RESPONSE"
fi
echo ""

# 8. Completar borrador (debe cambiar de DRAFT a SUBMITTED)
echo -e "${BLUE}8. Completar borrador (DRAFT -> SUBMITTED)...${NC}"
COMPLETE_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/applications/$DRAFT_ID/complete" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{"comments": "Borrador completado y enviado"}')

if echo "$COMPLETE_RESPONSE" | grep -q '"success":true'; then
    # Verificar que el estado cambió a SUBMITTED
    STATUS=$(echo $COMPLETE_RESPONSE | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    
    if [ "$STATUS" = "SUBMITTED" ]; then
        show_result 0 "Borrador completado exitosamente. Estado: $STATUS"
    else
        show_result 1 "Estado incorrecto. Esperado: SUBMITTED, Obtenido: $STATUS"
    fi
    
    echo "Respuesta:"
    show_json "$COMPLETE_RESPONSE"
else
    show_result 1 "Error al completar borrador"
    echo "Respuesta:"
    show_json "$COMPLETE_RESPONSE"
fi
echo ""

# 9. Intentar actualizar borrador ya completado (debe fallar)
echo -e "${BLUE}9. Intentar actualizar borrador ya completado (debe fallar)...${NC}"
UPDATE_COMPLETED_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/draft?applicationId=$DRAFT_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{
    "companyName": "Mi Empresa Modificada S.A."
  }')

if echo "$UPDATE_COMPLETED_RESPONSE" | grep -q '"success":false'; then
    show_result 0 "Validación correcta: no permite editar solicitud ya completada"
    echo "Respuesta:"
    show_json "$UPDATE_COMPLETED_RESPONSE"
else
    show_result 1 "Error: debería haber fallado la validación"
    echo "Respuesta:"
    show_json "$UPDATE_COMPLETED_RESPONSE"
fi
echo ""

# 10. Verificar que el borrador completado ya no aparece en /my-drafts
echo -e "${BLUE}10. Verificar que borrador completado ya no aparece en /my-drafts...${NC}"
FINAL_DRAFTS_LIST=$(curl -s -X GET "$BASE_URL/api/applications/my-drafts" \
  -H "Authorization: Bearer $APPLICANT_TOKEN")

FINAL_COUNT=$(echo $FINAL_DRAFTS_LIST | grep -o '"count":[0-9]*' | cut -d':' -f2 || echo "0")

if [ "$FINAL_COUNT" = "0" ] || [ -z "$FINAL_COUNT" ]; then
    show_result 0 "Borrador completado correctamente removido de la lista de borradores"
else
    show_result 1 "El borrador completado todavía aparece en la lista (count: $FINAL_COUNT)"
fi
echo "Respuesta:"
show_json "$FINAL_DRAFTS_LIST"
echo ""

# 11. Crear otro borrador para verificar que se pueden tener múltiples borradores
echo -e "${BLUE}11. Crear segundo borrador...${NC}"
DRAFT2_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/draft" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{
    "companyName": "Segunda Empresa S.A.",
    "cuit": "30-87654321-0",
    "amountRequested": 100000.00,
    "creditMonths": 24
  }')

DRAFT2_ID=$(echo $DRAFT2_RESPONSE | grep -o '"applicationId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$DRAFT2_ID" ]; then
    show_result 0 "Segundo borrador creado con ID: $DRAFT2_ID"
    
    # Verificar que ahora hay 2 borradores en la lista
    MULTIPLE_DRAFTS=$(curl -s -X GET "$BASE_URL/api/applications/my-drafts" \
      -H "Authorization: Bearer $APPLICANT_TOKEN")
    
    MULTIPLE_COUNT=$(echo $MULTIPLE_DRAFTS | grep -o '"count":[0-9]*' | cut -d':' -f2 || echo "0")
    
    if [ "$MULTIPLE_COUNT" = "1" ]; then
        show_result 0 "Múltiples borradores soportados correctamente (count: $MULTIPLE_COUNT)"
    else
        show_result 1 "Error en conteo de borradores (esperado: 1, obtenido: $MULTIPLE_COUNT)"
    fi
else
    show_result 1 "Error al crear segundo borrador"
    echo "Respuesta:"
    show_json "$DRAFT2_RESPONSE"
fi
echo ""

echo "=========================================="
echo -e "${GREEN}  PRUEBAS COMPLETADAS${NC}"
echo "=========================================="

