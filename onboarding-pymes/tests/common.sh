#!/bin/bash

# Funciones comunes para scripts de pruebas
# Incluir en scripts con: source "$(dirname "$0")/common.sh" o source "../common.sh"

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables comunes
BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin123!@#}"
ANALYST_EMAIL="${ANALYST_EMAIL:-analyst@example.com}"
ANALYST_PASSWORD="${ANALYST_PASSWORD:-Analyst123!@#}"
MANAGER_EMAIL="${MANAGER_EMAIL:-manager@example.com}"
MANAGER_PASSWORD="${MANAGER_PASSWORD:-Manager123!@#}"

# Función para mostrar resultados de pruebas
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

# Función para obtener token de autenticación
get_token() {
    local email=$1
    local password=$2
    
    local response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{
        \"email\": \"$email\",
        \"password\": \"$password\"
      }")
    
    echo "$response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4
}

# Función para obtener userId del token
get_user_id() {
    local response=$1
    echo "$response" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4
}

# Función para crear usuario (requiere token de ADMIN)
create_user() {
    local admin_token=$1
    local full_name=$2
    local username=$3
    local email=$4
    local password=$5
    local role_id=$6
    local consent_gdpr=${7:-true}
    
    local response=$(curl -s -X POST "$BASE_URL/api/users" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $admin_token" \
      -d "{
        \"fullName\": \"$full_name\",
        \"username\": \"$username\",
        \"email\": \"$email\",
        \"password\": \"$password\",
        \"roleId\": \"$role_id\",
        \"consentGdpr\": $consent_gdpr
      }")
    
    echo "$response"
}

# Función para verificar si usuario existe
user_exists() {
    local email=$1
    
    local token=$(get_token "$email" "dummy")
    # Si obtenemos token, el usuario existe
    if [ -n "$token" ]; then
        return 0
    else
        return 1
    fi
}

# Función para obtener o crear usuario (retorna token)
ensure_user() {
    local email=$1
    local password=$2
    local admin_token=$3
    local full_name=$4
    local username=$5
    local role_id=$6
    
    # Intentar login primero
    local token=$(get_token "$email" "$password")
    
    if [ -n "$token" ]; then
        echo "$token"
        return 0
    fi
    
    # Si no existe y tenemos admin_token, crear usuario
    if [ -n "$admin_token" ]; then
        echo -e "${YELLOW}Usuario $email no encontrado. Creando...${NC}"
        local create_response=$(create_user "$admin_token" "$full_name" "$username" "$email" "$password" "$role_id")
        
        if echo "$create_response" | grep -q '"success":true'; then
            # Esperar un momento para que el usuario se persista
            sleep 1
            token=$(get_token "$email" "$password")
            
            if [ -n "$token" ]; then
                show_result 0 "Usuario $email creado exitosamente"
                echo "$token"
                return 0
            fi
        else
            echo -e "${RED}Error al crear usuario $email${NC}"
            show_json "$create_response"
        fi
    fi
    
    return 1
}

# Función para esperar a que un endpoint esté disponible
wait_for_endpoint() {
    local endpoint=$1
    local max_attempts=${2:-30}
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s -f "$BASE_URL$endpoint" > /dev/null 2>&1; then
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 1
    done
    
    return 1
}

# Función para imprimir encabezado de sección
print_section() {
    local title=$1
    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE}  $title${NC}"
    echo -e "${BLUE}==========================================${NC}"
    echo ""
}

