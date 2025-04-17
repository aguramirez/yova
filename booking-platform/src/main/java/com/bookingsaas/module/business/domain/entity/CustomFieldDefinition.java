package com.bookingsaas.module.business.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Entidad que representa la definición de un campo personalizado para clientes
 */
@Entity
@Table(name = "custom_field_definitions", schema = "public")
@SQLDelete(sql = "UPDATE public.custom_field_definitions SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CustomFieldDefinition extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "field_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FieldType fieldType;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "possible_values")
    private String possibleValues;

    /**
     * Tipo de campo personalizado
     */
    public enum FieldType {
        TEXT,       // Texto libre
        NUMBER,     // Valor numérico
        DATE,       // Fecha
        DATETIME,   // Fecha y hora
        DROPDOWN,   // Lista desplegable
        CHECKBOX,   // Casilla de verificación
        RADIO,      // Botón de opción
        EMAIL,      // Correo electrónico
        PHONE,      // Teléfono
        URL,        // URL
        TEXTAREA    // Área de texto
    }

    /**
     * Obtiene la lista de valores posibles para campos tipo DROPDOWN o RADIO
     * @return Lista de valores posibles
     */
    @Transient
    public List<String> getPossibleValuesList() {
        if (possibleValues == null || possibleValues.isBlank()) {
            return List.of();
        }
        
        return Arrays.asList(possibleValues.split(","));
    }

    /**
     * Verifica si el campo requiere valores posibles (DROPDOWN, RADIO)
     * @return true si el tipo de campo requiere valores posibles
     */
    @Transient
    public boolean requiresPossibleValues() {
        return fieldType == FieldType.DROPDOWN || fieldType == FieldType.RADIO;
    }

    /**
     * Verifica si un valor es válido para este campo
     * @param value Valor a validar
     * @return true si el valor es válido para el tipo de campo
     */
    public boolean isValidValue(String value) {
        if (value == null || value.isBlank()) {
            return !required;
        }
        
        switch (fieldType) {
            case NUMBER:
                try {
                    Double.parseDouble(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case DATE:
            case DATETIME:
                // La validación específica dependerá del formato esperado
                return true; // Simplificado para este ejemplo
            case DROPDOWN:
            case RADIO:
                return getPossibleValuesList().contains(value);
            case EMAIL:
                return value.matches("^[A-Za-z0-9+_.-]+@(.+)$");
            case PHONE:
                return value.matches("^[0-9+\\-() ]+$");
            case URL:
                return value.matches("^(http|https)://.*$");
            case CHECKBOX:
                return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
            default:
                return true; // TEXT, TEXTAREA aceptan cualquier valor
        }
    }
}