package com.bookingsaas.module.payment.domain.entity;

import com.bookingsaas.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PaymentTransaction extends BaseEntity {

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_by")
    private UUID createdBy;
}