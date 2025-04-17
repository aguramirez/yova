package com.bookingsaas.module.payment.domain.repository;

import com.bookingsaas.module.payment.domain.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByPaymentId(UUID paymentId);
}