#!/bin/bash
# Script para actualizar ramas de feature desde developer

set -e

FEATURE_BRANCH=${1:-$(git branch --show-current)}
BASE_BRANCH=${2:-developer}

if [ "$FEATURE_BRANCH" = "$BASE_BRANCH" ] || [ "$FEATURE_BRANCH" = "main" ]; then
    echo "❌ No puedes actualizar $FEATURE_BRANCH desde sí mismo"
    exit 1
fi

echo "🔄 Actualizando rama: $FEATURE_BRANCH desde $BASE_BRANCH"
echo ""

# Verificar que hay cambios guardados
if [ -n "$(git status --porcelain)" ]; then
    echo "⚠️  Tienes cambios sin commitear. Guardándolos en stash..."
    git stash push -m "Auto-stash antes de actualizar desde $BASE_BRANCH - $(date)"
fi

# Obtener últimas actualizaciones
echo "📥 Obteniendo actualizaciones del remoto..."
git fetch origin

# Verificar cuántos commits detrás está
COMMITS_BEHIND=$(git rev-list --count origin/$BASE_BRANCH..$FEATURE_BRANCH 2>/dev/null || echo "0")
COMMITS_AHEAD=$(git rev-list --count $FEATURE_BRANCH..origin/$BASE_BRANCH 2>/dev/null || echo "0")

if [ "$COMMITS_AHEAD" -eq 0 ]; then
    echo "✅ Tu rama ya está actualizada con origin/$BASE_BRANCH"
    exit 0
fi

echo "📊 Estadísticas:"
echo "   - Commits en $BASE_BRANCH que no tienes: $COMMITS_AHEAD"
echo "   - Commits en tu rama que no están en $BASE_BRANCH: $COMMITS_BEHIND"
echo ""

# Actualizar base branch localmente
echo "🔄 Actualizando $BASE_BRANCH local..."
git checkout $BASE_BRANCH 2>/dev/null || git checkout -b $BASE_BRANCH origin/$BASE_BRANCH
git pull origin $BASE_BRANCH

# Volver a feature branch
echo "🔄 Cambiando a $FEATURE_BRANCH..."
git checkout $FEATURE_BRANCH

# Preguntar método de actualización
echo ""
echo "¿Cómo quieres actualizar?"
echo "  1) Rebase (recomendado para features - historial limpio)"
echo "  2) Merge (más seguro - preserva historial)"
read -p "Elige (1 o 2): " choice

case $choice in
    1)
        echo "🔄 Aplicando rebase..."
        git rebase $BASE_BRANCH || {
            echo "❌ Conflictos durante rebase. Resuelve los conflictos y ejecuta:"
            echo "   git rebase --continue"
            exit 1
        }
        ;;
    2)
        echo "🔄 Aplicando merge..."
        git merge $BASE_BRANCH || {
            echo "❌ Conflictos durante merge. Resuelve los conflictos y ejecuta:"
            echo "   git commit"
            exit 1
        }
        ;;
    *)
        echo "❌ Opción inválida"
        exit 1
        ;;
esac

# Reaplicar cambios guardados
if [ -n "$(git stash list | grep 'Auto-stash antes de actualizar')" ]; then
    echo "🔄 Reaplicando cambios guardados..."
    git stash pop || echo "⚠️  Hubo conflictos al reaplicar cambios. Revísalos con: git stash show -p"
fi

echo ""
echo "✅ Rama $FEATURE_BRANCH actualizada desde $BASE_BRANCH"
echo ""
echo "📝 Siguientes pasos:"
echo "   1. Verifica que compile: mvn clean compile"
echo "   2. Ejecuta tests: mvn test"
echo "   3. Si hiciste rebase, fuerza push: git push origin $FEATURE_BRANCH --force-with-lease"
