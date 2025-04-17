package com.bookingsaas.module.notification.domain.repository;

import com.bookingsaas.module.notification.domain.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con plantillas de notificación
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    /**
     * Busca plantillas por negocio
     * @param businessId ID del negocio
     * @return Lista de plantillas
     */
    List<NotificationTemplate> findByBusinessId(UUID businessId);

    /**
     * Busca una plantilla por negocio y tipo
     * @param businessId ID del negocio
     * @param type Tipo de notificación
     * @return Plantilla si existe
     */
    Optional<NotificationTemplate> findByBusinessIdAndType(UUID businessId, String type);

    /**
     * Busca plantillas por negocio y nombre
     * @param businessId ID del negocio
     * @param name Nombre de la plantilla
     * @return Lista de plantillas que coinciden con el nombre
     */
    List<NotificationTemplate> findByBusinessIdAndNameContainingIgnoreCase(UUID businessId, String name);

    /**
     * Elimina plantillas de un negocio
     * @param businessId ID del negocio
     */
    void deleteByBusinessId(UUID businessId);

    /**
     * Comprueba si existe una plantilla para un tipo específico
     * @param businessId ID del negocio
     * @param type Tipo de notificación
     * @return true si existe
     */
    boolean existsByBusinessIdAndType(UUID businessId, String type);

    /**
     * Cuenta plantillas por negocio
     * @param businessId ID del negocio
     * @return Número de plantillas
     */
    long countByBusinessId(UUID businessId);
}