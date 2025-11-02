#!/bin/bash

# Script para ejecutar la limpieza de tablas no utilizadas
# Requiere: mysql client instalado y credenciales configuradas

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}  Limpieza de Tablas No Utilizadas${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

# Configuración de base de datos (desde application.properties)
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="onboarding_db"
DB_USER="onboarding_user"
DB_PASS="8gmcu87aRxDAJa"

# Ruta del script SQL
SQL_SCRIPT="$(dirname "$0")/clear_unused_tables.sql"

# Verificar que el script SQL existe
if [ ! -f "$SQL_SCRIPT" ]; then
    echo -e "${RED}Error: No se encontró el script SQL: $SQL_SCRIPT${NC}"
    exit 1
fi

echo -e "${YELLOW}Advertencia: Esta operación vaciará las siguientes tablas:${NC}"
echo -e "${YELLOW}  Tablas principales:${NC}"
echo -e "${YELLOW}  - credit_applications${NC}"
echo -e "${YELLOW}  - digital_signatures${NC}"
echo -e "${YELLOW}  - documents${NC}"
echo -e "${YELLOW}  - application_status_history${NC}"
echo -e "${YELLOW}  - users${NC}"
echo -e "${YELLOW}  - roles${NC}"
echo -e "${YELLOW}  Tablas de catálogos y otras:${NC}"
echo -e "${YELLOW}  - business_categories${NC}"
echo -e "${YELLOW}  - credit_destinations${NC}"
echo -e "${YELLOW}  - kyc_verifications${NC}"
echo -e "${YELLOW}  - login_attempts${NC}"
echo -e "${YELLOW}  - onboarding${NC}"
echo -e "${YELLOW}  - password_reset_tokens${NC}"
echo -e "${YELLOW}  - professions${NC}"
echo -e "${YELLOW}  - pyme_info${NC}"
echo -e "${YELLOW}  - risk_assessments${NC}"
echo -e "${YELLOW}  - user_sessions${NC}"
echo ""

read -p "¿Estás seguro de que quieres continuar? (s/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[SsYy]$ ]]; then
    echo -e "${YELLOW}Operación cancelada.${NC}"
    exit 0
fi

echo -e "${BLUE}Ejecutando limpieza de tablas...${NC}"

# Ejecutar script SQL
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$SQL_SCRIPT"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Tablas vaciadas exitosamente${NC}"
    echo ""
    echo -e "${BLUE}Verificando estado de las tablas:${NC}"
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
        SELECT 
            'credit_applications' AS tabla, COUNT(*) AS registros FROM credit_applications
        UNION ALL
        SELECT 
            'digital_signatures' AS tabla, COUNT(*) AS registros FROM digital_signatures
        UNION ALL
        SELECT 
            'documents' AS tabla, COUNT(*) AS registros FROM documents
        UNION ALL
        SELECT 
            'application_status_history' AS tabla, COUNT(*) AS registros FROM application_status_history
        UNION ALL
        SELECT 
            'users' AS tabla, COUNT(*) AS registros FROM users
        UNION ALL
        SELECT 
            'roles' AS tabla, COUNT(*) AS registros FROM roles
        UNION ALL
        SELECT 
            'business_categories' AS tabla, COUNT(*) AS registros FROM business_categories
        UNION ALL
        SELECT 
            'credit_destinations' AS tabla, COUNT(*) AS registros FROM credit_destinations
        UNION ALL
        SELECT 
            'kyc_verifications' AS tabla, COUNT(*) AS registros FROM kyc_verifications
        UNION ALL
        SELECT 
            'login_attempts' AS tabla, COUNT(*) AS registros FROM login_attempts
        UNION ALL
        SELECT 
            'onboarding' AS tabla, COUNT(*) AS registros FROM onboarding
        UNION ALL
        SELECT 
            'password_reset_tokens' AS tabla, COUNT(*) AS registros FROM password_reset_tokens
        UNION ALL
        SELECT 
            'professions' AS tabla, COUNT(*) AS registros FROM professions
        UNION ALL
        SELECT 
            'pyme_info' AS tabla, COUNT(*) AS registros FROM pyme_info
        UNION ALL
        SELECT 
            'risk_assessments' AS tabla, COUNT(*) AS registros FROM risk_assessments
        UNION ALL
        SELECT 
            'user_sessions' AS tabla, COUNT(*) AS registros FROM user_sessions;
    " 2>/dev/null
    echo ""
    echo -e "${GREEN}✓ Limpieza completada${NC}"
else
    echo -e "${RED}✗ Error al ejecutar la limpieza${NC}"
    exit 1
fi

