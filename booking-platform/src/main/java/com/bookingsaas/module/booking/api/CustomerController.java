package com.bookingsaas.module.booking.api;

import com.bookingsaas.module.booking.domain.entity.Customer;
import com.bookingsaas.module.booking.domain.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * Controlador REST para operaciones de clientes
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Crear un nuevo cliente
     * @param businessId ID del negocio
     * @param customer Datos del cliente
     * @return Cliente creado
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST')")
    public ResponseEntity<Customer> createCustomer(
            @PathVariable UUID businessId,
            @Valid @RequestBody Customer customer) {
        
        // Asegurar que el businessId de la URL coincida con el del cliente
        customer.setBusinessId(businessId);
        
        // Crear cliente
        Customer createdCustomer = customerService.createCustomer(customer);
        return new ResponseEntity<>(createdCustomer, HttpStatus.CREATED);
    }

    /**
     * Obtener un cliente por su ID
     * @param businessId ID del negocio
     * @param id ID del cliente
     * @return Cliente encontrado
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'PROFESSIONAL')")
    public ResponseEntity<Customer> getCustomerById(
            @PathVariable UUID businessId,
            @PathVariable UUID id) {
        
        Customer customer = customerService.getCustomerById(id);
        
        // Verificar que el cliente pertenezca al negocio
        if (!customer.getBusinessId().equals(businessId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(customer);
    }

    /**
     * Obtener clientes de un negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de clientes
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'PROFESSIONAL')")
    public ResponseEntity<Page<Customer>> getBusinessCustomers(
            @PathVariable UUID businessId,
            Pageable pageable) {
        
        return ResponseEntity.ok(customerService.getCustomersByBusiness(businessId, pageable));
    }

    /**
     * Buscar clientes por nombre
     * @param businessId ID del negocio
     * @param searchTerm Término de búsqueda
     * @param pageable Paginación
     * @return Página de clientes
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'PROFESSIONAL')")
    public ResponseEntity<Page<Customer>> searchCustomers(
            @PathVariable UUID businessId,
            @RequestParam String searchTerm,
            Pageable pageable) {
        
        return ResponseEntity.ok(customerService.searchCustomersByName(businessId, searchTerm, pageable));
    }

    /**
     * Actualizar un cliente
     * @param businessId ID del negocio
     * @param id ID del cliente
     * @param customer Datos actualizados
     * @return Cliente actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST')")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable UUID businessId,
            @PathVariable UUID id,
            @Valid @RequestBody Customer customer) {
        
        Customer existingCustomer = customerService.getCustomerById(id);
        
        // Verificar que el cliente pertenezca al negocio
        if (!existingCustomer.getBusinessId().equals(businessId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(customerService.updateCustomer(id, customer));
    }

    /**
     * Actualizar un campo personalizado de un cliente
     * @param businessId ID del negocio
     * @param id ID del cliente
     * @param fieldDefinitionId ID de la definición del campo
     * @param value Valor del campo
     * @return Cliente actualizado
     */
    @PutMapping("/{id}/customFields/{fieldDefinitionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST')")
    public ResponseEntity<Customer> updateCustomField(
            @PathVariable UUID businessId,
            @PathVariable UUID id,
            @PathVariable UUID fieldDefinitionId,
            @RequestParam String value) {
        
        Customer customer = customerService.getCustomerById(id);
        
        // Verificar que el cliente pertenezca al negocio
        if (!customer.getBusinessId().equals(businessId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(customerService.updateCustomField(id, fieldDefinitionId, value));
    }

    /**
     * Eliminar un cliente
     * @param businessId ID del negocio
     * @param id ID del cliente
     * @return Respuesta sin contenido
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN')")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable UUID businessId,
            @PathVariable UUID id) {
        
        Customer customer = customerService.getCustomerById(id);
        
        // Verificar que el cliente pertenezca al negocio
        if (!customer.getBusinessId().equals(businessId)) {
            return ResponseEntity.notFound().build();
        }
        
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}