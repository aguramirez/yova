package com.bookingsaas.module.notification.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que representa las preferencias de notificación de un cliente
 */
@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NotificationPreference extends AuditableEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "channel_type", nullable = false)
    private String channelType;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    /**
     * Comprueba si las notificaciones están habilitadas para este canal
     * @return true si están habilitadas
     */
    @Transient
    public boolean isNotificationsEnabled() {
        return isEnabled;
    }

    /**
     * Habilita las notificaciones para este canal
     */
    public void enableNotifications() {
        this.isEnabled = true;
    }

    /**
     * Deshabilita las notificaciones para este canal
     */
    public void disableNotifications() {
        this.isEnabled = false;
    }
}