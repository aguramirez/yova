package com.bookingsaas.module.payment.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * Entidad que representa un método de pago guardado para un cliente
 */
@Entity
@Table(name = "payment_methods")
@SQLDelete(sql = "UPDATE payment_methods SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PaymentMethod extends AuditableEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "last_four_digits")
    private String lastFourDigits;

    @Column(name = "expiry_date")
    private String expiryDate;

    @Column(name = "token_id")
    private String tokenId;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    /**
     * Tipos de métodos de pago
     */
    public static final class PaymentMethodType {
        public static final String CREDIT_CARD = "CREDIT_CARD";
        public static final String DEBIT_CARD = "DEBIT_CARD";
        public static final String PAYPAL = "PAYPAL";
        public static final String MERCADO_PAGO = "MERCADO_PAGO";
        public static final String BANK_TRANSFER = "BANK_TRANSFER";
        public static final String CASH = "CASH";
        
        private PaymentMethodType() {
            // Constructor privado para evitar instanciación
        }
    }

    /**
     * Establece este método de pago como predeterminado
     */
    public void setAsDefault() {
        this.isDefault = true;
    }

    /**
     * Genera una descripción legible del método de pago
     * @return Descripción del método de pago
     */
    @Transient
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(formatType());
        
        if (lastFourDigits != null && !lastFourDigits.isBlank()) {
            sb.append(" terminada en ").append(lastFourDigits);
        }
        
        if (expiryDate != null && !expiryDate.isBlank()) {
            sb.append(", vence: ").append(expiryDate);
        }
        
        return sb.toString();
    }

    /**
     * Formatea el tipo de pago a un formato legible
     * @return Tipo de pago formateado
     */
    @Transient
    private String formatType() {
        switch (type) {
            case PaymentMethodType.CREDIT_CARD:
                return "Tarjeta de crédito";
            case PaymentMethodType.DEBIT_CARD:
                return "Tarjeta de débito";
            case PaymentMethodType.PAYPAL:
                return "PayPal";
            case PaymentMethodType.MERCADO_PAGO:
                return "Mercado Pago";
            case PaymentMethodType.BANK_TRANSFER:
                return "Transferencia bancaria";
            case PaymentMethodType.CASH:
                return "Efectivo";
            default:
                return type;
        }
    }

    /**
     * Comprueba si la tarjeta está expirada
     * @return true si está expirada
     */
    @Transient
    public boolean isExpired() {
        if (expiryDate == null || expiryDate.isBlank() || !expiryDate.contains("/")) {
            return false; // No aplicable
        }
        
        try {
            String[] parts = expiryDate.split("/");
            int expMonth = Integer.parseInt(parts[0]);
            int expYear = Integer.parseInt(parts[1]);
            
            // Convertir a formato AAMM
            if (expYear < 100) {
                expYear += 2000;
            }
            
            java.time.YearMonth cardExpiry = java.time.YearMonth.of(expYear, expMonth);
            java.time.YearMonth now = java.time.YearMonth.now();
            
            return cardExpiry.isBefore(now);
        } catch (Exception e) {
            return false; // Error en formato, no podemos determinar
        }
    }
}