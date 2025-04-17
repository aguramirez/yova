package com.bookingsaas.module.payment.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un pago realizado por un cliente
 */
@Entity
@Table(name = "payments")
@SQLDelete(sql = "UPDATE payments SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Payment extends AuditableEntity {

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    /**
     * Estado del pago
     */
    public enum PaymentStatus {
        PENDING,    // Pendiente de procesamiento
        COMPLETED,  // Completado con éxito
        FAILED,     // Falló el procesamiento
        REFUNDED,   // Reembolsado
        CANCELED    // Cancelado antes de procesamiento
    }

    /**
     * Completa el pago exitosamente
     * @param transactionId ID de transacción proporcionado por el gateway de pago
     * @return true si se pudo completar
     */
    public boolean complete(String transactionId) {
        if (status != PaymentStatus.PENDING) {
            return false;
        }
        
        this.status = PaymentStatus.COMPLETED;
        this.transactionId = transactionId;
        this.paymentDate = LocalDateTime.now();
        return true;
    }

    /**
     * Marca el pago como fallido
     * @param errorDetails Detalles del error de pago
     * @return true si se pudo marcar como fallido
     */
    public boolean fail(String errorDetails) {
        if (status != PaymentStatus.PENDING) {
            return false;
        }
        
        this.status = PaymentStatus.FAILED;
        return true;
    }

    /**
     * Reembolsa el pago
     * @return true si se pudo reembolsar
     */
    public boolean refund() {
        if (status != PaymentStatus.COMPLETED) {
            return false;
        }
        
        this.status = PaymentStatus.REFUNDED;
        return true;
    }

    /**
     * Cancela el pago
     * @return true si se pudo cancelar
     */
    public boolean cancel() {
        if (status != PaymentStatus.PENDING) {
            return false;
        }
        
        this.status = PaymentStatus.CANCELED;
        return true;
    }

    /**
     * Comprueba si el pago está completo
     * @return true si está completo
     */
    @Transient
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    /**
     * Comprueba si el pago está pendiente
     * @return true si está pendiente
     */
    @Transient
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    /**
     * Comprueba si el pago ha sido reembolsado
     * @return true si está reembolsado
     */
    @Transient
    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }
}