package com.bookingsaas.module.notification.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entidad que representa una plantilla para notificaciones
 */
@Entity
@Table(name = "notification_templates")
@SQLDelete(sql = "UPDATE notification_templates SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NotificationTemplate extends AuditableEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "subject")
    private String subject;

    @Column(name = "content_template", nullable = false, columnDefinition = "TEXT")
    private String contentTemplate;

    /**
     * Tipos de notificación comunes
     */
    public static final class NotificationTypes {
        public static final String BOOKING_CONFIRMATION = "BOOKING_CONFIRMATION";
        public static final String REMINDER = "REMINDER";
        public static final String CANCELLATION = "CANCELLATION";
        public static final String LOYALTY_POINTS = "LOYALTY_POINTS";
        public static final String REFERRAL_CONVERTED = "REFERRAL_CONVERTED";
        public static final String POINTS_EXPIRING = "POINTS_EXPIRING";
        public static final String REWARD_REDEEMED = "REWARD_REDEEMED";
        public static final String PAYMENT_CONFIRMATION = "PAYMENT_CONFIRMATION";
        public static final String INVOICE_ISSUED = "INVOICE_ISSUED";
        
        private NotificationTypes() {
            // Constructor privado para evitar instanciación
        }
    }

    /**
     * Procesa la plantilla reemplazando variables con valores
     * @param variables Mapa de variables y sus valores
     * @return Contenido con variables reemplazadas
     */
    public String processTemplate(Map<String, Object> variables) {
        String processedContent = contentTemplate;
        
        // Buscar variables en el formato {{variable.propiedad}}
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(processedContent);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1).trim();
            String replacement = getVariableValue(placeholder, variables);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    /**
     * Procesa el asunto reemplazando variables con valores
     * @param variables Mapa de variables y sus valores
     * @return Asunto con variables reemplazadas
     */
    public String processSubject(Map<String, Object> variables) {
        if (subject == null || subject.isBlank()) {
            return "";
        }
        
        String processedSubject = subject;
        
        // Buscar variables en el formato {{variable.propiedad}}
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(processedSubject);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1).trim();
            String replacement = getVariableValue(placeholder, variables);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    /**
     * Obtiene el valor de una variable a partir de una ruta (por ejemplo "customer.firstName")
     * @param path Ruta de la variable
     * @param variables Mapa de variables
     * @return Valor de la variable o placeholder si no se encuentra
     */
    private String getVariableValue(String path, Map<String, Object> variables) {
        String[] parts = path.split("\\.");
        
        if (parts.length < 2) {
            return "{{" + path + "}}"; // Mantener el placeholder si no tiene formato correcto
        }
        
        String objName = parts[0];
        String property = parts[1];
        
        // Buscar el objeto
        Object obj = variables.get(objName);
        if (obj == null) {
            return ""; // Si no se encuentra el objeto, devolver vacío
        }
        
        // Si es un mapa, buscar la propiedad
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            Object value = map.get(property);
            return value != null ? value.toString() : "";
        }
        
        // Intentar acceder a la propiedad por reflexión (implementación básica)
        try {
            String getterMethod = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
            java.lang.reflect.Method method = obj.getClass().getMethod(getterMethod);
            Object value = method.invoke(obj);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return ""; // Si hay error, devolver vacío
        }
    }

    /**
     * Extrae todas las variables usadas en la plantilla
     * @return Lista de variables en formato "objeto.propiedad"
     */
    @Transient
    public java.util.List<String> extractVariables() {
        java.util.List<String> variables = new java.util.ArrayList<>();
        
        // Buscar variables en el formato {{variable.propiedad}}
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher contentMatcher = pattern.matcher(contentTemplate);
        
        while (contentMatcher.find()) {
            String variable = contentMatcher.group(1).trim();
            variables.add(variable);
        }
        
        // También buscar en el asunto si existe
        if (subject != null && !subject.isBlank()) {
            Matcher subjectMatcher = pattern.matcher(subject);
            while (subjectMatcher.find()) {
                String variable = subjectMatcher.group(1).trim();
                if (!variables.contains(variable)) {
                    variables.add(variable);
                }
            }
        }
        
        return variables;
    }

    /**
     * Comprueba si la plantilla contiene una variable específica
     * @param variablePath Ruta de la variable (ej: "customer.firstName")
     * @return true si la plantilla contiene la variable
     */
    @Transient
    public boolean containsVariable(String variablePath) {
        return extractVariables().contains(variablePath);
    }

    /**
     * Crea un mapa de variables vacío con la estructura básica esperada
     * @return Mapa de variables con estructura básica
     */
    @Transient
    public Map<String, Object> createEmptyVariablesMap() {
        Map<String, Object> variables = new HashMap<>();
        
        // Objetos comunes en plantillas
        variables.put("business", new HashMap<String, Object>());
        variables.put("customer", new HashMap<String, Object>());
        variables.put("booking", new HashMap<String, Object>());
        variables.put("service", new HashMap<String, Object>());
        variables.put("professional", new HashMap<String, Object>());
        variables.put("payment", new HashMap<String, Object>());
        variables.put("loyalty", new HashMap<String, Object>());
        variables.put("referral", new HashMap<String, Object>());
        
        return variables;
    }
}