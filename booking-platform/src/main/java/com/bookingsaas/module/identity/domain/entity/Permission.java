package com.bookingsaas.module.identity.domain.entity;

import com.bookingsaas.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Entidad que representa un permiso en el sistema
 */
@Entity
@Table(name = "permissions", schema = "public")
@SQLDelete(sql = "UPDATE public.permissions SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    /**
     * Constantes para los nombres de permisos comunes
     */
    public static final class PermissionNames {
        // Permisos de negocio
        public static final String BUSINESS_CREATE = "BUSINESS_CREATE";
        public static final String BUSINESS_READ = "BUSINESS_READ";
        public static final String BUSINESS_UPDATE = "BUSINESS_UPDATE";
        public static final String BUSINESS_DELETE = "BUSINESS_DELETE";
        
        // Permisos de usuario
        public static final String USER_CREATE = "USER_CREATE";
        public static final String USER_READ = "USER_READ";
        public static final String USER_UPDATE = "USER_UPDATE";
        public static final String USER_DELETE = "USER_DELETE";
        
        // Permisos de booking
        public static final String BOOKING_CREATE = "BOOKING_CREATE";
        public static final String BOOKING_READ = "BOOKING_READ";
        public static final String BOOKING_UPDATE = "BOOKING_UPDATE";
        public static final String BOOKING_DELETE = "BOOKING_DELETE";
        public static final String BOOKING_VALIDATE = "BOOKING_VALIDATE";
        
        // Permisos de cliente
        public static final String CUSTOMER_CREATE = "CUSTOMER_CREATE";
        public static final String CUSTOMER_READ = "CUSTOMER_READ";
        public static final String CUSTOMER_UPDATE = "CUSTOMER_UPDATE";
        public static final String CUSTOMER_DELETE = "CUSTOMER_DELETE";
        
        // Permisos de profesional
        public static final String PROFESSIONAL_CREATE = "PROFESSIONAL_CREATE";
        public static final String PROFESSIONAL_READ = "PROFESSIONAL_READ";
        public static final String PROFESSIONAL_UPDATE = "PROFESSIONAL_UPDATE";
        public static final String PROFESSIONAL_DELETE = "PROFESSIONAL_DELETE";
        
        // Permisos de servicio
        public static final String SERVICE_CREATE = "SERVICE_CREATE";
        public static final String SERVICE_READ = "SERVICE_READ";
        public static final String SERVICE_UPDATE = "SERVICE_UPDATE";
        public static final String SERVICE_DELETE = "SERVICE_DELETE";
        
        // Permisos de pagos
        public static final String PAYMENT_PROCESS = "PAYMENT_PROCESS";
        public static final String PAYMENT_REFUND = "PAYMENT_REFUND";
        public static final String PAYMENT_READ = "PAYMENT_READ";
        
        // Permisos de lealtad
        public static final String LOYALTY_MANAGE = "LOYALTY_MANAGE";
        public static final String LOYALTY_READ = "LOYALTY_READ";
        public static final String LOYALTY_POINTS_ADJUST = "LOYALTY_POINTS_ADJUST";
        
        // Permisos de reportes
        public static final String REPORT_ACCESS = "REPORT_ACCESS";
        public static final String REPORT_EXPORT = "REPORT_EXPORT";
        
        // Permisos administrativos
        public static final String SETTINGS_MANAGE = "SETTINGS_MANAGE";
        public static final String MODULE_MANAGE = "MODULE_MANAGE";

        private PermissionNames() {
            // Constructor privado para evitar instanciación
        }
    }
}