package com.bookingsaas.module.booking.domain.repository;

import com.bookingsaas.module.booking.domain.entity.RewardItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con recompensas
 */
@Repository
public interface RewardItemRepository extends JpaRepository<RewardItem, UUID> {

    /**
     * Busca recompensas por negocio y estado activo
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de recompensas activas
     */
    Page<RewardItem> findByBusinessIdAndStatus(UUID businessId, RewardItem.RewardStatus status, Pageable pageable);

    /**
     * Busca recompensas por negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de recompensas
     */
    Page<RewardItem> findByBusinessId(UUID businessId, Pageable pageable);

    /**
     * Busca recompensas por tipo
     * @param businessId ID del negocio
     * @param type Tipo de recompensa
     * @param pageable Paginación
     * @return Página de recompensas del tipo especificado
     */
    Page<RewardItem> findByBusinessIdAndType(UUID businessId, RewardItem.RewardType type, Pageable pageable);

    /**
     * Busca recompensas por parte de su nombre
     * @param businessId ID del negocio
     * @param searchTerm Término de búsqueda
     * @param pageable Paginación
     * @return Página de recompensas que coinciden con la búsqueda
     */
    @Query("SELECT r FROM RewardItem r WHERE r.businessId = :businessId AND r.status = 'ACTIVE' AND LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<RewardItem> findByNameContaining(@Param("businessId") UUID businessId, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Busca recompensas por rango de puntos
     * @param businessId ID del negocio
     * @param maxPoints Puntos máximos
     * @param pageable Paginación
     * @return Página de recompensas que requieren hasta los puntos especificados
     */
    @Query("SELECT r FROM RewardItem r WHERE r.businessId = :businessId AND r.status = 'ACTIVE' AND r.pointsCost <= :maxPoints ORDER BY r.pointsCost")
    Page<RewardItem> findByMaxPoints(@Param("businessId") UUID businessId, @Param("maxPoints") Integer maxPoints, Pageable pageable);

    /**
     * Busca recompensas con stock disponible
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de recompensas con stock
     */
    @Query("SELECT r FROM RewardItem r WHERE r.businessId = :businessId AND r.status = 'ACTIVE' AND (r.stock IS NULL OR r.stock > 0)")
    Page<RewardItem> findWithStock(@Param("businessId") UUID businessId, Pageable pageable);

    /**
     * Busca recompensas populares (más canjeadas)
     * @param businessId ID del negocio
     * @return Lista de recompensas ordenadas por número de canjes
     */
    @Query("SELECT r, COUNT(rr) as redemptions FROM RewardItem r LEFT JOIN RewardRedemption rr ON r.id = rr.rewardItemId " +
           "WHERE r.businessId = :businessId AND r.status = 'ACTIVE' GROUP BY r.id ORDER BY redemptions DESC")
    List<RewardItem> findByPopularity(@Param("businessId") UUID businessId, Pageable pageable);

    /**
     * Busca recompensas de descuento
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de recompensas de tipo descuento
     */
    Page<RewardItem> findByBusinessIdAndTypeAndStatus(UUID businessId, RewardItem.RewardType type, RewardItem.RewardStatus status, Pageable pageable);

    /**
     * Cuenta recompensas por negocio
     * @param businessId ID del negocio
     * @return Número de recompensas
     */
    long countByBusinessId(UUID businessId);

    /**
     * Cuenta recompensas activas por negocio
     * @param businessId ID del negocio
     * @return Número de recompensas activas
     */
    long countByBusinessIdAndStatus(UUID businessId, RewardItem.RewardStatus status);
}