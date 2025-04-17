package com.bookingsaas.module.booking.domain.repository;

import com.bookingsaas.module.booking.domain.entity.RewardRedemption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, UUID> {
    Page<RewardRedemption> findByCustomerId(UUID customerId, Pageable pageable);
}