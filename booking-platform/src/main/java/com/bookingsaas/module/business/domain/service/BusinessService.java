package com.bookingsaas.module.business.domain.service;

import com.bookingsaas.module.business.domain.entity.Business;
import com.bookingsaas.module.business.domain.entity.BusinessModule;
import com.bookingsaas.module.business.domain.repository.BusinessModuleRepository;
import com.bookingsaas.module.business.domain.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para gestionar negocios
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessModuleRepository businessModuleRepository;

    /**
     * Crear un nuevo negocio
     * @param business Entidad de negocio a crear
     * @return Negocio creado
     */
    @Transactional
    public Business createBusiness(Business business) {
        // Validar que el email no exista
        if (businessRepository.findByContactEmail(business.getContactEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un negocio con este email");
        }

        // Si no se especifica un estado de suscripción, establecer como TRIAL
        if (business.getSubscriptionStatus() == null) {
            business.setSubscriptionStatus(Business.SubscriptionStatus.TRIAL);
        }

        // Si es TRIAL y no tiene fecha de fin, establecer 30 días por defecto
        if (business.getSubscriptionStatus() == Business.SubscriptionStatus.TRIAL
                && business.getTrialEndsAt() == null) {
            business.setTrialEndsAt(LocalDateTime.now().plusDays(30));
        }

        // Si no se especifica un plan, establecer como BASIC
        if (business.getSubscriptionPlan() == null) {
            business.setSubscriptionPlan(Business.SubscriptionPlan.BASIC);
        }

        // Guardar negocio
        Business savedBusiness = businessRepository.save(business);
        log.info("Negocio creado con ID: {}", savedBusiness.getId());

        // Inicializar módulos
        initDefaultModules(savedBusiness);
        
        return savedBusiness;
    }

    /**
     * Inicializar módulos por defecto para un nuevo negocio
     * @param business Negocio
     */
    private void initDefaultModules(Business business) {
        // Módulos por defecto para todos los negocios
        activateModule(business.getId(), BusinessModule.ModuleNames.LOYALTY, true);
        activateModule(business.getId(), BusinessModule.ModuleNames.PAYMENT, true);
        activateModule(business.getId(), BusinessModule.ModuleNames.NOTIFICATION, true);
        
        // Estos módulos están desactivados por defecto
        activateModule(business.getId(), BusinessModule.ModuleNames.DOCUMENT, false);
        activateModule(business.getId(), BusinessModule.ModuleNames.ANALYTICS, false);
    }

    /**
     * Obtener un negocio por su ID
     * @param businessId ID del negocio
     * @return Negocio encontrado
     * @throws RuntimeException si no se encuentra
     */
    @Transactional(readOnly = true)
    public Business getBusinessById(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado con ID: " + businessId));
    }

    /**
     * Obtener un negocio por su email
     * @param email Email del negocio
     * @return Negocio encontrado
     * @throws RuntimeException si no se encuentra
     */
    @Transactional(readOnly = true)
    public Business getBusinessByEmail(String email) {
        return businessRepository.findByContactEmail(email)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado con email: " + email));
    }

    /**
     * Actualizar un negocio existente
     * @param businessId ID del negocio
     * @param businessDetails Detalles actualizados
     * @return Negocio actualizado
     */
    @Transactional
    public Business updateBusiness(UUID businessId, Business businessDetails) {
        Business existingBusiness = getBusinessById(businessId);
        
        // Actualizar campos
        existingBusiness.setName(businessDetails.getName());
        existingBusiness.setType(businessDetails.getType());
        existingBusiness.setContactPhone(businessDetails.getContactPhone());
        existingBusiness.setTimeZone(businessDetails.getTimeZone());
        
        // El email es un campo crítico, validar si cambia
        if (!existingBusiness.getContactEmail().equals(businessDetails.getContactEmail())) {
            if (businessRepository.findByContactEmail(businessDetails.getContactEmail()).isPresent()) {
                throw new IllegalArgumentException("Ya existe un negocio con este email");
            }
            existingBusiness.setContactEmail(businessDetails.getContactEmail());
        }
        
        return businessRepository.save(existingBusiness);
    }

    /**
     * Actualizar el estado de suscripción de un negocio
     * @param businessId ID del negocio
     * @param status Nuevo estado
     * @param plan Nuevo plan (opcional)
     * @return Negocio actualizado
     */
    @Transactional
    public Business updateSubscriptionStatus(UUID businessId, Business.SubscriptionStatus status, 
                                             Business.SubscriptionPlan plan) {
        Business business = getBusinessById(businessId);
        business.setSubscriptionStatus(status);
        
        if (plan != null) {
            business.setSubscriptionPlan(plan);
        }
        
        // Si cambia a TRIAL, establecer fecha de fin
        if (status == Business.SubscriptionStatus.TRIAL) {
            business.setTrialEndsAt(LocalDateTime.now().plusDays(30));
        }
        
        return businessRepository.save(business);
    }

    /**
     * Activar o desactivar un módulo para un negocio
     * @param businessId ID del negocio
     * @param moduleName Nombre del módulo
     * @param active Estado activo (true) o inactivo (false)
     * @return Módulo actualizado
     */
    @Transactional
    public BusinessModule activateModule(UUID businessId, String moduleName, boolean active) {
        Business business = getBusinessById(businessId);
        
        // Buscar el módulo o crearlo si no existe
        BusinessModule module = businessModuleRepository
                .findByBusinessIdAndModuleName(businessId, moduleName)
                .orElseGet(() -> {
                    BusinessModule newModule = BusinessModule.builder()
                            .business(business)
                            .moduleName(moduleName)
                            .active(false)
                            .build();
                    return businessModuleRepository.save(newModule);
                });
        
        // Actualizar estado
        module.setActive(active);
        BusinessModule savedModule = businessModuleRepository.save(module);
        
        log.info("Módulo {} {} para negocio {}", 
                moduleName, active ? "activado" : "desactivado", businessId);
        
        return savedModule;
    }

    /**
     * Verificar si un módulo está activo para un negocio
     * @param businessId ID del negocio
     * @param moduleName Nombre del módulo
     * @return true si está activo
     */
    @Transactional(readOnly = true)
    public boolean isModuleActive(UUID businessId, String moduleName) {
        return businessModuleRepository.isModuleActive(businessId, moduleName);
    }

    /**
     * Obtener todos los módulos de un negocio
     * @param businessId ID del negocio
     * @return Lista de módulos
     */
    @Transactional(readOnly = true)
    public List<BusinessModule> getBusinessModules(UUID businessId) {
        return businessModuleRepository.findByBusinessId(businessId);
    }

    /**
     * Eliminar un negocio (soft delete)
     * @param businessId ID del negocio
     */
    @Transactional
    public void deleteBusiness(UUID businessId) {
        Business business = getBusinessById(businessId);
        businessRepository.delete(business);
        log.info("Negocio eliminado (soft delete) con ID: {}", businessId);
    }
}