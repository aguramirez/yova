package com.bookingsaas.module.booking.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un canje de recompensa por puntos
 */
@Entity
@Table(name = "reward_redemptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RewardRedemption extends AuditableEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "reward_item_id", nullable = false)
    private UUID rewardItemId;

    @Column(name = "points_spent", nullable = false)
    private Integer pointsSpent;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RedemptionStatus status;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "redemption_code")
    private String redemptionCode;

    /**
     * Estado del canje de recompensa
     */
    public enum RedemptionStatus {
        PENDING,    // Pendiente de utilizar/entregar
        COMPLETED,  // Utilizada/entregada
        CANCELED    // Cancelada
    }

    /**
     * Marca el canje como completado
     * @return true si se pudo completar
     */
    public boolean complete() {
        if (status != RedemptionStatus.PENDING) {
            return false;
        }
        
        this.status = RedemptionStatus.COMPLETED;
        return true;
    }

    /**
     * Cancela el canje
     * @return true si se pudo cancelar
     */
    public boolean cancel() {
        if (status != RedemptionStatus.PENDING) {
            return false;
        }
        
        this.status = RedemptionStatus.CANCELED;
        return true;
    }

    /**
     * Comprueba si el canje está pendiente (no utilizado ni cancelado)
     * @return true si está pendiente
     */
    @Transient
    public boolean isPending() {
        return status == RedemptionStatus.PENDING;
    }

    /**
     * Comprueba si el canje ha sido completado
     * @return true si está completado
     */
    @Transient
    public boolean isCompleted() {
        return status == RedemptionStatus.COMPLETED;
    }

    /**
     * Comprueba si el canje está cancelado
     * @return true si está cancelado
     */
    @Transient
    public boolean isCanceled() {
        return status == RedemptionStatus.CANCELED;
    }

    /**
     * Genera un código único de redención
     * @return Código generado
     */
    public String generateRedemptionCode() {
        // Combinación de timestamp, ID de cliente e ID de recompensa para generar un código único
        String baseString = System.currentTimeMillis() + "-" + customerId + "-" + rewardItemId;
        
        // Convertir a un hash y tomar los primeros 8 caracteres
        int hashCode = baseString.hashCode();
        String code = Integer.toHexString(hashCode).toUpperCase();
        
        // Asegurar que tenga al menos 6 caracteres
        while (code.length() < 6) {
            code = "0" + code;
        }
        
        // Tomar los primeros 8 caracteres o todo si es más corto
        code = code.substring(0, Math.min(code.length(), 8));
        
        this.redemptionCode = code;
        return code;
    }

    /**
     * Comprueba si el canje ha expirado
     * @return true si la fecha de expiración ha pasado
     */
    @Transient
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}