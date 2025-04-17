package com.bookingsaas.module.booking.domain.repository;

import com.bookingsaas.module.booking.domain.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con servicios
 */
@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    /**
     * Busca servicios por negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de servicios del negocio
     */
    Page<Service> findByBusinessId(UUID businessId, Pageable pageable);

    /**
     * Busca servicios por categoría
     * @param categoryId ID de la categoría
     * @param pageable Paginación
     * @return Página de servicios de la categoría
     */
    Page<Service> findByCategoryId(UUID categoryId, Pageable pageable);

    /**
     * Busca servicios por negocio y categoría
     * @param businessId ID del negocio
     * @param categoryId ID de la categoría
     * @param pageable Paginación
     * @return Página de servicios del negocio y categoría
     */
    Page<Service> findByBusinessIdAndCategoryId(UUID businessId, UUID categoryId, Pageable pageable);

    /**
     * Busca servicios por parte de su nombre
     * @param businessId ID del negocio
     * @param searchTerm Término de búsqueda
     * @param pageable Paginación
     * @return Página de servicios que coinciden con la búsqueda
     */
    @Query("SELECT s FROM Service s WHERE s.businessId = :businessId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Service> findByNameContaining(@Param("businessId") UUID businessId, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Busca servicios por rango de precio
     * @param businessId ID del negocio
     * @param minPrice Precio mínimo
     * @param maxPrice Precio máximo
     * @param pageable Paginación
     * @return Página de servicios en el rango de precio
     */
    Page<Service> findByBusinessIdAndPriceBetween(UUID businessId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Busca servicios por duración
     * @param businessId ID del negocio
     * @param duration Duración en minutos
     * @param pageable Paginación
     * @return Página de servicios con la duración especificada
     */
    Page<Service> findByBusinessIdAndDurationMinutes(UUID businessId, Integer duration, Pageable pageable);

    /**
     * Busca servicios ordenados por popularidad (más reservados)
     * @param businessId ID del negocio
     * @return Lista de servicios ordenados por número de reservas
     */
    @Query("SELECT s, COUNT(b) as bookings FROM Service s LEFT JOIN Booking b ON s.id = b.service.id " +
           "WHERE s.businessId = :businessId GROUP BY s.id ORDER BY bookings DESC")
    List<Service> findByPopularity(@Param("businessId") UUID businessId, Pageable pageable);

    /**
     * Cuenta servicios por negocio
     * @param businessId ID del negocio
     * @return Número de servicios
     */
    long countByBusinessId(UUID businessId);

    /**
     * Cuenta servicios por categoría
     * @param categoryId ID de la categoría
     * @return Número de servicios
     */
    long countByCategoryId(UUID categoryId);
}