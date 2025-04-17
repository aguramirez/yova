package com.bookingsaas.module.business.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad que representa un negocio en el sistema
 */
@Entity
@Table(name = "businesses", schema = "public")
@SQLDelete(sql = "UPDATE public.businesses SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Business extends AuditableEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private BusinessType type;

    @Column(name = "contact_email", nullable = false, unique = true)
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(name = "subscription_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus subscriptionStatus;

    @Column(name = "subscription_plan", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<BusinessModule> modules = new HashSet<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<BusinessLocation> locations = new HashSet<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<BusinessSetting> settings = new HashSet<>();

    /**
     * Comprueba si un módulo específico está activo para este negocio
     * @param moduleName Nombre del módulo a comprobar
     * @return true si el módulo está activo, false en caso contrario
     */
    public boolean isModuleActive(String moduleName) {
        return modules.stream()
                .filter(module -> module.getModuleName().equals(moduleName))
                .findFirst()
                .map(BusinessModule::isActive)
                .orElse(false);
    }

    /**
     * Activa un módulo para este negocio
     * @param moduleName Nombre del módulo a activar
     */
    public void activateModule(String moduleName) {
        BusinessModule module = findOrCreateModule(moduleName);
        module.setActive(true);
    }

    /**
     * Desactiva un módulo para este negocio
     * @param moduleName Nombre del módulo a desactivar
     */
    public void deactivateModule(String moduleName) {
        BusinessModule module = findOrCreateModule(moduleName);
        module.setActive(false);
    }

    /**
     * Busca un módulo por nombre o lo crea si no existe
     * @param moduleName Nombre del módulo
     * @return El módulo encontrado o creado
     */
    private BusinessModule findOrCreateModule(String moduleName) {
        return modules.stream()
                .filter(m -> m.getModuleName().equals(moduleName))
                .findFirst()
                .orElseGet(() -> {
                    BusinessModule newModule = BusinessModule.builder()
                            .business(this)
                            .moduleName(moduleName)
                            .active(false)
                            .build();
                    modules.add(newModule);
                    return newModule;
                });
    }

    /**
     * Obtiene el esquema de base de datos asociado a este negocio
     * @return Nombre del esquema
     */
    public String getSchemaName() {
        return "business_" + getId().toString().replace("-", "_");
    }

    /**
     * Obtiene la zona horaria como ZoneId
     * @return ZoneId configurado para el negocio
     */
    public ZoneId getZoneId() {
        return ZoneId.of(timeZone);
    }

    /**
     * Tipo de negocio
     */
    public enum BusinessType {
        SALON,      // Peluquería, estética
        HEALTHCARE, // Consultorio médico, dental, etc.
        RESTAURANT, // Restaurantes
        HOTEL,      // Hoteles y alojamientos
        GYM,        // Gimnasios y centros deportivos
        EDUCATION,  // Centros educativos, tutorías
        PROFESSIONAL, // Servicios profesionales (legal, contable)
        OTHER       // Otros tipos
    }

    /**
     * Estado de la suscripción
     */
    public enum SubscriptionStatus {
        TRIAL,      // Periodo de prueba
        ACTIVE,     // Suscripción activa y al día
        SUSPENDED,  // Suscripción suspendida (pago pendiente)
        CANCELED    // Suscripción cancelada
    }

    /**
     * Plan de suscripción
     */
    public enum SubscriptionPlan {
        BASIC,      // Plan básico
        STANDARD,   // Plan estándar
        PREMIUM     // Plan premium
    }
}