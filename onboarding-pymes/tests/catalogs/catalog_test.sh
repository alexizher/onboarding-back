#!/bin/bash

# Script de pruebas para módulo de Catálogos
# Endpoints: /api/catalogs

# Cargar funciones comunes
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

print_section "TEST: Módulo de Catálogos"

# Obtener token de ADMIN primero
echo -e "${BLUE}Obteniendo token de ADMIN...${NC}"
ADMIN_TOKEN=$(get_token "$ADMIN_EMAIL" "$ADMIN_PASSWORD")

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}Error: No se pudo obtener token de ADMIN${NC}"
    exit 1
fi

show_result 0 "Token ADMIN obtenido"
echo ""

# BUSINESS CATEGORIES
echo -e "${BLUE}=== Business Categories ===${NC}"

# 1. Listar categorías de negocio
echo -e "${BLUE}1. Listar categorías de negocio...${NC}"
CATEGORIES_RESPONSE=$(curl -s -X GET "$BASE_URL/api/catalogs/business-categories" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$CATEGORIES_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Categorías obtenidas exitosamente"
    CATEGORIES_COUNT=$(echo "$CATEGORIES_RESPONSE" | grep -o '"categoryId":"[^"]*"' | wc -l)
    echo "Categorías encontradas: $CATEGORIES_COUNT"
else
    show_result 1 "Error al obtener categorías"
    show_json "$CATEGORIES_RESPONSE"
fi
echo ""

# 2. Crear categoría de negocio
echo -e "${BLUE}2. Crear categoría de negocio...${NC}"
TIMESTAMP=$(date +%s)
CREATE_CATEGORY=$(curl -s -X POST "$BASE_URL/api/catalogs/business-categories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"name\": \"Categoría de Prueba $TIMESTAMP\",
    \"description\": \"Categoría creada para pruebas\"
  }")

CATEGORY_ID=$(echo "$CREATE_CATEGORY" | grep -o '"categoryId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$CATEGORY_ID" ]; then
    show_result 0 "Categoría creada con ID: $CATEGORY_ID"
else
    show_result 1 "Error al crear categoría"
    show_json "$CREATE_CATEGORY"
fi
echo ""

# 3. Obtener categoría por ID
if [ -n "$CATEGORY_ID" ]; then
    echo -e "${BLUE}3. Obtener categoría por ID...${NC}"
    GET_CATEGORY=$(curl -s -X GET "$BASE_URL/api/catalogs/business-categories/$CATEGORY_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN")

    if echo "$GET_CATEGORY" | grep -q '"success":true'; then
        show_result 0 "Categoría obtenida exitosamente"
    else
        show_result 1 "Error al obtener categoría"
    fi
    echo ""
fi

# DOCUMENT TYPES
echo -e "${BLUE}=== Document Types ===${NC}"

# 4. Listar tipos de documento
echo -e "${BLUE}4. Listar tipos de documento...${NC}"
DOC_TYPES_RESPONSE=$(curl -s -X GET "$BASE_URL/api/catalogs/document-types" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$DOC_TYPES_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Tipos de documento obtenidos exitosamente"
    DOC_TYPES_COUNT=$(echo "$DOC_TYPES_RESPONSE" | grep -o '"documentTypeId":"[^"]*"' | wc -l)
    echo "Tipos encontrados: $DOC_TYPES_COUNT"
else
    show_result 1 "Error al obtener tipos de documento"
    show_json "$DOC_TYPES_RESPONSE"
fi
echo ""

# 5. Crear tipo de documento
echo -e "${BLUE}5. Crear tipo de documento...${NC}"
CREATE_DOC_TYPE=$(curl -s -X POST "$BASE_URL/api/catalogs/document-types" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"name\": \"Tipo de Prueba $TIMESTAMP\",
    \"description\": \"Tipo creado para pruebas\",
    \"isRequired\": false
  }")

DOC_TYPE_ID=$(echo "$CREATE_DOC_TYPE" | grep -o '"documentTypeId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$DOC_TYPE_ID" ]; then
    show_result 0 "Tipo de documento creado con ID: $DOC_TYPE_ID"
else
    show_result 1 "Error al crear tipo de documento"
    show_json "$CREATE_DOC_TYPE"
fi
echo ""

# PROFESSIONS
echo -e "${BLUE}=== Professions ===${NC}"

# 6. Listar profesiones
echo -e "${BLUE}6. Listar profesiones...${NC}"
PROFESSIONS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/catalogs/professions" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$PROFESSIONS_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Profesiones obtenidas exitosamente"
    PROFESSIONS_COUNT=$(echo "$PROFESSIONS_RESPONSE" | grep -o '"professionId":"[^"]*"' | wc -l)
    echo "Profesiones encontradas: $PROFESSIONS_COUNT"
else
    show_result 1 "Error al obtener profesiones"
fi
echo ""

# 7. Crear profesión
echo -e "${BLUE}7. Crear profesión...${NC}"
CREATE_PROFESSION=$(curl -s -X POST "$BASE_URL/api/catalogs/professions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"name\": \"Profesión de Prueba $TIMESTAMP\",
    \"description\": \"Profesión creada para pruebas\"
  }")

PROFESSION_ID=$(echo "$CREATE_PROFESSION" | grep -o '"professionId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$PROFESSION_ID" ]; then
    show_result 0 "Profesión creada con ID: $PROFESSION_ID"
else
    show_result 1 "Error al crear profesión"
    show_json "$CREATE_PROFESSION"
fi
echo ""

# CREDIT DESTINATIONS
echo -e "${BLUE}=== Credit Destinations ===${NC}"

# 8. Listar destinos de crédito
echo -e "${BLUE}8. Listar destinos de crédito...${NC}"
DESTINATIONS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/catalogs/credit-destinations" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$DESTINATIONS_RESPONSE" | grep -q '"success":true'; then
    show_result 0 "Destinos obtenidos exitosamente"
    DESTINATIONS_COUNT=$(echo "$DESTINATIONS_RESPONSE" | grep -o '"destinationId":"[^"]*"' | wc -l)
    echo "Destinos encontrados: $DESTINATIONS_COUNT"
else
    show_result 1 "Error al obtener destinos"
fi
echo ""

# 9. Crear destino de crédito
echo -e "${BLUE}9. Crear destino de crédito...${NC}"
CREATE_DESTINATION=$(curl -s -X POST "$BASE_URL/api/catalogs/credit-destinations" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"name\": \"Destino de Prueba $TIMESTAMP\",
    \"description\": \"Destino creado para pruebas\"
  }")

DESTINATION_ID=$(echo "$CREATE_DESTINATION" | grep -o '"destinationId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$DESTINATION_ID" ]; then
    show_result 0 "Destino creado con ID: $DESTINATION_ID"
else
    show_result 1 "Error al crear destino"
    show_json "$CREATE_DESTINATION"
fi
echo ""

echo -e "${BLUE}==========================================${NC}"
echo -e "${GREEN}  PRUEBAS DE CATÁLOGOS COMPLETADAS${NC}"
echo -e "${BLUE}==========================================${NC}"

