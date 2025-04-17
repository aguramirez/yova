package com.bookingsaas.module.booking.domain.repository;

import com.bookingsaas.module.booking.domain.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con reservas
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Busca una reserva por su código de asistencia
     * @param attendanceCode Código de asistencia
     * @return Reserva si existe
     */
    Optional<Booking> findByAttendanceCode(String attendanceCode);

    /**
     * Busca reservas por negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de reservas del negocio
     */
    Page<Booking> findByBusinessId(UUID businessId, Pageable pageable);

    /**
     * Busca reservas por cliente
     * @param customerId ID del cliente
     * @param pageable Paginación
     * @return Página de reservas del cliente
     */
    Page<Booking> findByCustomerId(UUID customerId, Pageable pageable);

    /**
     * Busca reservas por profesional
     * @param professionalId ID del profesional
     * @param pageable Paginación
     * @return Página de reservas del profesional
     */
    Page<Booking> findByProfessionalId(UUID professionalId, Pageable pageable);

    /**
     * Busca reservas por negocio y estado
     * @param businessId ID del negocio
     * @param status Estado de la reserva
     * @param pageable Paginación
     * @return Página de reservas con el estado especificado
     */
    Page<Booking> findByBusinessIdAndStatus(UUID businessId, Booking.BookingStatus status, Pageable pageable);

    /**
     * Busca reservas por rango de fechas
     * @param businessId ID del negocio
     * @param startFrom Fecha de inicio (desde)
     * @param startTo Fecha de inicio (hasta)
     * @param pageable Paginación
     * @return Página de reservas en el rango especificado
     */
    Page<Booking> findByBusinessIdAndStartTimeBetween(UUID businessId, LocalDateTime startFrom, LocalDateTime startTo, Pageable pageable);

    /**
     * Busca reservas para un día específico
     * @param businessId ID del negocio
     * @param date Fecha (se ignorará la parte de hora)
     * @return Lista de reservas para el día
     */
    @Query("SELECT b FROM Booking b WHERE b.businessId = :businessId AND DATE(b.startTime) = DATE(:date) ORDER BY b.startTime")
    List<Booking> findByBusinessIdAndDate(@Param("businessId") UUID businessId, @Param("date") LocalDateTime date);

    /**
     * Busca reservas para un día y un profesional específico
     * @param businessId ID del negocio
     * @param professionalId ID del profesional
     * @param date Fecha (se ignorará la parte de hora)
     * @return Lista de reservas para el día y profesional
     */
    @Query("SELECT b FROM Booking b WHERE b.businessId = :businessId AND b.professional.id = :professionalId AND DATE(b.startTime) = DATE(:date) ORDER BY b.startTime")
    List<Booking> findByBusinessIdAndProfessionalIdAndDate(@Param("businessId") UUID businessId, @Param("professionalId") UUID professionalId, @Param("date") LocalDateTime date);

    /**
     * Busca reservas por código de evento de Google Calendar
     * @param googleEventId ID del evento en Google Calendar
     * @return Reserva si existe
     */
    Optional<Booking> findByGoogleEventId(String googleEventId);

    /**
     * Busca reservas que necesitan recordatorio
     * @param from Fecha desde
     * @param to Fecha hasta
     * @return Lista de reservas que necesitan recordatorio
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.startTime BETWEEN :from AND :to")
    List<Booking> findNeedingReminder(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Busca reservas superpuestas para un profesional
     * @param professionalId ID del profesional
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @param bookingIdToExclude ID de reserva a excluir (para ediciones)
     * @return Lista de reservas superpuestas
     */
    @Query("SELECT b FROM Booking b WHERE b.professional.id = :professionalId AND b.id != :excludeId AND b.status NOT IN ('CANCELED', 'NO_SHOW') " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findOverlappingForProfessional(
            @Param("professionalId") UUID professionalId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") UUID bookingIdToExclude);

    /**
     * Busca reservas superpuestas para un recurso
     * @param resourceId ID del recurso
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @param bookingIdToExclude ID de reserva a excluir (para ediciones)
     * @return Lista de reservas superpuestas
     */
    @Query("SELECT b FROM Booking b WHERE b.resource.id = :resourceId AND b.id != :excludeId AND b.status NOT IN ('CANCELED', 'NO_SHOW') " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findOverlappingForResource(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") UUID bookingIdToExclude);

    /**
     * Cuenta reservas por negocio y estado
     * @param businessId ID del negocio
     * @param status Estado de reserva
     * @return Número de reservas
     */
    long countByBusinessIdAndStatus(UUID businessId, Booking.BookingStatus status);

    /**
     * Cuenta reservas por día para un negocio
     * @param businessId ID del negocio
     * @param date Fecha 
     * @return Número de reservas en ese día
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.businessId = :businessId AND DATE(b.startTime) = DATE(:date)")
    long countByBusinessIdAndDate(@Param("businessId") UUID businessId, @Param("date") LocalDateTime date);

    /**
     * Encuentra las próximas reservas de un cliente
     * @param customerId ID del cliente
     * @param now Fecha actual
     * @param pageable Paginación
     * @return Página de próximas reservas
     */
    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId AND b.startTime > :now AND b.status NOT IN ('CANCELED', 'NO_SHOW') ORDER BY b.startTime")
    Page<Booking> findUpcomingByCustomer(@Param("customerId") UUID customerId, @Param("now") LocalDateTime now, Pageable pageable);
}