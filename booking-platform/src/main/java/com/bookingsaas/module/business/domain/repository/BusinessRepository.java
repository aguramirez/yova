package com.bookingsaas.module.business.domain.repository;

import com.bookingsaas.module.business.domain.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con negocios
 */
@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    /**
     * Busca un negocio por su correo electrónico
     * @param email Correo electrónico del negocio
     * @return Negocio si existe
     */
    Optional<Business> findByContactEmail(String email);

    /**
     * Busca negocios por su tipo
     * @param type Tipo de negocio
     * @return Lista de negocios del tipo especificado
     */
    List<Business> findByType(Business.BusinessType type);

    /**
     * Busca negocios por su estado de suscripción
     * @param status Estado de suscripción
     * @return Lista de negocios con el estado especificado
     */
    List<Business> findBySubscriptionStatus(Business.SubscriptionStatus status);

    /**
     * Busca negocios cuyo período de prueba está a punto de vencer
     * @param date Fecha de referencia
     * @param days Días antes del vencimiento
     * @return Lista de negocios con prueba por vencer
     */
    @Query("SELECT b FROM Business b WHERE b.subscriptionStatus = 'TRIAL' AND b.trialEndsAt <= :date AND b.trialEndsAt > :dateMinus")
    List<Business> findWithTrialEndingSoon(@Param("date") LocalDateTime date, @Param("dateMinus") LocalDateTime dateMinus);

    /**
     * Busca negocios con módulos específicos activos
     * @param moduleName Nombre del módulo
     * @return Lista de negocios con el módulo activo
     */
    @Query("SELECT b FROM Business b JOIN b.modules m WHERE m.moduleName = :moduleName AND m.active = true")
    List<Business> findWithActiveModule(@Param("moduleName") String moduleName);

    /**
     * Busca negocios por palabra clave en su nombre
     * @param keyword Palabra clave
     * @return Lista de negocios que contienen la palabra clave
     */
    List<Business> findByNameContainingIgnoreCase(String keyword);

    /**
     * Busca negocios creados después de una fecha
     * @param date Fecha de referencia
     * @return Lista de negocios creados después de la fecha
     */
    List<Business> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Cuenta negocios por tipo
     * @param type Tipo de negocio
     * @return Número de negocios del tipo especificado
     */
    long countByType(Business.BusinessType type);

    /**
     * Cuenta negocios por estado de suscripción
     * @param status Estado de suscripción
     * @return Número de negocios con el estado especificado
     */
    long countBySubscriptionStatus(Business.SubscriptionStatus status);
}