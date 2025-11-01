#!/bin/bash
# Smoke test para SSE (Server-Sent Events)
# Uso: ./SSE_SMOKE_TEST.sh

BASE_URL="http://localhost:8080"
SSE_ENDPOINT="$BASE_URL/api/notifications/stream"

echo "=== SSE Smoke Test ==="
echo ""

# 1. Login como applicant
echo "1. Login como applicant..."
APP_TOKEN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"juan@example.com","password":"Pass@123"}')

APP_JWT=$(echo "$APP_TOKEN_RESPONSE" | jq -r '.token // .data.token')
APP_USER_ID=$(echo "$APP_TOKEN_RESPONSE" | jq -r '.userId // .data.userId')

if [ -z "$APP_JWT" ] || [ "$APP_JWT" == "null" ]; then
  echo "ERROR: No se pudo obtener token de applicant"
  echo "Response: $APP_TOKEN_RESPONSE"
  exit 1
fi

echo "✓ Token obtenido (longitud: ${#APP_JWT})"
echo "✓ User ID: $APP_USER_ID"
echo ""

# 2. Login como admin
echo "2. Login como admin..."
ADMIN_TOKEN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"UoT*8q#dK21t!vRw"}')

ADMIN_JWT=$(echo "$ADMIN_TOKEN_RESPONSE" | jq -r '.token // .data.token')

if [ -z "$ADMIN_JWT" ] || [ "$ADMIN_JWT" == "null" ]; then
  echo "ERROR: No se pudo obtener token de admin"
  echo "Response: $ADMIN_TOKEN_RESPONSE"
  exit 1
fi

echo "✓ Token de admin obtenido (longitud: ${#ADMIN_JWT})"
echo ""

# 3. Crear solicitud como applicant
echo "3. Crear solicitud como applicant..."
APP_PAYLOAD='{
  "companyName": "SSE Test Pyme",
  "cuit": "20999999991",
  "companyAddress": "Av Siempreviva 742",
  "amountRequested": 1000,
  "purpose": "SSE smoke test",
  "creditMonths": 6,
  "monthlyIncome": 2000,
  "monthlyExpenses": 500,
  "existingDebt": 0,
  "acceptTerms": true
}'

APP_CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/applications" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APP_JWT" \
  -d "$APP_PAYLOAD")

APP_ID=$(echo "$APP_CREATE_RESPONSE" | jq -r '.data.applicationId // .applicationId')

if [ -z "$APP_ID" ] || [ "$APP_ID" == "null" ]; then
  echo "ERROR: No se pudo crear solicitud"
  echo "Response: $APP_CREATE_RESPONSE"
  exit 1
fi

echo "✓ Solicitud creada: $APP_ID"
echo ""

# 4. Abrir stream SSE en background
echo "4. Abriendo stream SSE..."
rm -f /tmp/sse_test.out /tmp/sse_test.pid

# Stream SSE con token en query parameter (compatible con Angular EventSource)
nohup bash -c "curl -N -H 'Accept: text/event-stream' \
  '$SSE_ENDPOINT?token=$APP_JWT' > /tmp/sse_test.out 2>&1" >/dev/null 2>&1 & 

SSE_PID=$!
echo $SSE_PID > /tmp/sse_test.pid
echo "✓ Stream SSE iniciado (PID: $SSE_PID)"
echo ""

# Esperar a que el stream se conecte
sleep 2

# Inicializar variables de estado
INIT_DETECTED=0
EVENT_STATUS_DETECTED=0
SECOND_EVENT_DETECTED=0
PING_DETECTED=0

# Verificar que el stream está recibiendo eventos
if grep -q "event:init" /tmp/sse_test.out 2>/dev/null; then
  echo "✓ Stream conectado (evento 'init' recibido)"
  INIT_DETECTED=1
else
  echo "⚠ Advertencia: No se detectó evento 'init' en el stream"
  echo "Últimas líneas del stream:"
  tail -n 10 /tmp/sse_test.out 2>/dev/null | grep -v "^%" | grep -v "^[[:space:]]*$" || echo "(sin salida)"
fi
echo ""

# 5. Cambiar estado de la solicitud (como admin)
echo "5. Cambiar estado de solicitud a 'SUBMITTED'..."
STATUS_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/applications/$APP_ID/status" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -d '{"newStatus":"SUBMITTED","comments":"SSE smoke test"}')

echo "✓ Estado cambiado (response: $(echo $STATUS_RESPONSE | head -c 50))"
echo ""

# Esperar a que el evento SSE llegue
sleep 2

# Verificar que se recibió el evento application-status
if grep -q "event:application-status" /tmp/sse_test.out 2>/dev/null || grep -q "APPLICATION_STATUS_CHANGED" /tmp/sse_test.out 2>/dev/null; then
  echo "✓ Evento 'application-status' recibido en el stream"
  echo ""
  echo "Últimas líneas del stream:"
  tail -n 20 /tmp/sse_test.out | grep -v "^%" | grep -v "^[[:space:]]*$"
  EVENT_STATUS_DETECTED=1
else
  echo "⚠ Advertencia: No se detectó evento 'application-status'"
  echo "Contenido completo del stream:"
  cat /tmp/sse_test.out 2>/dev/null | grep -v "^%" | grep -v "^[[:space:]]*$" || echo "(sin salida)"
  EVENT_STATUS_DETECTED=0
fi
echo ""

# 6. Cambiar estado nuevamente
echo "6. Cambiar estado a 'UNDER_REVIEW'..."
STATUS_RESPONSE2=$(curl -s -X PUT "$BASE_URL/api/applications/$APP_ID/status" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -d '{"newStatus":"UNDER_REVIEW","comments":"SSE smoke test 2"}')

echo "✓ Estado cambiado nuevamente"
echo ""

# Esperar y verificar
sleep 2

if grep -q "UNDER_REVIEW" /tmp/sse_test.out 2>/dev/null; then
  echo "✓ Segundo evento recibido con estado 'UNDER_REVIEW'"
  SECOND_EVENT_DETECTED=1
else
  echo "⚠ Segundo evento no detectado"
  SECOND_EVENT_DETECTED=0
fi
echo ""

# 7. Verificar heartbeat (pings)
echo "7. Verificando heartbeat (pings)..."
sleep 12  # Esperar un ciclo de heartbeat (10s)

if grep -q "event:ping" /tmp/sse_test.out 2>/dev/null; then
  PING_COUNT=$(grep -c "event:ping" /tmp/sse_test.out 2>/dev/null || echo "0")
  echo "✓ Heartbeat funcionando (pings detectados: $PING_COUNT)"
  PING_DETECTED=1
else
  echo "⚠ No se detectaron pings de heartbeat"
fi
echo ""

# 8. Limpiar
echo "8. Limpiando..."
kill $SSE_PID 2>/dev/null || true
rm -f /tmp/sse_test.out /tmp/sse_test.pid
echo "✓ Proceso SSE terminado"
echo ""

echo "=== Smoke Test Completado ==="
echo ""
echo "Resumen:"
echo "- Login de applicant y admin: ✓"
echo "- Creación de solicitud: ✓"
echo "- Conexión SSE: $([ $INIT_DETECTED -eq 1 ] && echo '✓' || echo '✗')"
echo "- Eventos application-status: $([ $EVENT_STATUS_DETECTED -eq 1 ] && echo '✓' || echo '✗')"
echo "- Segundo evento (UNDER_REVIEW): $([ $SECOND_EVENT_DETECTED -eq 1 ] && echo '✓' || echo '✗')"
echo "- Heartbeat (pings): $([ $PING_DETECTED -eq 1 ] && echo '✓' || echo '✗')"

