#!/bin/bash
# Test para el Panel de Operadores/Analistas
# Uso: ./ANALYST_PANEL_TEST.sh

BASE_URL="http://localhost:8080"

echo "=== Test Panel de Operadores/Analistas ==="
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

# 2. Login como admin (para asignar)
echo "2. Login como admin..."
ADMIN_TOKEN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin123!@#"}')

ADMIN_JWT=$(echo "$ADMIN_TOKEN_RESPONSE" | jq -r '.token // .data.token')
ADMIN_USER_ID=$(echo "$ADMIN_TOKEN_RESPONSE" | jq -r '.userId // .data.userId')

if [ -z "$ADMIN_JWT" ] || [ "$ADMIN_JWT" == "null" ]; then
  echo "ERROR: No se pudo obtener token de admin"
  exit 1
fi

echo "✓ Token de admin obtenido"
echo ""

# 3. Obtener estadísticas del dashboard
echo "3. Obtener estadísticas del dashboard..."
STATS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/applications/statistics" \
  -H "Authorization: Bearer $ANALYST_JWT")

STATS_SUCCESS=$(echo "$STATS_RESPONSE" | jq -r '.success // false')

if [ "$STATS_SUCCESS" == "true" ]; then
  echo "✓ Estadísticas obtenidas exitosamente"
  echo "Detalle de estadísticas:"
  echo "$STATS_RESPONSE" | jq '.data | {total, assigned, unassigned, createdToday, createdLastMonth, byStatus}'
else
  echo "✗ Error al obtener estadísticas"
  echo "Response: $STATS_RESPONSE"
fi
echo ""

# 4. Filtrar aplicaciones - Todas las solicitudes (sin filtros)
echo "4. Filtrar aplicaciones - Todas las solicitudes..."
FILTER_PAYLOAD='{
  "page": 0,
  "size": 10,
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}'

FILTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/filter" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ANALYST_JWT" \
  -d "$FILTER_PAYLOAD")

FILTER_SUCCESS=$(echo "$FILTER_RESPONSE" | jq -r '.success // false')
TOTAL_ELEMENTS=$(echo "$FILTER_RESPONSE" | jq -r '.data.totalElements // 0')

if [ "$FILTER_SUCCESS" == "true" ]; then
  echo "✓ Filtrado exitoso"
  echo "Total de solicitudes: $TOTAL_ELEMENTS"
  echo "Solicitudes en esta página: $(echo "$FILTER_RESPONSE" | jq '.data.content | length')"
  
  if [ "$TOTAL_ELEMENTS" -gt 0 ]; then
    echo "Primera solicitud:"
    echo "$FILTER_RESPONSE" | jq '.data.content[0] | {applicationId, status, companyName, assignedTo}'
    
    APP_ID=$(echo "$FILTER_RESPONSE" | jq -r '.data.content[0].applicationId')
    echo "✓ Usando applicationId: $APP_ID para pruebas siguientes"
  else
    echo "⚠ No hay solicitudes para probar asignación"
    APP_ID=""
  fi
else
  echo "✗ Error al filtrar aplicaciones"
  echo "Response: $FILTER_RESPONSE"
  APP_ID=""
fi
echo ""

# 5. Filtrar aplicaciones - Por estado SUBMITTED
echo "5. Filtrar aplicaciones por estado SUBMITTED..."
FILTER_BY_STATUS='{
  "status": "SUBMITTED",
  "page": 0,
  "size": 5,
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}'

FILTER_STATUS_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/filter" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ANALYST_JWT" \
  -d "$FILTER_BY_STATUS")

FILTER_STATUS_SUCCESS=$(echo "$FILTER_STATUS_RESPONSE" | jq -r '.success // false')
SUBMITTED_COUNT=$(echo "$FILTER_STATUS_RESPONSE" | jq -r '.data.totalElements // 0')

if [ "$FILTER_STATUS_SUCCESS" == "true" ]; then
  echo "✓ Filtrado por estado SUBMITTED exitoso"
  echo "Total de solicitudes SUBMITTED: $SUBMITTED_COUNT"
  
  if [ "$SUBMITTED_COUNT" -gt 0 ] && [ -z "$APP_ID" ]; then
    APP_ID=$(echo "$FILTER_STATUS_RESPONSE" | jq -r '.data.content[0].applicationId // ""')
    if [ -n "$APP_ID" ] && [ "$APP_ID" != "null" ]; then
      echo "✓ Usando applicationId: $APP_ID para pruebas siguientes"
    fi
  fi
else
  echo "✗ Error al filtrar por estado"
  echo "Response: $FILTER_STATUS_RESPONSE"
fi
echo ""

# 6. Filtrar aplicaciones - Sin asignar
echo "6. Filtrar aplicaciones sin asignar..."
FILTER_UNASSIGNED='{
  "assignedToUserId": null,
  "page": 0,
  "size": 5,
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}'

FILTER_UNASSIGNED_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/filter" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ANALYST_JWT" \
  -d "$FILTER_UNASSIGNED")

FILTER_UNASSIGNED_SUCCESS=$(echo "$FILTER_UNASSIGNED_RESPONSE" | jq -r '.success // false')
UNASSIGNED_COUNT=$(echo "$FILTER_UNASSIGNED_RESPONSE" | jq -r '.data.totalElements // 0')

if [ "$FILTER_UNASSIGNED_SUCCESS" == "true" ]; then
  echo "✓ Filtrado de solicitudes sin asignar exitoso"
  echo "Total de solicitudes sin asignar: $UNASSIGNED_COUNT"
  
  if [ "$UNASSIGNED_COUNT" -gt 0 ] && [ -z "$APP_ID" ]; then
    APP_ID=$(echo "$FILTER_UNASSIGNED_RESPONSE" | jq -r '.data.content[0].applicationId // ""')
    if [ -n "$APP_ID" ] && [ "$APP_ID" != "null" ]; then
      echo "✓ Usando applicationId: $APP_ID para pruebas siguientes"
    fi
  fi
else
  echo "✗ Error al filtrar sin asignar"
  echo "Response: $FILTER_UNASSIGNED_RESPONSE"
fi
echo ""

# 7. Asignar solicitud a analyst (solo si hay APP_ID)
if [ -n "$APP_ID" ] && [ "$APP_ID" != "null" ]; then
  echo "7. Asignar solicitud a analyst..."
  ASSIGN_PAYLOAD=$(jq -n \
    --arg appId "$APP_ID" \
    --arg analystId "$ANALYST_USER_ID" \
    '{applicationId: $appId, assignedToUserId: $analystId, comments: "Asignación desde test"}')
  
  ASSIGN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/assign" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_JWT" \
    -d "$ASSIGN_PAYLOAD")
  
  ASSIGN_SUCCESS=$(echo "$ASSIGN_RESPONSE" | jq -r '.success // false')
  ASSIGNED_TO=$(echo "$ASSIGN_RESPONSE" | jq -r '.data.assignedTo // null')
  
  if [ "$ASSIGN_SUCCESS" == "true" ]; then
    echo "✓ Asignación exitosa"
    echo "Solicitud asignada a: $ASSIGNED_TO"
  else
    echo "✗ Error al asignar solicitud"
    echo "Response: $ASSIGN_RESPONSE"
  fi
  echo ""
  
  # 8. Filtrar aplicaciones asignadas a este analyst
  echo "8. Filtrar aplicaciones asignadas a este analyst..."
  FILTER_ASSIGNED='{
    "assignedToUserId": "'$ANALYST_USER_ID'",
    "page": 0,
    "size": 10,
    "sortBy": "createdAt",
    "sortDirection": "DESC"
  }'
  
  FILTER_ASSIGNED_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/filter" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ANALYST_JWT" \
    -d "$FILTER_ASSIGNED")
  
  FILTER_ASSIGNED_SUCCESS=$(echo "$FILTER_ASSIGNED_RESPONSE" | jq -r '.success // false')
  ASSIGNED_COUNT=$(echo "$FILTER_ASSIGNED_RESPONSE" | jq -r '.data.totalElements // 0')
  
  if [ "$FILTER_ASSIGNED_SUCCESS" == "true" ]; then
    echo "✓ Filtrado de solicitudes asignadas exitoso"
    echo "Total de solicitudes asignadas a este analyst: $ASSIGNED_COUNT"
  else
    echo "✗ Error al filtrar asignadas"
    echo "Response: $FILTER_ASSIGNED_RESPONSE"
  fi
  echo ""
else
  echo "7. ⚠ Saltando asignación: no hay solicitudes disponibles"
  echo ""
fi

# 9. Filtrar con múltiples criterios (empresa y monto)
echo "9. Filtrar con múltiples criterios (empresa y monto)..."
FILTER_MULTIPLE='{
  "companyName": "Test",
  "minAmount": 100,
  "maxAmount": 50000,
  "page": 0,
  "size": 5,
  "sortBy": "amountRequested",
  "sortDirection": "DESC"
}'

FILTER_MULTIPLE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications/filter" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ANALYST_JWT" \
  -d "$FILTER_MULTIPLE")

FILTER_MULTIPLE_SUCCESS=$(echo "$FILTER_MULTIPLE_RESPONSE" | jq -r '.success // false')

if [ "$FILTER_MULTIPLE_SUCCESS" == "true" ]; then
  MULTIPLE_COUNT=$(echo "$FILTER_MULTIPLE_RESPONSE" | jq -r '.data.totalElements // 0')
  echo "✓ Filtrado múltiple exitoso"
  echo "Resultados encontrados: $MULTIPLE_COUNT"
else
  echo "✗ Error en filtrado múltiple"
  echo "Response: $FILTER_MULTIPLE_RESPONSE"
fi
echo ""

# 10. Obtener estadísticas finales
echo "10. Obtener estadísticas finales (después de cambios)..."
FINAL_STATS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/applications/statistics" \
  -H "Authorization: Bearer $ANALYST_JWT")

FINAL_STATS_SUCCESS=$(echo "$FINAL_STATS_RESPONSE" | jq -r '.success // false')

if [ "$FINAL_STATS_SUCCESS" == "true" ]; then
  echo "✓ Estadísticas finales obtenidas"
  echo "Resumen:"
  echo "$FINAL_STATS_RESPONSE" | jq '.data | {
    total: .total,
    assigned: .assigned,
    unassigned: .unassigned,
    createdToday: .createdToday,
    createdLastMonth: .createdLastMonth,
    byStatus: .byStatus
  }'
else
  echo "✗ Error al obtener estadísticas finales"
fi
echo ""

echo "=== Test Completado ==="
echo ""
echo "Resumen:"
echo "- Login de analyst y admin: ✓"
echo "- Estadísticas del dashboard: $([ "$STATS_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
echo "- Filtrado general: $([ "$FILTER_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
echo "- Filtrado por estado: $([ "$FILTER_STATUS_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
echo "- Filtrado sin asignar: $([ "$FILTER_UNASSIGNED_SUCCESS" == "true" ] && echo '✓' || echo '✗')"

if [ -n "$APP_ID" ] && [ "$APP_ID" != "null" ]; then
  echo "- Asignación de solicitud: $([ "$ASSIGN_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
  echo "- Filtrado por asignado: $([ "$FILTER_ASSIGNED_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
else
  echo "- Asignación de solicitud: ⚠ (no disponible)"
  echo "- Filtrado por asignado: ⚠ (no disponible)"
fi

echo "- Filtrado múltiple: $([ "$FILTER_MULTIPLE_SUCCESS" == "true" ] && echo '✓' || echo '✗')"
echo "- Estadísticas finales: $([ "$FINAL_STATS_SUCCESS" == "true" ] && echo '✓' || echo '✗')"

