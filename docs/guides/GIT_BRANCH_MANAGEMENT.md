# Guía de Gestión de Ramas - Evitar Ramas Desactualizadas

## Problema Común

Las ramas de feature creadas al inicio del proyecto se desactualizan rápidamente cuando:
- `developer` o `main` reciben muchos commits
- Las ramas de feature no se actualizan regularmente
- Los desarrolladores trabajan en ramas antiguas sin sincronizar

## Soluciones y Mejores Prácticas

### 1. **Actualizar Ramas Regularmente (RECOMENDADO)**

#### Opción A: Rebase (Historial Limpio)
```bash
# En tu rama de feature
git checkout feature/tu-rama
git fetch origin
git rebase origin/developer

# Si hay conflictos, resolverlos y continuar
git rebase --continue
```

#### Opción B: Merge (Preserva Historial Completo)
```bash
# En tu rama de feature
git checkout feature/tu-rama
git fetch origin
git merge origin/developer

# Resolver conflictos si los hay
# Luego continuar normalmente
```

### 2. **Estrategia de Sincronización Continua**

#### Crear un script de actualización automática:
```bash
#!/bin/bash
# update-branch.sh

BRANCH=$1
TARGET_BRANCH=${2:-developer}

if [ -z "$BRANCH" ]; then
    echo "Uso: ./update-branch.sh <rama-feature> [rama-base]"
    exit 1
fi

echo "Actualizando rama $BRANCH desde $TARGET_BRANCH..."

# Guardar cambios actuales
git stash push -m "Cambios temporales antes de actualizar"

# Cambiar a la rama objetivo
git checkout $TARGET_BRANCH
git pull origin $TARGET_BRANCH

# Cambiar a la rama de feature
git checkout $BRANCH

# Actualizar con rebase
git rebase $TARGET_BRANCH

# Reaplicar cambios guardados
git stash pop

echo "Rama $BRANCH actualizada desde $TARGET_BRANCH"
```

### 3. **Workflow Recomendado para Nuevas Ramas**

```bash
# 1. SIEMPRE empezar desde developer actualizado
git checkout developer
git pull origin developer

# 2. Crear nueva rama desde developer actualizado
git checkout -b feature/nueva-funcionalidad

# 3. Trabajar en la nueva rama
# ... hacer commits ...

# 4. Antes de hacer merge, actualizar de nuevo
git checkout developer
git pull origin developer
git checkout feature/nueva-funcionalidad
git rebase developer  # o git merge developer

# 5. Resolver conflictos si los hay
# 6. Push y crear PR
```

### 4. **Comandos Útiles de Diagnóstico**

#### Ver qué tan desactualizada está una rama:
```bash
# Ver commits en developer que no están en tu rama
git log feature/tu-rama..developer --oneline

# Ver commits en tu rama que no están en developer
git log developer..feature/tu-rama --oneline

# Ver el último commit común (punto de divergencia)
git merge-base feature/tu-rama developer
git log -1 $(git merge-base feature/tu-rama developer)
```

#### Comparar cambios entre ramas:
```bash
# Ver archivos diferentes
git diff developer --name-only feature/tu-rama

# Ver cambios en un archivo específico
git diff developer feature/tu-rama -- path/to/file.java
```

### 5. **Estrategia para Ramas Muy Desactualizadas**

Si una rama está muy desactualizada (muchos commits detrás):

#### Opción 1: Rebasar (Recomendado para features)
```bash
git checkout feature/rama-vieja
git fetch origin
git rebase origin/developer

# Resolver conflictos commit por commit
# Esto reescribe el historial pero mantiene commits limpios
```

#### Opción 2: Merge único (Más seguro, preserva historial)
```bash
git checkout feature/rama-vieja
git fetch origin
git merge origin/developer

# Resolver todos los conflictos de una vez
# Crear un commit de merge
```

#### Opción 3: Recrear la rama (Si los cambios son pequeños)
```bash
# 1. Ver qué cambios únicos tiene la rama vieja
git diff developer feature/rama-vieja > cambios.patch

# 2. Crear nueva rama desde developer actualizado
git checkout developer
git checkout -b feature/rama-vieja-nueva

# 3. Aplicar los cambios
git apply cambios.patch

# 4. Resolver conflictos si los hay
# 5. Hacer commit
```

### 6. **Configuración de Git Hooks (Automatización)**

#### Pre-push hook para verificar que la rama esté actualizada:
```bash
#!/bin/bash
# .git/hooks/pre-push

current_branch=$(git symbolic-ref HEAD | sed -e 's,.*/\(.*\),\1,')
base_branch="developer"

if [ "$current_branch" != "$base_branch" ]; then
    # Verificar si hay commits nuevos en base
    git fetch origin $base_branch > /dev/null 2>&1
    commits_behind=$(git rev-list --count HEAD..origin/$base_branch)
    
    if [ "$commits_behind" -gt 5 ]; then
        echo "ADVERTENCIA: Tu rama está $commits_behind commits detrás de $base_branch"
        echo "   Considera actualizar antes de push:"
        echo "   git fetch origin $base_branch"
        echo "   git rebase origin/$base_branch"
        read -p "¿Continuar con push de todos modos? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
fi
```

### 7. **Mejores Prácticas de Equipo**

1. **Estándares de Ramas:**
   - Ramas de feature deben durar máximo 1-2 semanas
   - Ramas de feature deben actualizarse diariamente o cada 2 días
   - No trabajar directamente en `developer` o `main`

2. **Proceso de Merge:**
   - SIEMPRE actualizar la rama antes de crear PR
   - Resolver conflictos en la rama de feature, no en `developer`
   - Hacer merge de `developer` a la rama antes de hacer merge de la rama a `developer`

3. **Herramientas:**
   - Usar PR/MR para revisión
   - Requerir que las ramas estén actualizadas antes de merge
   - Usar CI/CD para verificar que las ramas compilen

### 8. **Comandos de Limpieza**

#### Limpiar ramas antiguas localmente:
```bash
# Ver ramas que ya fueron mergeadas
git branch --merged developer

# Eliminar ramas mergeadas (seguro)
git branch --merged developer | grep -v "developer\|main" | xargs git branch -d

# Forzar eliminación de ramas no mergeadas (CUIDADO)
git branch -D feature/rama-vieja
```

#### Limpiar ramas remotas:
```bash
# Ver ramas remotas eliminadas
git remote prune origin

# Eliminar ramas remotas que ya fueron mergeadas
git push origin --delete feature/rama-vieja
```

### 9. **Workflow de Actualización para Tu Caso Específico**

Dado que `feature/autenthication-system` está desactualizada:

```bash
# 1. Verificar estado actual
git checkout feature/autenthication-system
git fetch origin
git log --oneline feature/autenthication-system..origin/developer | wc -l

# 2. Guardar cambios locales si los hay
git stash

# 3. Actualizar developer local
git checkout developer
git pull origin developer

# 4. Actualizar feature desde developer
git checkout feature/autenthication-system
git rebase developer
# O si prefieres merge:
# git merge developer

# 5. Resolver conflictos sistemáticamente:
# - Ver archivos en conflicto: git status
# - Abrir cada archivo y resolver conflictos
# - Usar: git add <archivo> después de resolver
# - Continuar: git rebase --continue

# 6. Si hay muchos conflictos, considerar:
# - git rebase --abort
# - git merge developer (más fácil de resolver)

# 7. Reaplicar cambios guardados
git stash pop

# 8. Verificar que todo compile
mvn clean compile

# 9. Push forzado (si hiciste rebase)
git push origin feature/autenthication-system --force-with-lease
```

### 10. **Checklist Antes de Trabajar en una Rama**

- [ ] ¿La rama está actualizada con `developer`?
- [ ] ¿Compila sin errores?
- [ ] ¿No hay conflictos pendientes?
- [ ] ¿Los tests pasan?
- [ ] ¿Está sincronizada con el remoto?

## Resumen de Comandos Esenciales

```bash
# Actualizar rama regularmente
git checkout feature/mi-rama
git fetch origin
git rebase origin/developer

# Ver qué tan desactualizada está
git log HEAD..origin/developer --oneline

# Verificar antes de trabajar
git checkout developer
git pull
git checkout feature/mi-rama
git rebase developer

# Limpiar ramas viejas
git branch --merged developer | xargs git branch -d
```

## Recursos Adicionales

- [Git Branching Strategies](https://www.atlassian.com/git/tutorials/comparing-workflows)
- [Git Rebase vs Merge](https://www.atlassian.com/git/tutorials/rewriting-history/git-rebase)
- [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/)

