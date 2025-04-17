package com.bookingsaas.module.business.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que representa una configuración específica de un negocio
 */
@Entity
@Table(name = "business_settings", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BusinessSetting extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "setting_key", nullable = false)
    private String settingKey;

    @Column(name = "setting_value")
    private String settingValue;

    /**
     * Constantes para las claves de configuración comunes
     */
    public static final class SettingKeys {
        // Configuración general
        public static final String LOGO_URL = "general.logo_url";
        public static final String BRAND_COLOR = "general.brand_color";
        public static final String SECONDARY_COLOR = "general.secondary_color";
        public static final String BUSINESS_HOURS = "general.business_hours";
        public static final String DEFAULT_APPOINTMENT_DURATION = "general.default_appointment_duration";
        public static final String BUFFER_TIME_MINUTES = "general.buffer_time_minutes";

        // Configuración de reservas
        public static final String BOOKING_LEAD_TIME_MINUTES = "booking.lead_time_minutes";
        public static final String BOOKING_FUTURE_DAYS = "booking.future_days";
        public static final String ALLOW_CUSTOMER_CANCELLATION = "booking.allow_customer_cancellation";
        public static final String CANCELLATION_LEAD_TIME_MINUTES = "booking.cancellation_lead_time_minutes";
        public static final String REMINDER_TIME_HOURS = "booking.reminder_time_hours";

        // Configuración de fidelización
        public static final String LOYALTY_POINTS_PER_BOOKING = "loyalty.points_per_booking";
        public static final String LOYALTY_POINTS_EXPIRATION_DAYS = "loyalty.points_expiration_days";
        public static final String LOYALTY_REFERRAL_POINTS = "loyalty.referral_points";
        public static final String LOYALTY_FIRST_BOOKING_BONUS = "loyalty.first_booking_bonus";
        public static final String LOYALTY_BIRTHDAY_BONUS = "loyalty.birthday_bonus";

        private SettingKeys() {
            // Constructor privado para evitar instanciación
        }
    }

    /**
     * Convierte el valor a entero, o devuelve un valor por defecto si no es posible
     * @param defaultValue Valor por defecto
     * @return El valor como entero o el valor por defecto
     */
    public Integer getValueAsInteger(Integer defaultValue) {
        if (settingValue == null || settingValue.isBlank()) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(settingValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Convierte el valor a booleano, o devuelve un valor por defecto si no es posible
     * @param defaultValue Valor por defecto
     * @return El valor como booleano o el valor por defecto
     */
    public Boolean getValueAsBoolean(Boolean defaultValue) {
        if (settingValue == null || settingValue.isBlank()) {
            return defaultValue;
        }
        
        return "true".equalsIgnoreCase(settingValue) || 
               "yes".equalsIgnoreCase(settingValue) || 
               "1".equals(settingValue);
    }
}