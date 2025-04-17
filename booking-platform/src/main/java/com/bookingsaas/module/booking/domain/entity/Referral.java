package com.bookingsaas.module.booking.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa una referencia de un cliente a otro
 */
@Entity
@Table(name = "referrals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Referral extends AuditableEntity {

    @Column(name = "referrer_customer_id", nullable = false)
    private UUID referrerCustomerId;

    @Column(name = "referred_customer_id", nullable = false)
    private UUID referredCustomerId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReferralStatus status;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "points_awarded")
    private Integer pointsAwarded;

    /**
     * Estado de la referencia
     */
    public enum ReferralStatus {
        PENDING,    // Pendiente (el referido aún no ha completado una cita)
        CONVERTED,  // Convertida (el referido completó una cita)
        EXPIRED,    // Expirada (pasó el tiempo límite sin conversión)
        CANCELED    // Cancelada (por alguna razón administrativa)
    }

    /**
     * Convierte la referencia cuando el cliente referido asiste a su primera cita
     * @param pointsToAward Puntos a otorgar al referente
     * @return true si la conversión fue exitosa
     */
    public boolean convert(int pointsToAward) {
        if (status != ReferralStatus.PENDING) {
            return false;
        }
        
        this.status = ReferralStatus.CONVERTED;
        this.convertedAt = LocalDateTime.now();
        this.pointsAwarded = pointsToAward;
        return true;
    }

    /**
     * Marca la referencia como expirada
     * @return true si se pudo marcar como expirada
     */
    public boolean expire() {
        if (status != ReferralStatus.PENDING) {
            return false;
        }
        
        this.status = ReferralStatus.EXPIRED;
        return true;
    }

    /**
     * Cancela la referencia
     * @return true si se pudo cancelar
     */
    public boolean cancel() {
        if (status == ReferralStatus.CONVERTED) {
            return false; // No se puede cancelar una referencia ya convertida
        }
        
        this.status = ReferralStatus.CANCELED;
        return true;
    }

    /**
     * Comprueba si la referencia está activa (pendiente y no expirada)
     * @return true si está activa
     */
    @Transient
    public boolean isActive() {
        return status == ReferralStatus.PENDING;
    }

    /**
     * Comprueba si la referencia se ha convertido exitosamente
     * @return true si está convertida
     */
    @Transient
    public boolean isConverted() {
        return status == ReferralStatus.CONVERTED;
    }
}