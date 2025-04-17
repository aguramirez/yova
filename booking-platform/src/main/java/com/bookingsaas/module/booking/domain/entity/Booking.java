package com.bookingsaas.module.booking.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa una reserva o cita
 */
@Entity
@Table(name = "bookings")
@SQLDelete(sql = "UPDATE bookings SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Booking extends AuditableEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id")
    private Professional professional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(name = "attendance_code")
    private String attendanceCode;

    @Column(name = "attendance_validated", nullable = false)
    private boolean attendanceValidated;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "notes")
    private String notes;

    /**
     * Estado de la reserva
     */
    public enum BookingStatus {
        PENDING,    // Pendiente de confirmación
        CONFIRMED,  // Confirmada
        CANCELED,   // Cancelada
        COMPLETED,  // Completada (atendida)
        NO_SHOW     // No se presentó
    }

    /**
     * Comprueba si la reserva está cancelada
     * @return true si está cancelada
     */
    public boolean isCancelled() {
        return status == BookingStatus.CANCELED;
    }

    /**
     * Comprueba si la reserva está completada (atendida)
     * @return true si está completada
     */
    public boolean isCompleted() {
        return status == BookingStatus.COMPLETED;
    }

    /**
     * Comprueba si el cliente no se presentó
     * @return true si está marcada como no-show
     */
    public boolean isNoShow() {
        return status == BookingStatus.NO_SHOW;
    }

    /**
     * Marca la reserva como completada (atendida)
     * @param validatedBy ID del usuario que valida la asistencia
     */
    public void completeBooking(UUID validatedBy) {
        this.status = BookingStatus.COMPLETED;
        this.attendanceValidated = true;
        this.validatedAt = LocalDateTime.now();
        this.validatedBy = validatedBy;
        
        // Actualizar estadísticas del cliente
        if (customer != null) {
            customer.recordAttendance();
        }
        
        // Actualizar estadísticas del profesional
        if (professional != null) {
            professional.updateAttendanceRate(true);
        }
    }

    /**
     * Marca la reserva como no-show (cliente no se presentó)
     * @param validatedBy ID del usuario que registra la no-asistencia
     */
    public void markAsNoShow(UUID validatedBy) {
        this.status = BookingStatus.NO_SHOW;
        this.validatedAt = LocalDateTime.now();
        this.validatedBy = validatedBy;
        
        // Actualizar estadísticas del profesional
        if (professional != null) {
            professional.updateAttendanceRate(false);
        }
    }

    /**
     * Cancela la reserva
     */
    public void cancelBooking() {
        this.status = BookingStatus.CANCELED;
    }

    /**
     * Confirma la reserva
     */
    public void confirmBooking() {
        this.status = BookingStatus.CONFIRMED;
    }

    /**
     * Genera un código de asistencia
     * @return Código alfanumérico único
     */
    public String generateAttendanceCode() {
        // Combinación de timestamp, ID de reserva e ID de cliente para generar un código único
        String baseString = System.currentTimeMillis() + "-" + getId() + "-" + customer.getId();
        
        // Convertir a un hash y tomar los primeros 8 caracteres
        int hashCode = baseString.hashCode();
        String code = Integer.toHexString(hashCode).toUpperCase();
        
        // Asegurar que tenga al menos 6 caracteres
        while (code.length() < 6) {
            code = "0" + code;
        }
        
        // Tomar los primeros 8 caracteres o todo si es más corto
        code = code.substring(0, Math.min(code.length(), 8));
        
        this.attendanceCode = code;
        return code;
    }

    /**
     * Obtiene la duración de la cita en minutos
     * @return Duración en minutos
     */
    @Transient
    public long getDurationMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Valida que el rango de tiempo sea coherente
     * @return true si el tiempo de inicio es anterior al de fin
     */
    @Transient
    public boolean isTimeRangeValid() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }
}