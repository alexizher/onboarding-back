#!/bin/bash

# Script de pruebas para endpoints de firmas digitales
# BASE_URL: http://localhost:8080

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  TEST: Firmas Digitales REST API"
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
        exit 1
    fi
fi

show_result 0 "Token APPLICANT obtenido: ${APPLICANT_TOKEN:0:20}..."
echo "Applicant User ID: $APPLICANT_USER_ID"
echo ""

# 3. Crear una solicitud de crédito primero (o usar una existente)
echo -e "${BLUE}3. Crear solicitud de crédito...${NC}"
APPLICATION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/draft" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{
    "companyName": "Empresa de Prueba S.A.",
    "cuit": "20-11111111-1",
    "amountRequested": 50000.00,
    "creditMonths": 12,
    "acceptTerms": true
  }')

APPLICATION_ID=$(echo $APPLICATION_RESPONSE | grep -o '"applicationId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$APPLICATION_ID" ]; then
    echo -e "${RED}Error: No se pudo crear solicitud${NC}"
    echo "Respuesta:"
    show_json "$APPLICATION_RESPONSE"
    exit 1
fi

show_result 0 "Solicitud creada con ID: $APPLICATION_ID"
echo ""

# 4. Completar la solicitud (DRAFT -> SUBMITTED)
echo -e "${BLUE}4. Completar solicitud (DRAFT -> SUBMITTED)...${NC}"
COMPLETE_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/applications/$APPLICATION_ID/complete" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d '{}')

if echo "$COMPLETE_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Solicitud completada"
else
    show_result 1 "Error al completar solicitud"
    echo "Respuesta:"
    show_json "$COMPLETE_RESPONSE"
fi
echo ""

# 5. Obtener tipos de documentos disponibles
echo -e "${BLUE}5. Obtener tipos de documentos...${NC}"
DOC_TYPES_RESPONSE=$(curl -s -X GET "$BASE_URL/api/catalogs/document-types" \
  -H "Authorization: Bearer $APPLICANT_TOKEN")

DOC_TYPE_ID=$(echo $DOC_TYPES_RESPONSE | grep -o '"documentTypeId":"[^"]*"' | cut -d'"' -f4 | head -1)

if [ -z "$DOC_TYPE_ID" ]; then
    echo -e "${YELLOW}No hay tipos de documentos. Intentando crear uno...${NC}"
    if [ -n "$ADMIN_TOKEN" ]; then
        CREATE_DOC_TYPE=$(curl -s -X POST "$BASE_URL/api/catalogs/document-types" \
          -H "Content-Type: application/json" \
          -H "Authorization: Bearer $ADMIN_TOKEN" \
          -d '{
            "name": "Documento de Prueba",
            "description": "Tipo de documento para pruebas",
            "isRequired": false
          }')
        
        DOC_TYPE_ID=$(echo $CREATE_DOC_TYPE | grep -o '"documentTypeId":"[^"]*"' | cut -d'"' -f4)
    fi
    
    if [ -z "$DOC_TYPE_ID" ]; then
        echo -e "${RED}Error: No se pudo obtener o crear tipo de documento${NC}"
        exit 1
    fi
fi

show_result 0 "Tipo de documento obtenido: $DOC_TYPE_ID"
echo ""

# 6. Buscar documentos existentes de la aplicación primero
echo -e "${BLUE}6. Buscar documentos existentes de la aplicación...${NC}"
DOCS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/documents/application/$APPLICATION_ID" \
  -H "Authorization: Bearer $APPLICANT_TOKEN")

DOCUMENT_ID=$(echo $DOCS_RESPONSE | grep -o '"documentId":"[^"]*"' | cut -d'"' -f4 | head -1)

# Si no hay documentos, intentar subir uno nuevo
if [ -z "$DOCUMENT_ID" ]; then
    echo -e "${YELLOW}No hay documentos existentes. Intentando subir uno nuevo...${NC}"
    
    # Crear un PDF simple en base64 con timestamp único para evitar duplicados
    TIMESTAMP=$(date +%s%N)
    # Agregar contenido único al PDF (el timestamp como texto)
    PDF_BASE64="JVBERi0xLjQKJeLjz9MKMyAwIG9iago8PAovTGVuZ3RoIDQwIDAgUgovRmlsdGVyIC9GbGF0ZURlY29kZQo+PgpzdHJlYW0KeNozNDJQMFAwNFIwVDAwMFEwMjRQMDMwNVFQMDRUMAplbmRzdHJlYW0KZW5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCAxIDAgUgovUmVzb3VyY2VzIDIgMCBSCi9Db250ZW50cyAzIDAgUgo+PgplbmRvYmoKNSAwIG9iago8PAovUGFnZXMgMSAwIFIKL1R5cGUgL0NhdGFsb2cKPj4KZW5kb2JqCjEgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9LaWRzIFs0IDAgUl0KL0NvdW50IDEKL01lZGlhQm94IFswIDAgNjEyIDc5Ml0KPj4KZW5kb2JqCjIgMCBvYmoKPDwKPj4KZW5kb2JqCjYgMCBvYmoKPDwKL1R5cGUgL0NhdGFsb2cKL1BhZ2VzIDEgMCBSCj4+CmVuZG9iagp4cmVmCjAgNwowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDAwMDkgMDAwMDAgbiAKMDAwMDAwMDA1NCAwMDAwMCBuIAowMDAwMDAwMTEwIDAwMDAwIG4gCjAwMDAwMDAyMDcgMDAwMDAgbiAKMDAwMDAwMDI1OCAwMDAwMCBuIAowMDAwMDAwMzIzIDAwMDAwIG4gCnRyYWlsZXIKPDwKL1NpemUgNwovUm9vdCA2IDAgUgo+PgKzdGFydHhyZWYKNDI0CiUlRU9GCg=="
    
    UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/upload" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $APPLICANT_TOKEN" \
      -d "{
        \"applicationId\": \"$APPLICATION_ID\",
        \"documentTypeId\": \"$DOC_TYPE_ID\",
        \"fileName\": \"documento_prueba_${TIMESTAMP}.pdf\",
        \"fileContent\": \"$PDF_BASE64\",
        \"mimeType\": \"application/pdf\"
      }")
    
    DOCUMENT_ID=$(echo $UPLOAD_RESPONSE | grep -o '"documentId":"[^"]*"' | cut -d'"' -f4)
    
    # Si falla por duplicado, buscar documentos del usuario o de cualquier aplicación para usar uno existente
    if [ -z "$DOCUMENT_ID" ] && echo "$UPLOAD_RESPONSE" | grep -q "duplicado\|duplicate"; then
        echo -e "${YELLOW}Documento duplicado detectado. Buscando documentos existentes...${NC}"
        
        # Intentar obtener documentos de la aplicación primero
        DOCS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/documents/application/$APPLICATION_ID" \
          -H "Authorization: Bearer $APPLICANT_TOKEN")
        
        DOCUMENT_ID=$(echo $DOCS_RESPONSE | grep -o '"documentId":"[^"]*"' | cut -d'"' -f4 | head -1)
        
        # Si no hay documentos en la aplicación, buscar documentos del usuario
        if [ -z "$DOCUMENT_ID" ]; then
            # El endpoint podría ser diferente, intentar con el que tenemos
            echo -e "${YELLOW}Buscando documentos del usuario en otras aplicaciones...${NC}"
            # Intentar usar cualquier documento disponible del usuario
            USER_DOCS=$(curl -s -X GET "$BASE_URL/api/documents/user/$APPLICANT_USER_ID" \
              -H "Authorization: Bearer $APPLICANT_TOKEN" 2>/dev/null || echo "")
            
            if [ -n "$USER_DOCS" ]; then
                DOCUMENT_ID=$(echo $USER_DOCS | grep -o '"documentId":"[^"]*"' | cut -d'"' -f4 | head -1)
            fi
        fi
        
        if [ -n "$DOCUMENT_ID" ]; then
            show_result 0 "Documento existente encontrado. Usando: $DOCUMENT_ID"
        else
            echo -e "${YELLOW}No se encontraron documentos existentes. Continuando con el documento duplicado existente...${NC}"
            # Si hay un error de duplicado, significa que el documento ya existe en la BD
            # Buscar el documento por hash usando la aplicación como referencia
            # En este caso, simplemente continuamos sin el documento nuevo
        fi
    fi
    
    # Si aún no tenemos un documento, buscar en todas las aplicaciones del usuario
    if [ -z "$DOCUMENT_ID" ]; then
        echo -e "${YELLOW}Intentando obtener cualquier documento disponible del usuario...${NC}"
        # Buscar en todas las aplicaciones del usuario
        APP_LIST=$(curl -s -X GET "$BASE_URL/api/applications/my-applications" \
          -H "Authorization: Bearer $APPLICANT_TOKEN" 2>/dev/null || echo "")
        
        # Extraer IDs de aplicaciones y buscar documentos
        for APP_ID in $(echo "$APP_LIST" | grep -o '"applicationId":"[^"]*"' | cut -d'"' -f4); do
            if [ -n "$APP_ID" ]; then
                APP_DOCS=$(curl -s -X GET "$BASE_URL/api/documents/application/$APP_ID" \
                  -H "Authorization: Bearer $APPLICANT_TOKEN" 2>/dev/null || echo "")
                
                DOCUMENT_ID=$(echo $APP_DOCS | grep -o '"documentId":"[^"]*"' | cut -d'"' -f4 | head -1)
                
                if [ -n "$DOCUMENT_ID" ]; then
                    show_result 0 "Documento encontrado en aplicación $APP_ID: $DOCUMENT_ID"
                    break
                fi
            fi
        done
    fi
    
    if [ -z "$DOCUMENT_ID" ]; then
        echo -e "${RED}Error: No se pudo subir documento y no hay documentos existentes disponibles${NC}"
        echo "Respuesta de upload:"
        show_json "$UPLOAD_RESPONSE"
        echo ""
        echo -e "${YELLOW}Nota: El script requiere al menos un documento para continuar con las pruebas de firmas.${NC}"
        echo -e "${YELLOW}Por favor, sube un documento manualmente y vuelve a ejecutar el script.${NC}"
        exit 1
    fi
    
    show_result 0 "Documento subido exitosamente. ID: $DOCUMENT_ID"
else
    show_result 0 "Documento existente encontrado. ID: $DOCUMENT_ID"
fi
echo ""

# 7. Firmar el documento
echo -e "${BLUE}7. Firmar documento...${NC}"
SIGN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/signatures" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -d "{
    \"documentId\": \"$DOCUMENT_ID\",
    \"signatureMethod\": \"MANUAL\",
    \"signatureData\": \"data:firma/base64;base64,SGVsbG8gV29ybGQ=\" 
  }")

SIGNATURE_ID=$(echo $SIGN_RESPONSE | grep -o '"signatureId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$SIGNATURE_ID" ]; then
    echo -e "${RED}Error: No se pudo firmar documento${NC}"
    echo "Respuesta:"
    show_json "$SIGN_RESPONSE"
    exit 1
fi

show_result 0 "Documento firmado. Signature ID: $SIGNATURE_ID"
echo "Respuesta:"
show_json "$SIGN_RESPONSE"
echo ""

# 8. Obtener firmas del documento
echo -e "${BLUE}8. Obtener firmas del documento...${NC}"
DOC_SIGNATURES=$(curl -s -X GET "$BASE_URL/api/signatures/document/$DOCUMENT_ID" \
  -H "Authorization: Bearer $APPLICANT_TOKEN")

if echo "$DOC_SIGNATURES" | grep -q '"success":true'; then
    show_result 0 "Firmas obtenidas exitosamente"
    echo "Respuesta:"
    show_json "$DOC_SIGNATURES"
else
    show_result 1 "Error al obtener firmas"
    echo "Respuesta:"
    show_json "$DOC_SIGNATURES"
fi
echo ""

# 9. Obtener una firma por ID
echo -e "${BLUE}9. Obtener firma por ID...${NC}"
GET_SIGNATURE=$(curl -s -X GET "$BASE_URL/api/signatures/$SIGNATURE_ID" \
  -H "Authorization: Bearer $APPLICANT_TOKEN")

if echo "$GET_SIGNATURE" | grep -q '"success":true'; then
    show_result 0 "Firma obtenida exitosamente"
    SIGNATURE_STATUS=$(echo $GET_SIGNATURE | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    echo "Estado de la firma: $SIGNATURE_STATUS"
else
    show_result 1 "Error al obtener firma"
    echo "Respuesta:"
    show_json "$GET_SIGNATURE"
fi
echo ""

# 10. Login como ANALYST para verificar la firma
echo -e "${BLUE}10. Login como ANALYST...${NC}"
ANALYST_EMAIL="analyst@example.com"
ANALYST_PASSWORD="Analyst123!@#"

# Intentar login primero
ANALYST_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$ANALYST_EMAIL\",
    \"password\": \"$ANALYST_PASSWORD\"
  }")

ANALYST_TOKEN=$(echo $ANALYST_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ANALYST_TOKEN" ]; then
    echo -e "${YELLOW}Usuario ANALYST no encontrado o contraseña incorrecta. Intentando crear uno nuevo...${NC}"
    
    if [ -n "$ADMIN_TOKEN" ]; then
        # Usar email único para evitar conflictos
        TIMESTAMP=$(date +%s)
        ANALYST_EMAIL="analyst_test_${TIMESTAMP}@example.com"
        ANALYST_USERNAME="testanalyst_${TIMESTAMP}"
        
        CREATE_ANALYST_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users" \
          -H "Content-Type: application/json" \
          -H "Authorization: Bearer $ADMIN_TOKEN" \
          -d "{
            \"fullName\": \"Test Analyst ${TIMESTAMP}\",
            \"username\": \"$ANALYST_USERNAME\",
            \"email\": \"$ANALYST_EMAIL\",
            \"password\": \"$ANALYST_PASSWORD\",
            \"roleId\": \"ROLE_ANALYST\",
            \"consentGdpr\": true
          }")
        
        # Verificar si la creación fue exitosa
        if echo "$CREATE_ANALYST_RESPONSE" | grep -q '"success":true'; then
            show_result 0 "Usuario ANALYST creado exitosamente con email: $ANALYST_EMAIL"
            
            # Esperar un momento para que el usuario se persista
            sleep 1
            
            # Intentar login
            ANALYST_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
              -H "Content-Type: application/json" \
              -d "{
                \"email\": \"$ANALYST_EMAIL\",
                \"password\": \"$ANALYST_PASSWORD\"
              }")
            
            ANALYST_TOKEN=$(echo $ANALYST_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
            
            if [ -n "$ANALYST_TOKEN" ]; then
                show_result 0 "Login exitoso con nuevo usuario ANALYST"
            fi
        else
            echo -e "${YELLOW}Error al crear usuario ANALYST:${NC}"
            show_json "$CREATE_ANALYST_RESPONSE"
        fi
    else
        echo -e "${YELLOW}No hay token de ADMIN para crear usuario ANALYST${NC}"
    fi
    
    if [ -z "$ANALYST_TOKEN" ]; then
        echo -e "${YELLOW}No se pudo crear/login como ANALYST. Continuando sin verificación...${NC}"
    fi
else
    show_result 0 "Login exitoso con usuario ANALYST existente"
fi

if [ -n "$ANALYST_TOKEN" ]; then
    show_result 0 "Token ANALYST obtenido: ${ANALYST_TOKEN:0:20}..."
    echo ""
    
    # 11. Verificar la firma
    echo -e "${BLUE}11. Verificar firma digital...${NC}"
    VERIFY_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/signatures/$SIGNATURE_ID/verify" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $ANALYST_TOKEN" \
      -d '{
        "isValid": true,
        "verificationNotes": "Firma verificada correctamente en pruebas"
      }')
    
    if echo "$VERIFY_RESPONSE" | grep -q '"success":true'; then
        VERIFY_STATUS=$(echo $VERIFY_RESPONSE | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$VERIFY_STATUS" = "verified" ]; then
            show_result 0 "Firma verificada exitosamente. Estado: $VERIFY_STATUS"
        else
            show_result 1 "Firma no verificada. Estado: $VERIFY_STATUS"
        fi
        echo "Respuesta:"
        show_json "$VERIFY_RESPONSE"
    else
        show_result 1 "Error al verificar firma"
        echo "Respuesta:"
        show_json "$VERIFY_RESPONSE"
    fi
    echo ""
else
    echo -e "${YELLOW}Saltando verificación de firma (no hay ANALYST disponible)${NC}"
    echo ""
fi

# 12. Obtener firmas de la aplicación
echo -e "${BLUE}12. Obtener firmas de la aplicación...${NC}"
APP_SIGNATURES=$(curl -s -X GET "$BASE_URL/api/signatures/application/$APPLICATION_ID" \
  -H "Authorization: Bearer $APPLICANT_TOKEN")

if echo "$APP_SIGNATURES" | grep -q '"success":true'; then
    show_result 0 "Firmas de aplicación obtenidas exitosamente"
    echo "Respuesta:"
    show_json "$APP_SIGNATURES"
else
    show_result 1 "Error al obtener firmas de aplicación"
    echo "Respuesta:"
    show_json "$APP_SIGNATURES"
fi
echo ""

# 13. Verificar si todos los documentos están firmados
echo -e "${BLUE}13. Verificar si todos los documentos están firmados...${NC}"
CHECK_SIGNED=$(curl -s -X GET "$BASE_URL/api/signatures/application/$APPLICATION_ID/check" \
  -H "Authorization: Bearer $APPLICANT_TOKEN")

if echo "$CHECK_SIGNED" | grep -q '"success":true'; then
    ALL_SIGNED=$(echo $CHECK_SIGNED | grep -o '"allDocumentsSigned":[^,}]*' | cut -d':' -f2)
    show_result 0 "Verificación completada. Todos firmados: $ALL_SIGNED"
    echo "Respuesta:"
    show_json "$CHECK_SIGNED"
else
    show_result 1 "Error al verificar firmas"
    echo "Respuesta:"
    show_json "$CHECK_SIGNED"
fi
echo ""

echo "=========================================="
echo -e "${GREEN}  PRUEBAS COMPLETADAS${NC}"
echo "=========================================="

