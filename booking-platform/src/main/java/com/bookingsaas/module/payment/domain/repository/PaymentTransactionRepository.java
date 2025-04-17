package com.bookingsaas.module.payment.domain.repository;

import com.bookingsaas.module.payment.domain.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    // Métodos básicos heredados de JpaRepository
}