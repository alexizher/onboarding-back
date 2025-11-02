#!/bin/bash

# Script de pruebas para endpoints de gestión de usuarios
# BASE_URL: http://localhost:8080

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  TEST: Gestión de Usuarios REST API"
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

# 1. Login como ADMIN para obtener token
echo "1. Login como ADMIN..."
ADMIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "Admin123!@#"
  }')

ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}Error: No se pudo obtener token de ADMIN${NC}"
    echo "Respuesta: $ADMIN_RESPONSE"
    exit 1
fi

show_result 0 "Token ADMIN obtenido: ${ADMIN_TOKEN:0:20}..."
echo ""

# 2. Login como APPLICANT para pruebas (crear si no existe)
echo "2. Login como APPLICANT..."
APPLICANT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "Pass@123"
  }')

APPLICANT_TOKEN=$(echo $APPLICANT_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
APPLICANT_USER_ID=$(echo $APPLICANT_RESPONSE | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$APPLICANT_TOKEN" ]; then
    echo -e "${YELLOW}Usuario APPLICANT no encontrado. Creando...${NC}"
    
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
    
    if echo "$CREATE_APPLICANT_RESPONSE" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ Usuario APPLICANT creado exitosamente${NC}"
        sleep 1
        
        # Intentar login nuevamente
        APPLICANT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
          -H "Content-Type: application/json" \
          -d '{
            "email": "juan@example.com",
            "password": "Pass@123"
          }')
        
        APPLICANT_TOKEN=$(echo $APPLICANT_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        APPLICANT_USER_ID=$(echo $APPLICANT_RESPONSE | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
    else
        echo -e "${RED}Error: No se pudo crear usuario APPLICANT${NC}"
        echo "Respuesta: $CREATE_APPLICANT_RESPONSE"
        exit 1
    fi
fi

if [ -z "$APPLICANT_TOKEN" ]; then
    echo -e "${RED}Error: No se pudo obtener token de APPLICANT${NC}"
    echo "Respuesta: $APPLICANT_RESPONSE"
    exit 1
fi

show_result 0 "Token APPLICANT obtenido"
echo "Applicant User ID: $APPLICANT_USER_ID"
echo ""

# 3. GET /api/users - Listar todos los usuarios (ADMIN)
echo "3. GET /api/users - Listar todos los usuarios..."
USERS_LIST=$(curl -s -X GET "$BASE_URL/api/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json")

if echo "$USERS_LIST" | grep -q '"success":true'; then
    USER_COUNT=$(echo "$USERS_LIST" | grep -o '"userId":"[^"]*"' | wc -l)
    show_result 0 "Usuarios obtenidos: $USER_COUNT"
    echo "$USERS_LIST" | python3 -m json.tool 2>/dev/null | head -30
else
    show_result 1 "Error al obtener usuarios"
    echo "$USERS_LIST"
fi
echo ""

# 4. GET /api/users/active - Listar usuarios activos
echo "4. GET /api/users/active - Usuarios activos..."
ACTIVE_USERS=$(curl -s -X GET "$BASE_URL/api/users/active" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json")

if echo "$ACTIVE_USERS" | grep -q '"success":true'; then
    ACTIVE_COUNT=$(echo "$ACTIVE_USERS" | grep -o '"userId":"[^"]*"' | wc -l)
    show_result 0 "Usuarios activos: $ACTIVE_COUNT"
else
    show_result 1 "Error al obtener usuarios activos"
    echo "$ACTIVE_USERS"
fi
echo ""

# 5. GET /api/users/{userId} - Obtener usuario por ID (ADMIN)
echo "5. GET /api/users/$APPLICANT_USER_ID - Obtener usuario por ID..."
USER_BY_ID=$(curl -s -X GET "$BASE_URL/api/users/$APPLICANT_USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json")

if echo "$USER_BY_ID" | grep -q '"success":true'; then
    show_result 0 "Usuario obtenido exitosamente"
    echo "$USER_BY_ID" | python3 -m json.tool 2>/dev/null | head -20
else
    show_result 1 "Error al obtener usuario"
    echo "$USER_BY_ID"
fi
echo ""

# 6. GET /api/users/{userId} - Obtener propio perfil (APPLICANT)
echo "6. GET /api/users/$APPLICANT_USER_ID - Obtener propio perfil (APPLICANT)..."
OWN_PROFILE=$(curl -s -X GET "$BASE_URL/api/users/$APPLICANT_USER_ID" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -H "Content-Type: application/json")

if echo "$OWN_PROFILE" | grep -q '"success":true'; then
    show_result 0 "Perfil propio obtenido exitosamente"
else
    show_result 1 "Error al obtener perfil propio"
    echo "$OWN_PROFILE"
fi
echo ""

# 7. PUT /api/users/{userId} - Actualizar propio perfil (APPLICANT)
echo "7. PUT /api/users/$APPLICANT_USER_ID - Actualizar propio perfil..."
UPDATE_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/users/$APPLICANT_USER_ID" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Juan Pérez Actualizado",
    "phone": "3001234567"
  }')

if echo "$UPDATE_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Perfil actualizado exitosamente"
    echo "$UPDATE_RESPONSE" | python3 -m json.tool 2>/dev/null | head -20
else
    show_result 1 "Error al actualizar perfil"
    echo "$UPDATE_RESPONSE"
fi
echo ""

# 8. POST /api/users - Crear nuevo usuario (ADMIN)
echo "8. POST /api/users - Crear nuevo usuario..."
NEW_USER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Usuario Test",
    "username": "testuser'$(date +%s)'",
    "email": "test'$(date +%s)'@example.com",
    "password": "Test@Pass123",
    "roleId": "ROLE_APPLICANT",
    "consentGdpr": true
  }')

if echo "$NEW_USER_RESPONSE" | grep -q '"success":true'; then
    NEW_USER_ID=$(echo "$NEW_USER_RESPONSE" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
    show_result 0 "Usuario creado exitosamente"
    echo "Nuevo User ID: $NEW_USER_ID"
    echo "$NEW_USER_RESPONSE" | python3 -m json.tool 2>/dev/null | head -20
else
    show_result 1 "Error al crear usuario"
    echo "$NEW_USER_RESPONSE"
    NEW_USER_ID=""
fi
echo ""

# 9. POST /api/users/{userId}/deactivate - Desactivar usuario (ADMIN)
if [ ! -z "$NEW_USER_ID" ]; then
    echo "9. POST /api/users/$NEW_USER_ID/deactivate - Desactivar usuario..."
    DEACTIVATE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/$NEW_USER_ID/deactivate" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json")
    
    if echo "$DEACTIVATE_RESPONSE" | grep -q '"success":true'; then
        show_result 0 "Usuario desactivado exitosamente"
    else
        show_result 1 "Error al desactivar usuario"
        echo "$DEACTIVATE_RESPONSE"
    fi
    echo ""
fi

# 10. POST /api/users/{userId}/activate - Activar usuario (ADMIN)
if [ ! -z "$NEW_USER_ID" ]; then
    echo "10. POST /api/users/$NEW_USER_ID/activate - Activar usuario..."
    ACTIVATE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/$NEW_USER_ID/activate" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json")
    
    if echo "$ACTIVATE_RESPONSE" | grep -q '"success":true'; then
        show_result 0 "Usuario activado exitosamente"
    else
        show_result 1 "Error al activar usuario"
        echo "$ACTIVATE_RESPONSE"
    fi
    echo ""
fi

# 11. POST /api/users/{userId}/assign-role - Asignar rol (ADMIN)
if [ ! -z "$NEW_USER_ID" ]; then
    echo "11. POST /api/users/$NEW_USER_ID/assign-role - Asignar rol..."
    ASSIGN_ROLE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/$NEW_USER_ID/assign-role" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "roleId": "ROLE_ANALYST"
      }')
    
    if echo "$ASSIGN_ROLE_RESPONSE" | grep -q '"success":true'; then
        show_result 0 "Rol asignado exitosamente"
        echo "$ASSIGN_ROLE_RESPONSE" | python3 -m json.tool 2>/dev/null | head -20
    else
        show_result 1 "Error al asignar rol"
        echo "$ASSIGN_ROLE_RESPONSE"
    fi
    echo ""
fi

# 12. POST /api/users/{userId}/change-password - Cambiar contraseña (APPLICANT)
echo "12. POST /api/users/$APPLICANT_USER_ID/change-password - Cambiar contraseña..."
CHANGE_PWD_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/$APPLICANT_USER_ID/change-password" \
  -H "Authorization: Bearer $APPLICANT_TOKEN" \
  -H "Content-Type: application/json" \
      -d '{
    "currentPassword": "Pass@123",
    "newPassword": "NewPass@456"
  }')

if echo "$CHANGE_PWD_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Contraseña cambiada exitosamente"
    # Restaurar contraseña original
    echo -e "${YELLOW}Restaurando contraseña original...${NC}"
    curl -s -X POST "$BASE_URL/api/users/$APPLICANT_USER_ID/change-password" \
      -H "Authorization: Bearer $APPLICANT_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "currentPassword": "NewPass@456",
        "newPassword": "Pass@123"
      }' > /dev/null
else
    show_result 1 "Error al cambiar contraseña"
    echo "$CHANGE_PWD_RESPONSE"
fi
echo ""

# 13. Test de permisos: APPLICANT intenta ver otro usuario (debe fallar)
echo "13. Test de permisos: APPLICANT intenta ver otro usuario (debe fallar)..."
OTHER_USER_ID=$(echo "$USERS_LIST" | grep -o '"userId":"[^"]*"' | head -2 | tail -1 | cut -d'"' -f4)
if [ ! -z "$OTHER_USER_ID" ] && [ "$OTHER_USER_ID" != "$APPLICANT_USER_ID" ]; then
    FORBIDDEN_TEST=$(curl -s -X GET "$BASE_URL/api/users/$OTHER_USER_ID" \
      -H "Authorization: Bearer $APPLICANT_TOKEN" \
      -H "Content-Type: application/json")
    
    if echo "$FORBIDDEN_TEST" | grep -q '"success":false' && echo "$FORBIDDEN_TEST" | grep -q 'permiso'; then
        show_result 0 "Permisos validados correctamente (403 esperado)"
    else
        show_result 1 "Error: Debería haber devuelto 403"
        echo "$FORBIDDEN_TEST"
    fi
else
    echo -e "${YELLOW}Skipped: No hay otro usuario para probar${NC}"
fi
echo ""

echo "=========================================="
echo "  RESUMEN DE PRUEBAS"
echo "=========================================="
echo -e "${GREEN}Pruebas completadas${NC}"
echo ""
echo "Endpoints probados:"
echo "  ✓ GET /api/users"
echo "  ✓ GET /api/users/active"
echo "  ✓ GET /api/users/{userId}"
echo "  ✓ PUT /api/users/{userId}"
echo "  ✓ POST /api/users"
echo "  ✓ POST /api/users/{userId}/activate"
echo "  ✓ POST /api/users/{userId}/deactivate"
echo "  ✓ POST /api/users/{userId}/assign-role"
echo "  ✓ POST /api/users/{userId}/change-password"
echo "  ✓ Validación de permisos"
echo ""

