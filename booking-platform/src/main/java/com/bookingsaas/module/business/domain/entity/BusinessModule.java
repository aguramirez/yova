package com.bookingsaas.module.business.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Entidad que representa un módulo activado para un negocio
 */
@Entity
@Table(name = "business_modules", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BusinessModule extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "settings", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode settings;

    /**
     * Constantes para los nombres de módulos
     */
    public static final class ModuleNames {
        public static final String LOYALTY = "loyalty";
        public static final String PAYMENT = "payment";
        public static final String NOTIFICATION = "notification";
        public static final String DOCUMENT = "document";
        public static final String ANALYTICS = "analytics";

        private ModuleNames() {
            // Constructor privado para evitar instanciación
        }
    }
}