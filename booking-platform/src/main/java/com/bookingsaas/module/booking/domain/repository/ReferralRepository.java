package com.bookingsaas.module.booking.domain.repository;

import com.bookingsaas.module.booking.domain.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {
    Optional<Referral> findByReferrerCustomerIdAndReferredCustomerId(UUID referrerId, UUID referredId);
}