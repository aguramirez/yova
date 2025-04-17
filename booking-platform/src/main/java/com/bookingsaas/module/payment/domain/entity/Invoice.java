package com.bookingsaas.module.payment.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad que representa una factura generada por un pago
 */
@Entity
@Table(name = "invoices")
@SQLDelete(sql = "UPDATE invoices SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Invoice extends AuditableEntity {

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    /**
     * Estado de la factura
     */
    public enum InvoiceStatus {
        ISSUED,     // Emitida
        PAID,       // Pagada
        OVERDUE,    // Vencida
        CANCELED    // Cancelada
    }

    /**
     * Marca la factura como pagada
     * @return true si se pudo marcar como pagada
     */
    public boolean markAsPaid() {
        if (status == InvoiceStatus.PAID || status == InvoiceStatus.CANCELED) {
            return false;
        }
        
        this.status = InvoiceStatus.PAID;
        return true;
    }

    /**
     * Marca la factura como vencida
     * @return true si se pudo marcar como vencida
     */
    public boolean markAsOverdue() {
        if (status != InvoiceStatus.ISSUED) {
            return false;
        }
        
        this.status = InvoiceStatus.OVERDUE;
        return true;
    }

    /**
     * Cancela la factura
     * @return true si se pudo cancelar
     */
    public boolean cancel() {
        if (status == InvoiceStatus.PAID) {
            return false; // No se puede cancelar una factura pagada
        }
        
        this.status = InvoiceStatus.CANCELED;
        return true;
    }

    /**
     * Comprueba si la factura está pagada
     * @return true si está pagada
     */
    @Transient
    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    /**
     * Comprueba si la factura está vencida
     * @return true si está vencida
     */
    @Transient
    public boolean isOverdue() {
        return status == InvoiceStatus.OVERDUE || 
               (status == InvoiceStatus.ISSUED && LocalDate.now().isAfter(dueDate));
    }

    /**
     * Calcula los días de vencimiento
     * @return Número de días vencidos o 0 si no está vencida
     */
    @Transient
    public long getOverdueDays() {
        if (!isOverdue()) {
            return 0;
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    /**
     * Obtiene el monto neto (sin impuestos)
     * @return Monto neto
     */
    @Transient
    public BigDecimal getNetAmount() {
        return totalAmount.subtract(taxAmount);
    }
}