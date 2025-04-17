package com.bookingsaas.module.booking.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad que representa un recurso físico que se puede reservar (sala, equipo, etc.)
 */
@Entity
@Table(name = "resources")
@SQLDelete(sql = "UPDATE resources SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Resource extends AuditableEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "location")
    private String location;

    @Column(name = "active", nullable = false)
    private boolean active;
    
    @OneToMany(mappedBy = "resource", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Booking> bookings = new HashSet<>();

    /**
     * Comprueba si el recurso está disponible en un rango de tiempo específico
     * @param startTime Tiempo de inicio
     * @param endTime Tiempo de fin
     * @return true si el recurso está disponible, false si ya está reservado en ese período
     */
    public boolean isAvailable(LocalDateTime startTime, LocalDateTime endTime) {
        // Si el recurso no está activo, no está disponible
        if (!active) {
            return false;
        }
        
        // Verificar si hay alguna reserva que se solape con el rango solicitado
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
     * Tipos de recursos comunes
     */
    public static final class ResourceTypes {
        public static final String ROOM = "ROOM";
        public static final String EQUIPMENT = "EQUIPMENT";
        public static final String VEHICLE = "VEHICLE";
        public static final String SPACE = "SPACE";
        public static final String OTHER = "OTHER";

        private ResourceTypes() {
            // Constructor privado para evitar instanciación
        }
    }
}