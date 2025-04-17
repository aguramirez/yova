package com.bookingsaas.module.booking.domain.repository;

import com.bookingsaas.module.booking.domain.entity.Professional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con profesionales
 */
@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {

    /**
     * Busca profesionales por negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de profesionales del negocio
     */
    Page<Professional> findByBusinessId(UUID businessId, Pageable pageable);

    /**
     * Busca profesionales activos por negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de profesionales activos del negocio
     */
    Page<Professional> findByBusinessIdAndActiveTrue(UUID businessId, Pageable pageable);

    /**
     * Busca un profesional por el ID de usuario asociado
     * @param userId ID del usuario
     * @return Profesional si existe
     */
    Optional<Professional> findByUserId(UUID userId);

    /**
     * Busca un profesional por ID de usuario y negocio
     * @param userId ID del usuario
     * @param businessId ID del negocio
     * @return Profesional si existe
     */
    Optional<Professional> findByUserIdAndBusinessId(UUID userId, UUID businessId);

    /**
     * Busca un profesional por su ID de Google Calendar
     * @param googleCalendarId ID de Google Calendar
     * @return Profesional si existe
     */
    Optional<Professional> findByGoogleCalendarId(String googleCalendarId);

    /**
     * Busca profesionales por parte de su nombre
     * @param businessId ID del negocio
     * @param searchTerm Término de búsqueda
     * @param pageable Paginación
     * @return Página de profesionales que coinciden con la búsqueda
     */
    @Query("SELECT p FROM Professional p WHERE p.businessId = :businessId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Professional> findByNameContaining(@Param("businessId") UUID businessId, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Busca profesionales por especialización
     * @param businessId ID del negocio
     * @param specialization Especialización
     * @param pageable Paginación
     * @return Página de profesionales con la especialización especificada
     */
    Page<Professional> findByBusinessIdAndSpecialization(UUID businessId, String specialization, Pageable pageable);

    /**
     * Busca profesionales con mejor tasa de asistencia
     * @param businessId ID del negocio
     * @param minRate Tasa mínima de asistencia
     * @return Lista de profesionales con alta tasa de asistencia
     */
    @Query("SELECT p FROM Professional p WHERE p.businessId = :businessId AND p.attendanceRate >= :minRate AND p.active = true ORDER BY p.attendanceRate DESC")
    List<Professional> findWithHighAttendanceRate(@Param("businessId") UUID businessId, @Param("minRate") BigDecimal minRate);

    /**
     * Cuenta profesionales por negocio
     * @param businessId ID del negocio
     * @return Número de profesionales
     */
    long countByBusinessId(UUID businessId);

    /**
     * Cuenta profesionales activos por negocio
     * @param businessId ID del negocio
     * @return Número de profesionales activos
     */
    long countByBusinessIdAndActiveTrue(UUID businessId);
}