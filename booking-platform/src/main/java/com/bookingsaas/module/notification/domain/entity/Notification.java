package com.bookingsaas.module.notification.domain.entity;

import com.bookingsaas.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa una notificación enviada
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Notification extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "professional_id")
    private UUID professionalId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "subject")
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    /**
     * Estado de la notificación
     */
    public enum NotificationStatus {
        PENDING,    // Pendiente de envío
        SENT,       // Enviada al proveedor
        FAILED,     // Falló el envío
        DELIVERED,  // Entregada al destinatario
        READ        // Leída por el destinatario
    }

    /**
     * Canales de notificación
     */
    public static final class NotificationChannels {
        public static final String EMAIL = "EMAIL";
        public static final String SMS = "SMS";
        public static final String PUSH = "PUSH";
        public static final String IN_APP = "IN_APP";
        
        private NotificationChannels() {
            // Constructor privado para evitar instanciación
        }
    }

    /**
     * Marca la notificación como enviada
     * @param externalId ID proporcionado por el proveedor de notificaciones
     */
    public void markAsSent(String externalId) {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.externalId = externalId;
    }

    /**
     * Marca la notificación como entregada
     */
    public void markAsDelivered() {
        this.status = NotificationStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    /**
     * Marca la notificación como leída
     */
    public void markAsRead() {
        this.status = NotificationStatus.READ;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Marca la notificación como fallida
     * @param errorMessage Mensaje de error
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * Comprueba si la notificación está pendiente
     * @return true si está pendiente
     */
    @Transient
    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    /**
     * Comprueba si la notificación fue enviada exitosamente
     * @return true si fue enviada
     */
    @Transient
    public boolean isSent() {
        return status == NotificationStatus.SENT ||
               status == NotificationStatus.DELIVERED ||
               status == NotificationStatus.READ;
    }

    /**
     * Comprueba si la notificación falló
     * @return true si falló
     */
    @Transient
    public boolean isFailed() {
        return status == NotificationStatus.FAILED;
    }

    /**
     * Comprueba si la notificación fue leída
     * @return true si fue leída
     */
    @Transient
    public boolean isRead() {
        return status == NotificationStatus.READ;
    }
}