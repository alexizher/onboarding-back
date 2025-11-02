#!/bin/bash

# Script de pruebas de integración end-to-end
# Prueba el flujo completo: registro -> solicitud -> documentos -> firmas -> aprobación

# Cargar funciones comunes
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

print_section "TEST: Pruebas de Integración End-to-End"

# Obtener token de ADMIN
echo -e "${BLUE}Obteniendo token de ADMIN...${NC}"
ADMIN_TOKEN=$(get_token "$ADMIN_EMAIL" "$ADMIN_PASSWORD")

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}Error: No se pudo obtener token de ADMIN${NC}"
    exit 1
fi

show_result 0 "Token ADMIN obtenido"
echo ""

# PASO 1: Registro y Login de APPLICANT
echo -e "${BLUE}=== PASO 1: Registro y Login ===${NC}"
APPLICANT_EMAIL="applicant_integration_$(date +%s)@example.com"
APPLICANT_USERNAME="applicantint_$(date +%s)"
APPLICANT_PASSWORD="Pass@123"

REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$APPLICANT_USERNAME\",
    \"email\": \"$APPLICANT_EMAIL\",
    \"password\": \"$APPLICANT_PASSWORD\",
    \"fullName\": \"Test Integration Applicant\",
    \"dni\": \"12345678\",
    \"phone\": \"1234567890\"
  }")

if echo "$REGISTER_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Usuario registrado exitosamente"
    
    # Login
    APPLICANT_TOKEN=$(get_token "$APPLICANT_EMAIL" "$APPLICANT_PASSWORD")
    APPLICANT_ID=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\": \"$APPLICANT_EMAIL\", \"password\": \"$APPLICANT_PASSWORD\"}" | \
      grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$APPLICANT_TOKEN" ] && [ -n "$APPLICANT_ID" ]; then
        show_result 0 "Login exitoso. User ID: $APPLICANT_ID"
    else
        show_result 1 "Error al hacer login"
        exit 1
    fi
else
    show_result 1 "Error al registrar usuario"
    show_json "$REGISTER_RESPONSE"
    exit 1
fi
echo ""

# PASO 2: Crear catálogos necesarios (si no existen)
echo -e "${BLUE}=== PASO 2: Preparar Catálogos ===${NC}"
# Obtener tipos de documento
DOC_TYPES=$(curl -s -X GET "$BASE_URL/api/catalogs/document-types" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

DOC_TYPE_ID=$(echo "$DOC_TYPES" | grep -o '"documentTypeId":"[^"]*"' | cut -d'"' -f4 | head -1)

if [ -z "$DOC_TYPE_ID" ]; then
    # Crear tipo de documento
    CREATE_DOC_TYPE=$(curl -s -X POST "$BASE_URL/api/catalogs/document-types" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -d "{
        \"name\": \"Tipo Integración $(date +%s)\",
        \"description\": \"Tipo para pruebas de integración\",
        \"isRequired\": false
      }")
    
    DOC_TYPE_ID=$(echo "$CREATE_DOC_TYPE" | grep -o '"documentTypeId":"[^"]*"' | cut -d'"' -f4)
fi

if [ -n "$DOC_TYPE_ID" ]; then
    show_result 0 "Tipo de documento disponible: $DOC_TYPE_ID"
else
    show_result 1 "Error al obtener o crear tipo de documento"
    exit 1
fi
echo ""

# PASO 3: Crear solicitud de crédito
echo -e "${BLUE}=== PASO 3: Crear Solicitud de Crédito ===${NC}"
APPLICATION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/draft" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{
    "companyName": "Empresa Integración S.A.",
    "cuit": "20-44444444-4",
    "companyAddress": "Av. Test 123",
    "amountRequested": 150000.00,
    "creditMonths": 24,
    "monthlyIncome": 80000.00,
    "monthlyExpenses": 50000.00,
    "purpose": "Expansión de negocio",
    "acceptTerms": true
  }')

APPLICATION_ID=$(echo "$APPLICATION_RESPONSE" | grep -o '"applicationId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$APPLICATION_ID" ]; then
    show_result 0 "Solicitud creada: $APPLICATION_ID"
    
    # Completar solicitud
    COMPLETE_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/applications/$APPLICATION_ID/complete" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $APPLICANT_TOKEN" \
      -d '{}')
    
    if echo "$COMPLETE_RESPONSE" | grep -q '"success":true'; then
        show_result 0 "Solicitud completada (DRAFT -> SUBMITTED)"
    fi
else
    show_result 1 "Error al crear solicitud"
    exit 1
fi
echo ""

# PASO 4: Subir documento
echo -e "${BLUE}=== PASO 4: Subir Documento ===${NC}"
PDF_BASE64="JVBERi0xLjQKJeLjz9MKMyAwIG9iago8PAovTGVuZ3RoIDQwIDAgUgovRmlsdGVyIC9GbGF0ZURlY29kZQo+PgpzdHJlYW0KeNozNDJQMFAwNFIwVDAwMFEwMjRQMDMwNVFQMDRUMAplbmRzdHJlYW0KZW5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCAxIDAgUgovUmVzb3VyY2VzIDIgMCBSCi9Db250ZW50cyAzIDAgUgo+PgplbmRvYmoKNSAwIG9iago8PAovUGFnZXMgMSAwIFIKL1R5cGUgL0NhdGFsb2cKPj4KZW5kb2JqCjEgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9LaWRzIFs0IDAgUl0KL0NvdW50IDEKL01lZGlhQm94IFswIDAgNjEyIDc5Ml0KPj4KZW5kb2JqCjIgMCBvYmoKPDwKPj4KZW5kb2JqCjYgMCBvYmoKPDwKL1R5cGUgL0NhdGFsb2cKL1BhZ2VzIDEgMCBSCj4+CmVuZG9iagp4cmVmCjAgNwowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDAwMDkgMDAwMDAgbiAKMDAwMDAwMDA1NCAwMDAwMCBuIAowMDAwMDAwMTEwIDAwMDAwIG4gCjAwMDAwMDAyMDcgMDAwMDAgbiAKMDAwMDAwMDI1OCAwMDAwMCBuIAowMDAwMDAwMzIzIDAwMDAwIG4gCnRyYWlsZXIKPDwKL1NpemUgNwovUm9vdCA2IDAgUgo+PgKzdGFydHhyZWYKNDI0CiUlRU9GCg=="

UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/upload" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d "{
    \"applicationId\": \"$APPLICATION_ID\",
    \"documentTypeId\": \"$DOC_TYPE_ID\",
    \"fileName\": \"documento_integracion_$(date +%s).pdf\",
    \"fileContent\": \"$PDF_BASE64\",
    \"mimeType\": \"application/pdf\"
  }")

DOCUMENT_ID=$(echo "$UPLOAD_RESPONSE" | grep -o '"documentId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$DOCUMENT_ID" ]; then
    show_result 0 "Documento subido: $DOCUMENT_ID"
else
    show_result 1 "Error al subir documento"
    echo "Respuesta:"
    show_json "$UPLOAD_RESPONSE"
fi
echo ""

# PASO 5: Firmar documento
if [ -n "$DOCUMENT_ID" ]; then
    echo -e "${BLUE}=== PASO 5: Firmar Documento ===${NC}"
    SIGN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/signatures" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $APPLICANT_TOKEN" \
      -d "{
        \"documentId\": \"$DOCUMENT_ID\",
        \"signatureMethod\": \"MANUAL\",
        \"signatureData\": \"data:firma/base64;base64,SGVsbG8gV29ybGQ=\"
      }")
    
    SIGNATURE_ID=$(echo "$SIGN_RESPONSE" | grep -o '"signatureId":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$SIGNATURE_ID" ]; then
        show_result 0 "Documento firmado: $SIGNATURE_ID"
    else
        show_result 1 "Error al firmar documento"
    fi
    echo ""
fi

# PASO 6: Asignar analista y cambiar estado
echo -e "${BLUE}=== PASO 6: Asignar Analista ===${NC}"
# Crear analista
ANALYST_TOKEN=$(ensure_user "analyst_int_$(date +%s)@example.com" "Pass@123" "$ADMIN_TOKEN" "Test Analyst Integration" "analystint" "ROLE_ANALYST")
ANALYST_ID=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"analyst_int_$(date +%s)@example.com\", \"password\": \"Pass@123\"}" 2>/dev/null | \
  grep -o '"userId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$ANALYST_TOKEN" ] && [ -n "$APPLICATION_ID" ]; then
    # Asignar aplicación
    ASSIGN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/$APPLICATION_ID/assign" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $ANALYST_TOKEN" \
      -d "{\"assignedToUserId\": \"$ANALYST_ID\"}")
    
    if echo "$ASSIGN_RESPONSE" | grep -q '"success":true'; then
        show_result 0 "Aplicación asignada al analista"
    fi
fi
echo ""

# PASO 7: Cambiar estado a UNDER_REVIEW
if [ -n "$APPLICATION_ID" ] && [ -n "$ANALYST_TOKEN" ]; then
    echo -e "${BLUE}=== PASO 7: Cambiar Estado ===${NC}"
    CHANGE_STATUS=$(curl -s -X PUT "$BASE_URL/api/applications/$APPLICATION_ID/status" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $ANALYST_TOKEN" \
      -d '{
        "newStatus": "UNDER_REVIEW",
        "comments": "En revisión para integración"
      }')
    
    if echo "$CHANGE_STATUS" | grep -q '"success":true'; then
        show_result 0 "Estado cambiado a UNDER_REVIEW"
    else
        show_result 1 "Error al cambiar estado"
        show_json "$CHANGE_STATUS"
    fi
    echo ""
fi

# RESUMEN FINAL
echo -e "${BLUE}==========================================${NC}"
echo -e "${GREEN}  RESUMEN DE INTEGRACIÓN${NC}"
echo -e "${BLUE}==========================================${NC}"
echo "Usuario: $APPLICANT_EMAIL (ID: $APPLICANT_ID)"
echo "Solicitud: $APPLICATION_ID"
if [ -n "$DOCUMENT_ID" ]; then
    echo "Documento: $DOCUMENT_ID"
fi
if [ -n "$SIGNATURE_ID" ]; then
    echo "Firma: $SIGNATURE_ID"
fi
if [ -n "$ANALYST_ID" ]; then
    echo "Analista asignado: $ANALYST_ID"
fi
echo ""
echo -e "${GREEN}✓ Flujo de integración completado${NC}"

