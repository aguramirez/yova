package com.bookingsaas.module.booking.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad que representa a un cliente de un negocio
 */
@Entity
@Table(name = "customers")
@SQLDelete(sql = "UPDATE customers SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Customer extends AuditableEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints;

    @Column(name = "referral_code")
    private String referralCode;

    @Column(name = "total_appointments", nullable = false)
    private Integer totalAppointments;

    @Column(name = "appointments_attended", nullable = false)
    private Integer appointmentsAttended;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CustomerCustomField> customFields = new HashSet<>();

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Booking> bookings = new HashSet<>();

    /**
     * Obtiene el nombre completo del cliente
     * @return Nombre completo (nombre + apellido)
     */
    @Transient
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        
        if (firstName != null && !firstName.isBlank()) {
            sb.append(firstName);
        }
        
        if (lastName != null && !lastName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName);
        }
        
        return sb.toString();
    }

    /**
     * Agrega puntos de fidelidad al cliente
     * @param points Puntos a agregar
     */
    public void addLoyaltyPoints(int points) {
        if (points > 0) {
            this.loyaltyPoints = (this.loyaltyPoints != null ? this.loyaltyPoints : 0) + points;
        }
    }

    /**
     * Redime puntos de fidelidad del cliente
     * @param points Puntos a redimir
     * @return true si se pudieron redimir los puntos, false si no hay suficientes
     */
    public boolean redeemLoyaltyPoints(int points) {
        if (points <= 0) {
            return false;
        }
        
        int currentPoints = this.loyaltyPoints != null ? this.loyaltyPoints : 0;
        
        if (currentPoints >= points) {
            this.loyaltyPoints = currentPoints - points;
            return true;
        }
        
        return false;
    }

    /**
     * Registra una asistencia a una cita
     */
    public void recordAttendance() {
        this.appointmentsAttended = (this.appointmentsAttended != null ? this.appointmentsAttended : 0) + 1;
    }

    /**
     * Obtiene el valor de un campo personalizado por su definición
     * @param fieldDefinitionId ID de la definición del campo
     * @return Valor del campo personalizado o null si no existe
     */
    public String getCustomFieldValue(UUID fieldDefinitionId) {
        return customFields.stream()
                .filter(cf -> cf.getFieldDefinitionId().equals(fieldDefinitionId))
                .map(CustomerCustomField::getFieldValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Establece el valor de un campo personalizado
     * @param fieldDefinitionId ID de la definición del campo
     * @param value Valor a establecer
     */
    public void setCustomFieldValue(UUID fieldDefinitionId, String value) {
        CustomerCustomField customField = customFields.stream()
                .filter(cf -> cf.getFieldDefinitionId().equals(fieldDefinitionId))
                .findFirst()
                .orElse(null);
        
        if (customField == null) {
            customField = CustomerCustomField.builder()
                    .customer(this)
                    .fieldDefinitionId(fieldDefinitionId)
                    .build();
            customFields.add(customField);
        }
        
        customField.setFieldValue(value);
    }

    /**
     * Calcula la tasa de asistencia del cliente (citas asistidas / citas totales)
     * @return Tasa de asistencia como porcentaje (0-100)
     */
    @Transient
    public double getAttendanceRate() {
        if (totalAppointments == null || totalAppointments == 0) {
            return 0.0;
        }
        
        int attended = appointmentsAttended != null ? appointmentsAttended : 0;
        return (double) attended / totalAppointments * 100.0;
    }
}