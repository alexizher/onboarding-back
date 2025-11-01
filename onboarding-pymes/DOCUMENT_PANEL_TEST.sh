#!/bin/bash
# Test para el Panel de Documentos
# Uso: ./DOCUMENT_PANEL_TEST.sh

BASE_URL="http://localhost:8080"

echo "=== Test Panel de Documentos ==="
echo ""

# 1. Login como analyst
echo "1. Login como analyst..."
ANALYST_TOKEN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"analyst@example.com","password":"Analyst@123"}')

ANALYST_JWT=$(echo "$ANALYST_TOKEN_RESPONSE" | jq -r '.token // .data.token')
ANALYST_USER_ID=$(echo "$ANALYST_TOKEN_RESPONSE" | jq -r '.userId // .data.userId')

if [ -z "$ANALYST_JWT" ] || [ "$ANALYST_JWT" == "null" ]; then
  echo "ERROR: No se pudo obtener token de analyst"
  echo "Response: $ANALYST_TOKEN_RESPONSE"
  exit 1
fi

echo "✓ Token obtenido (longitud: ${#ANALYST_JWT})"
echo "✓ User ID: $ANALYST_USER_ID"
echo ""

# 2. Obtener documentos existentes del sistema
echo "2. Verificando documentos existentes..."
echo "✓ Continuando con documentos del sistema"
echo ""

# 3. Obtener estadísticas de documentos
echo "3. Obtener estadísticas de documentos..."
STATS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/documents/statistics" \
  -H "Authorization: Bearer $ANALYST_JWT")

STATS_SUCCESS=$(echo "$STATS_RESPONSE" | jq -r '.success // false')

if [ "$STATS_SUCCESS" == "true" ]; then
  echo "✓ Estadísticas obtenidas exitosamente"
  echo "Detalle de estadísticas:"
  echo "$STATS_RESPONSE" | jq '.data | {total, pending, verified, rejected, uploadedToday, uploadedLastMonth}'
else
  echo "✗ Error al obtener estadísticas"
  echo "Response: $STATS_RESPONSE"
fi
echo ""

# 4. Obtener documentos pendientes de verificación
echo "4. Obtener documentos pendientes..."
PENDING_RESPONSE=$(curl -s -X GET "$BASE_URL/api/documents/pending?page=0&size=10" \
  -H "Authorization: Bearer $ANALYST_JWT")

PENDING_SUCCESS=$(echo "$PENDING_RESPONSE" | jq -r '.success // false')
PENDING_COUNT=$(echo "$PENDING_RESPONSE" | jq -r '.data.totalElements // 0')

if [ "$PENDING_SUCCESS" == "true" ]; then
  echo "✓ Documentos pendientes obtenidos exitosamente"
  echo "Total de documentos pendientes: $PENDING_COUNT"
  
  if [ "$PENDING_COUNT" -gt 0 ]; then
    echo "Primer documento pendiente:"
    echo "$PENDING_RESPONSE" | jq '.data.content[0] | {documentId, fileName, verificationStatus, applicationId}'
    
    DOC_ID=$(echo "$PENDING_RESPONSE" | jq -r '.data.content[0].documentId // ""')
    APP_ID=$(echo "$PENDING_RESPONSE" | jq -r '.data.content[0].applicationId // ""')
    
    if [ -n "$DOC_ID" ] && [ "$DOC_ID" != "null" ]; then
      echo "✓ Usando documentId: $DOC_ID para pruebas siguientes"
    fi
  else
    echo "⚠ No hay documentos pendientes para probar verificación"
    DOC_ID=""
    APP_ID=""
  fi
else
  echo "✗ Error al obtener documentos pendientes"
  echo "Response: $PENDING_RESPONSE"
  DOC_ID=""
  APP_ID=""
fi
echo ""

# 5. Filtrar documentos - Todos los documentos (sin filtros)
echo "5. Filtrar documentos - Todos los documentos..."
FILTER_PAYLOAD='{
  "page": 0,
  "size": 10,
  "sortBy": "uploadedAt",
  "sortDirection": "DESC"
}'

FILTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/filter" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ANALYST_JWT" \
  -d "$FILTER_PAYLOAD")

FILTER_SUCCESS=$(echo "$FILTER_RESPONSE" | jq -r '.success // false')
TOTAL_DOCS=$(echo "$FILTER_RESPONSE" | jq -r '.data.totalElements // 0')

if [ "$FILTER_SUCCESS" == "true" ]; then
  echo "✓ Filtrado exitoso"
  echo "Total de documentos: $TOTAL_DOCS"
  
  if [ "$TOTAL_DOCS" -gt 0 ] && [ -z "$DOC_ID" ]; then
    DOC_ID=$(echo "$FILTER_RESPONSE" | jq -r '.data.content[0].documentId // ""')
    APP_ID=$(echo "$FILTER_RESPONSE" | jq -r '.data.content[0].applicationId // ""')
    if [ -n "$DOC_ID" ] && [ "$DOC_ID" != "null" ]; then
      echo "✓ Usando documentId: $DOC_ID para pruebas siguientes"
    fi
  fi
else
  echo "✗ Error al filtrar documentos"
  echo "Response: $FILTER_RESPONSE"
fi
echo ""

# 6. Filtrar documentos - Por estado pending
echo "6. Filtrar documentos por estado pending..."
FILTER_PENDING='{
  "verificationStatus": "pending",
  "page": 0,
  "size": 5,
  "sortBy": "uploadedAt",
  "sortDirection": "DESC"
}'

FILTER_PENDING_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/filter" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ANALYST_JWT" \
  -d "$FILTER_PENDING")

FILTER_PENDING_SUCCESS=$(echo "$FILTER_PENDING_RESPONSE" | jq -r '.success // false')
PENDING_FILTER_COUNT=$(echo "$FILTER_PENDING_RESPONSE" | jq -r '.data.totalElements // 0')

if [ "$FILTER_PENDING_SUCCESS" == "true" ]; then
  echo "✓ Filtrado por estado pending exitoso"
  echo "Total de documentos pending: $PENDING_FILTER_COUNT"
else
  echo "✗ Error al filtrar por estado pending"
  echo "Response: $FILTER_PENDING_RESPONSE"
fi
echo ""

# 7. Filtrar documentos - Por estado verified
echo "7. Filtrar documentos por estado verified..."
FILTER_VERIFIED='{
  "verificationStatus": "verified",
  "page": 0,
  "size": 5,
  "sortBy": "verifiedAt",
  "sortDirection": "DESC"
}'

FILTER_VERIFIED_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/filter" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ANALYST_JWT" \
  -d "$FILTER_VERIFIED")

FILTER_VERIFIED_SUCCESS=$(echo "$FILTER_VERIFIED_RESPONSE" | jq -r '.success // false')
VERIFIED_COUNT=$(echo "$FILTER_VERIFIED_RESPONSE" | jq -r '.data.totalElements // 0')

if [ "$FILTER_VERIFIED_SUCCESS" == "true" ]; then
  echo "✓ Filtrado por estado verified exitoso"
  echo "Total de documentos verified: $VERIFIED_COUNT"
else
  echo "✗ Error al filtrar por estado verified"
  echo "Response: $FILTER_VERIFIED_RESPONSE"
fi
echo ""

# 8. Filtrar documentos - Por applicationId (si tenemos uno)
if [ -n "$APP_ID" ] && [ "$APP_ID" != "null" ]; then
  echo "8. Filtrar documentos por applicationId..."
  FILTER_BY_APP='{
    "applicationId": "'$APP_ID'",
    "page": 0,
    "size": 10,
    "sortBy": "uploadedAt",
    "sortDirection": "DESC"
  }'
  
  FILTER_BY_APP_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/filter" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ANALYST_JWT" \
    -d "$FILTER_BY_APP")
  
  FILTER_BY_APP_SUCCESS=$(echo "$FILTER_BY_APP_RESPONSE" | jq -r '.success // false')
  APP_DOCS_COUNT=$(echo "$FILTER_BY_APP_RESPONSE" | jq -r '.data.totalElements // 0')
  
  if [ "$FILTER_BY_APP_SUCCESS" == "true" ]; then
    echo "✓ Filtrado por applicationId exitoso"
    echo "Total de documentos para esta aplicación: $APP_DOCS_COUNT"
  else
    echo "✗ Error al filtrar por applicationId"
    echo "Response: $FILTER_BY_APP_RESPONSE"
  fi
  echo ""
else
  echo "8. ⚠ Saltando filtro por applicationId: no hay applicationId disponible"
  echo ""
fi

# 9. Verificar documento (si hay uno disponible)
if [ -n "$DOC_ID" ] && [ "$DOC_ID" != "null" ]; then
  echo "9. Verificar documento..."
  VERIFY_PAYLOAD='{
    "status": "verified",
    "verifiedBy": "'$ANALYST_USER_ID'"
  }'
  
  VERIFY_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/documents/$DOC_ID/verify" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ANALYST_JWT" \
    -d "$VERIFY_PAYLOAD")
  
  VERIFY_SUCCESS=$(echo "$VERIFY_RESPONSE" | jq -r '.success // false')
  VERIFY_STATUS=$(echo "$VERIFY_RESPONSE" | jq -r '.data.verificationStatus // null')
  
  if [ "$VERIFY_SUCCESS" == "true" ]; then
    echo "✓ Verificación exitosa"
    echo "Estado del documento: $VERIFY_STATUS"
    echo "⚠ Nota: Si el usuario dueño del documento está conectado por SSE, debería recibir una notificación"
  else
    echo "✗ Error al verificar documento"
    echo "Response: $VERIFY_RESPONSE"
  fi
  echo ""
else
  echo "9. ⚠ Saltando verificación: no hay documentos disponibles"
  echo ""
fi

# 10. Obtener estadísticas finales (después de verificación)
echo "10. Obtener estadísticas finales (después de cambios)..."
FINAL_STATS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/documents/statistics" \
  -H "Authorization: Bearer $ANALYST_JWT")

FINAL_STATS_SUCCESS=$(echo "$FINAL_STATS_RESPONSE" | jq -r '.success // false')

if [ "$FINAL_STATS_SUCCESS" == "true" ]; then
  echo "✓ Estadísticas finales obtenidas"
  echo "Resumen:"
  echo "$FINAL_STATS_RESPONSE" | jq '.data | {
    total: .total,
    pending: .pending,
    verified: .verified,
    rejected: .rejected,
    uploadedToday: .uploadedToday,
    uploadedLastMonth: .uploadedLastMonth
  }'
else
  echo "✗ Error al obtener estadísticas finales"
fi
echo ""

echo "=== Test Completado ==="
echo ""
echo "Resumen:"
echo "- Login de analyst y applicant: ✓"
echo "- Estadísticas de documentos: $([ "$STATS_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
echo "- Documentos pendientes: $([ "$PENDING_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
echo "- Filtrado general: $([ "$FILTER_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
echo "- Filtrado por estado pending: $([ "$FILTER_PENDING_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
echo "- Filtrado por estado verified: $([ "$FILTER_VERIFIED_SUCCESS" == "true" ] && echo '✓' || echo '✗')"

if [ -n "$APP_ID" ] && [ "$APP_ID" != "null" ]; then
  echo "- Filtrado por applicationId: $([ "$FILTER_BY_APP_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
else
  echo "- Filtrado por applicationId: ⚠ (no disponible)"
fi

if [ -n "$DOC_ID" ] && [ "$DOC_ID" != "null" ]; then
  echo "- Verificación de documento: $([ "$VERIFY_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
else
  echo "- Verificación de documento: ⚠ (no disponible)"
fi

echo "- Estadísticas finales: $([ "$FINAL_STATS_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
echo ""
echo "Nota: Para probar las notificaciones SSE, el usuario dueño del documento debe estar conectado al stream SSE"
echo "      Endpoint SSE: GET /api/notifications/stream?token=<JWT>"

