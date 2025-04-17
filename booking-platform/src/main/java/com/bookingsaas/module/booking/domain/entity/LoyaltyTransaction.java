package com.bookingsaas.module.booking.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa una transacción de puntos de fidelidad
 */
@Entity
@Table(name = "loyalty_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LoyaltyTransaction extends AuditableEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded;

    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Tipo de transacción de puntos
     */
    public enum TransactionType {
        BOOKING,     // Puntos por asistencia a cita
        REFERRAL,    // Puntos por referir a un amigo
        REDEMPTION,  // Puntos gastados en recompensas
        MANUAL,      // Ajuste manual de puntos
        EXPIRATION   // Puntos expirados
    }

    /**
     * Comprueba si la transacción es positiva (otorga puntos)
     * @return true si otorga puntos
     */
    @Transient
    public boolean isPositive() {
        return transactionType == TransactionType.BOOKING ||
               transactionType == TransactionType.REFERRAL ||
               (transactionType == TransactionType.MANUAL && pointsAwarded > 0);
    }

    /**
     * Comprueba si la transacción es negativa (resta puntos)
     * @return true si resta puntos
     */
    @Transient
    public boolean isNegative() {
        return transactionType == TransactionType.REDEMPTION ||
               transactionType == TransactionType.EXPIRATION ||
               (transactionType == TransactionType.MANUAL && pointsAwarded < 0);
    }

    /**
     * Comprueba si los puntos han expirado
     * @return true si la fecha de expiración ha pasado
     */
    @Transient
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Calcula los días restantes hasta la expiración
     * @return Días restantes o 0 si ya ha expirado
     */
    @Transient
    public long getDaysUntilExpiration() {
        if (expiresAt == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiresAt)) {
            return 0;
        }
        
        return java.time.Duration.between(now, expiresAt).toDays();
    }
}