package com.bookingsaas.module.business.api;

import com.bookingsaas.module.business.domain.entity.Business;
import com.bookingsaas.module.business.domain.entity.BusinessModule;
import com.bookingsaas.module.business.domain.service.BusinessService;
import com.bookingsaas.module.identity.domain.entity.Role;
import com.bookingsaas.module.identity.domain.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para operaciones de negocios
 */
@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
@Slf4j
public class BusinessController {

    private final BusinessService businessService;
    private final AuthService authService;

    /**
     * Crear un nuevo negocio
     * @param business Datos del negocio
     * @return Negocio creado
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('USER')")
    public ResponseEntity<Business> createBusiness(@Valid @RequestBody Business business) {
        Business createdBusiness = businessService.createBusiness(business);
        
        // Asignar rol de administrador al usuario actual
        try {
            authService.assignRoleToUser(
                    authService.getCurrentUser().getId(),
                    createdBusiness.getId(),
                    Role.RoleNames.BUSINESS_ADMIN
            );
        } catch (Exception e) {
            log.warn("Error al asignar rol de administrador: {}", e.getMessage());
            // Continuamos aunque falle la asignación de rol
        }
        
        return new ResponseEntity<>(createdBusiness, HttpStatus.CREATED);
    }

    /**
     * Obtener un negocio por su ID
     * @param id ID del negocio
     * @return Negocio encontrado
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #id, 'BUSINESS_ADMIN')")
    public ResponseEntity<Business> getBusinessById(@PathVariable UUID id) {
        return ResponseEntity.ok(businessService.getBusinessById(id));
    }

    /**
     * Actualizar un negocio
     * @param id ID del negocio
     * @param business Datos actualizados
     * @return Negocio actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #id, 'BUSINESS_ADMIN')")
    public ResponseEntity<Business> updateBusiness(@PathVariable UUID id, @Valid @RequestBody Business business) {
        return ResponseEntity.ok(businessService.updateBusiness(id, business));
    }

    /**
     * Eliminar un negocio
     * @param id ID del negocio
     * @return Respuesta sin contenido
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #id, 'BUSINESS_ADMIN')")
    public ResponseEntity<Void> deleteBusiness(@PathVariable UUID id) {
        businessService.deleteBusiness(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener todos los módulos de un negocio
     * @param id ID del negocio
     * @return Lista de módulos
     */
    @GetMapping("/{id}/modules")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #id, 'BUSINESS_ADMIN')")
    public ResponseEntity<List<BusinessModule>> getBusinessModules(@PathVariable UUID id) {
        return ResponseEntity.ok(businessService.getBusinessModules(id));
    }

    /**
     * Activar o desactivar un módulo
     * @param id ID del negocio
     * @param moduleName Nombre del módulo
     * @param active Estado (activo/inactivo)
     * @return Módulo actualizado
     */
    @PutMapping("/{id}/modules/{moduleName}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasPermission(authentication.principal.subject, #id, 'MODULE_MANAGE')")
    public ResponseEntity<BusinessModule> updateModuleStatus(
            @PathVariable UUID id,
            @PathVariable String moduleName,
            @RequestParam boolean active) {
        
        return ResponseEntity.ok(businessService.activateModule(id, moduleName, active));
    }

    /**
     * Actualizar el estado de suscripción de un negocio
     * @param id ID del negocio
     * @param status Nuevo estado
     * @param plan Nuevo plan (opcional)
     * @return Negocio actualizado
     */
    @PutMapping("/{id}/subscription")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Business> updateSubscriptionStatus(
            @PathVariable UUID id,
            @RequestParam Business.SubscriptionStatus status,
            @RequestParam(required = false) Business.SubscriptionPlan plan) {
        
        return ResponseEntity.ok(businessService.updateSubscriptionStatus(id, status, plan));
    }
}