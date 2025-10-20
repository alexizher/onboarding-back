package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    /**
     * Buscar rol por ID
     */
    Optional<Role> findByRoleId(String roleId);

    /**
     * Buscar rol por nombre
     */
    Optional<Role> findByName(String name);

    /**
     * Verificar si existe un rol por ID
     */
    boolean existsByRoleId(String roleId);

    /**
     * Verificar si existe un rol por nombre
     */
    boolean existsByName(String name);

    /**
     * Obtener todos los roles activos
     */
    List<Role> findByIsActiveTrue();

    /**
     * Buscar roles por patrón de nombre
     */
    List<Role> findByNameContainingIgnoreCase(String namePattern);

    /**
     * Obtener rol de un usuario específico
     */
    @Query("SELECT u.role FROM User u " +
           "WHERE u.userId = :userId " +
           "AND u.role.isActive = true")
    Optional<Role> findRoleByUserId(@Param("userId") String userId);

    /**
     * Verificar si un usuario tiene un rol específico
     */
    @Query("SELECT COUNT(u) > 0 FROM User u " +
           "WHERE u.userId = :userId " +
           "AND u.role.roleId = :roleId " +
           "AND u.role.isActive = true")
    boolean userHasRole(@Param("userId") String userId, @Param("roleId") String roleId);
}
