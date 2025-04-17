package com.bookingsaas.module.booking.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad que representa a un profesional que proporciona servicios
 */
@Entity
@Table(name = "professionals")
@SQLDelete(sql = "UPDATE professionals SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Professional extends AuditableEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "google_calendar_id")
    private String googleCalendarId;

    @Column(name = "attendance_rate", precision = 5, scale = 2)
    private BigDecimal attendanceRate;

    @Column(name = "average_service_time")
    private Integer averageServiceTime;
    
    @OneToMany(mappedBy = "professional", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Booking> bookings = new HashSet<>();

    /**
     * Comprueba si el profesional está disponible en un rango de tiempo específico
     * @param startTime Tiempo de inicio
     * @param endTime Tiempo de fin
     * @return true si el profesional está disponible, false si ya tiene alguna reserva en ese período
     */
    public boolean isAvailable(LocalDateTime startTime, LocalDateTime endTime) {
        // Implementación básica que verifica conflictos con reservas existentes
        return bookings.stream()
                .filter(booking -> !booking.isCancelled())
                .noneMatch(booking -> {
                    LocalDateTime bookingStart = booking.getStartTime();
                    LocalDateTime bookingEnd = booking.getEndTime();
                    
                    // Verifica si hay solapamiento entre el rango solicitado y una reserva existente
                    return (startTime.isBefore(bookingEnd) && endTime.isAfter(bookingStart));
                });
    }

    /**
     * Actualiza la tasa de asistencia promedio basada en una nueva asistencia
     * @param attended true si el cliente asistió, false si no
     */
    public void updateAttendanceRate(boolean attended) {
        // Si no hay valor previo, inicializar
        if (attendanceRate == null) {
            attendanceRate = attended ? new BigDecimal("100.00") : BigDecimal.ZERO;
            return;
        }
        
        // Calcular un nuevo valor con peso, dando más importancia a las asistencias recientes
        // Este es un enfoque simple; en producción se podría usar un algoritmo más sofisticado
        BigDecimal currentRate = attendanceRate;
        BigDecimal newRateValue = attended ? new BigDecimal("100.00") : BigDecimal.ZERO;
        
        // Factor de peso para el nuevo valor (30% nuevo, 70% histórico)
        BigDecimal weightFactor = new BigDecimal("0.3");
        
        // Calcular: newRate = currentRate * 0.7 + newValue * 0.3
        BigDecimal updatedRate = currentRate.multiply(BigDecimal.ONE.subtract(weightFactor))
                .add(newRateValue.multiply(weightFactor));
        
        // Establecer con precisión de 2 decimales
        this.attendanceRate = updatedRate.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Actualiza el tiempo de servicio promedio basado en una nueva atención
     * @param serviceTimeMinutes Duración de la atención en minutos
     */
    public void updateAverageServiceTime(int serviceTimeMinutes) {
        // Si no hay valor previo, usar el nuevo directamente
        if (averageServiceTime == null) {
            averageServiceTime = serviceTimeMinutes;
            return;
        }
        
        // Calcular un promedio ponderado similar al de la tasa de asistencia
        int currentAverage = averageServiceTime;
        
        // Factor de peso para el nuevo valor (20% nuevo, 80% histórico)
        double weightFactor = 0.2;
        
        // Calcular: newAvg = currentAvg * 0.8 + newValue * 0.2
        double updatedAverage = currentAverage * (1 - weightFactor) + serviceTimeMinutes * weightFactor;
        
        // Redondear al entero más cercano
        this.averageServiceTime = (int) Math.round(updatedAverage);
    }
}