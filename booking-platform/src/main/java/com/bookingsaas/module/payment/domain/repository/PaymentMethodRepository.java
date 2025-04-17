package com.bookingsaas.module.payment.domain.repository;

import com.bookingsaas.module.payment.domain.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByCustomerIdAndBusinessId(UUID customerId, UUID businessId);
    
    Optional<PaymentMethod> findByCustomerIdAndBusinessIdAndIsDefaultTrue(UUID customerId, UUID businessId);
}