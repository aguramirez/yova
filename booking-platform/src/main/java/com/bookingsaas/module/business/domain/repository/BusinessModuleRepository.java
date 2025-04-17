package com.bookingsaas.module.business.domain.repository;

import com.bookingsaas.module.business.domain.entity.Business;
import com.bookingsaas.module.business.domain.entity.BusinessModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con módulos de negocio
 */
@Repository
public interface BusinessModuleRepository extends JpaRepository<BusinessModule, UUID> {

    /**
     * Busca todos los módulos de un negocio
     * @param businessId ID del negocio
     * @return Lista de módulos del negocio
     */
    List<BusinessModule> findByBusinessId(UUID businessId);

    /**
     * Busca un módulo específico de un negocio
     * @param businessId ID del negocio
     * @param moduleName Nombre del módulo
     * @return Módulo si existe
     */
    Optional<BusinessModule> findByBusinessIdAndModuleName(UUID businessId, String moduleName);

    /**
     * Busca todos los módulos activos de un negocio
     * @param businessId ID del negocio
     * @return Lista de módulos activos
     */
    List<BusinessModule> findByBusinessIdAndActiveTrue(UUID businessId);

    /**
     * Busca todos los negocios que tienen activo un módulo específico
     * @param moduleName Nombre del módulo
     * @return Lista de módulos activos con el nombre especificado
     */
    List<BusinessModule> findByModuleNameAndActiveTrue(String moduleName);

    /**
     * Cuenta cuántos negocios tienen activo un módulo específico
     * @param moduleName Nombre del módulo
     * @return Número de módulos activos
     */
    long countByModuleNameAndActiveTrue(String moduleName);

    /**
     * Verifica si un negocio tiene activo un módulo específico
     * @param businessId ID del negocio
     * @param moduleName Nombre del módulo
     * @return true si el módulo está activo
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM BusinessModule m WHERE m.business.id = :businessId AND m.moduleName = :moduleName AND m.active = true")
    boolean isModuleActive(@Param("businessId") UUID businessId, @Param("moduleName") String moduleName);
}