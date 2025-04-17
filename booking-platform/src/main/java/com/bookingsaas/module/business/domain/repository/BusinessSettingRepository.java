package com.bookingsaas.module.business.domain.repository;

import com.bookingsaas.module.business.domain.entity.Business;
import com.bookingsaas.module.business.domain.entity.BusinessSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessSettingRepository extends JpaRepository<BusinessSetting, UUID> {
    List<BusinessSetting> findByBusinessId(UUID businessId);
    
    Optional<BusinessSetting> findByBusinessIdAndSettingKey(UUID businessId, String settingKey);
}