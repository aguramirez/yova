package com.bookingsaas.module.business.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad que representa una ubicación física de un negocio
 */
@Entity
@Table(name = "business_locations", schema = "public")
@SQLDelete(sql = "UPDATE public.business_locations SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BusinessLocation extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "country")
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    /**
     * Devuelve la dirección completa formateada
     * @return Dirección completa en formato: dirección, ciudad, estado, país, código postal
     */
    @Transient
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        
        if (address != null && !address.isBlank()) {
            sb.append(address);
        }
        
        if (city != null && !city.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        
        if (state != null && !state.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state);
        }
        
        if (country != null && !country.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        
        if (postalCode != null && !postalCode.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(postalCode);
        }
        
        return sb.toString();
    }

    /**
     * Verifica si la ubicación tiene coordenadas geográficas definidas
     * @return true si tanto latitud como longitud están definidas
     */
    @Transient
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}