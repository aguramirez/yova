package com.bookingsaas.module.booking.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * Entidad que representa un artículo de recompensa canjeable por puntos
 */
@Entity
@Table(name = "reward_items")
@SQLDelete(sql = "UPDATE reward_items SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RewardItem extends AuditableEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "points_cost", nullable = false)
    private Integer pointsCost;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RewardType type;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RewardStatus status;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "limit_per_customer")
    private Integer limitPerCustomer;

    /**
     * Tipo de recompensa
     */
    public enum RewardType {
        DISCOUNT,    // Descuento en servicio
        SERVICE,     // Servicio gratuito
        PRODUCT,     // Producto físico
        EVENT,       // Acceso a evento
        MEMBERSHIP   // Mejora de membresía
    }

    /**
     * Estado de la recompensa
     */
    public enum RewardStatus {
        ACTIVE,     // Activo y disponible
        INACTIVE,   // Temporalmente no disponible
        DRAFT       // En preparación, no visible
    }

    /**
     * Comprueba si la recompensa está disponible para canje
     * @return true si está activa y tiene stock
     */
    @Transient
    public boolean isAvailable() {
        if (status != RewardStatus.ACTIVE) {
            return false;
        }
        
        // Si no se controla el stock, siempre está disponible
        if (stock == null) {
            return true;
        }
        
        return stock > 0;
    }

    /**
     * Actualiza el stock cuando se canjea una recompensa
     * @return true si hay stock disponible y se pudo actualizar
     */
    public boolean decrementStock() {
        if (stock == null) {
            return true; // Si no se controla el stock, siempre exitoso
        }
        
        if (stock <= 0) {
            return false;
        }
        
        stock--;
        
        // Si el stock llega a cero, marcar como inactivo
        if (stock == 0) {
            status = RewardStatus.INACTIVE;
        }
        
        return true;
    }

    /**
     * Incrementa el stock de la recompensa
     * @param quantity Cantidad a aumentar
     */
    public void incrementStock(int quantity) {
        if (stock == null) {
            stock = quantity;
        } else {
            stock += quantity;
        }
        
        // Si se aumenta el stock de un item inactivo, activarlo
        if (status == RewardStatus.INACTIVE && stock > 0) {
            status = RewardStatus.ACTIVE;
        }
    }
}