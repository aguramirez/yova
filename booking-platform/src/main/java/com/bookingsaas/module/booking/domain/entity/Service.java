package com.bookingsaas.module.booking.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad que representa un servicio ofrecido por un negocio
 */
@Entity
@Table(name = "services")
@SQLDelete(sql = "UPDATE services SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Service extends AuditableEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "category_id")
    private UUID categoryId;

    /**
     * Calcula el tiempo de finalización a partir de un tiempo de inicio
     * @param startTime Tiempo de inicio
     * @return Tiempo de finalización basado en la duración del servicio
     */
    public java.time.LocalDateTime calculateEndTime(java.time.LocalDateTime startTime) {
        return startTime.plusMinutes(durationMinutes);
    }

    /**
     * Calcula el precio con descuento
     * @param discountPercentage Porcentaje de descuento (0-100)
     * @return Precio con descuento aplicado
     */
    public BigDecimal calculateDiscountedPrice(int discountPercentage) {
        if (discountPercentage <= 0) {
            return price;
        }
        
        if (discountPercentage >= 100) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discountFactor = BigDecimal.ONE.subtract(
                new BigDecimal(discountPercentage).divide(new BigDecimal("100")));
        
        return price.multiply(discountFactor).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}