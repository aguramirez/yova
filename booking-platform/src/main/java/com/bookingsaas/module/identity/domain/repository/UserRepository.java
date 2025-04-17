package com.bookingsaas.module.identity.domain.repository;

import com.bookingsaas.module.identity.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con usuarios
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Busca un usuario por su correo electrónico
     * @param email Correo electrónico del usuario
     * @return Usuario si existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca un usuario por su ID en el proveedor de autenticación
     * @param authProviderId ID del usuario en Auth0/Firebase
     * @return Usuario si existe
     */
    Optional<User> findByAuthProviderId(String authProviderId);

    /**
     * Busca un usuario por su correo electrónico y estado activo
     * @param email Correo electrónico del usuario
     * @param active Estado activo
     * @return Usuario si existe
     */
    Optional<User> findByEmailAndActive(String email, boolean active);

    /**
     * Busca usuarios por parte de su nombre o apellido
     * @param searchTerm Término de búsqueda
     * @return Lista de usuarios que coinciden con la búsqueda
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> findByNameContaining(@Param("searchTerm") String searchTerm);

    /**
     * Busca usuarios asociados a un negocio específico con un rol específico
     * @param businessId ID del negocio
     * @param roleName Nombre del rol
     * @return Lista de usuarios con el rol en el negocio
     */
    @Query("SELECT u FROM User u JOIN u.businessRoles br WHERE br.business.id = :businessId AND br.role.name = :roleName")
    List<User> findByBusinessAndRole(@Param("businessId") UUID businessId, @Param("roleName") String roleName);

    /**
     * Busca usuarios asociados a un negocio
     * @param businessId ID del negocio
     * @return Lista de usuarios asociados al negocio
     */
    @Query("SELECT u FROM User u JOIN u.businessRoles br WHERE br.business.id = :businessId")
    List<User> findByBusiness(@Param("businessId") UUID businessId);

    /**
     * Comprueba si un usuario tiene un rol específico en un negocio
     * @param userId ID del usuario
     * @param businessId ID del negocio
     * @param roleName Nombre del rol
     * @return true si el usuario tiene el rol en el negocio
     */
    @Query("SELECT CASE WHEN COUNT(br) > 0 THEN true ELSE false END FROM User u JOIN u.businessRoles br WHERE u.id = :userId AND br.business.id = :businessId AND br.role.name = :roleName")
    boolean hasRole(@Param("userId") UUID userId, @Param("businessId") UUID businessId, @Param("roleName") String roleName);

    /**
     * Cuenta usuarios activos
     * @return Número de usuarios activos
     */
    long countByActiveTrue();

    /**
     * Cuenta usuarios por negocio
     * @param businessId ID del negocio
     * @return Número de usuarios asociados al negocio
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.businessRoles br WHERE br.business.id = :businessId")
    long countByBusiness(@Param("businessId") UUID businessId);
}