package com.bookingsaas.module.booking.domain.service;

import com.bookingsaas.module.booking.domain.entity.Customer;
import com.bookingsaas.module.booking.domain.entity.CustomerCustomField;
import com.bookingsaas.module.booking.domain.repository.CustomerRepository;
import com.bookingsaas.module.business.domain.entity.CustomFieldDefinition;
import com.bookingsaas.module.business.domain.repository.CustomFieldDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para gestionar clientes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    
    private static final String REFERRAL_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REFERRAL_CODE_LENGTH = 8;
    private final SecureRandom random = new SecureRandom();

    /**
     * Crear un nuevo cliente
     * @param customer Datos del cliente
     * @return Cliente creado
     */
    @Transactional
    public Customer createCustomer(Customer customer) {
        // Validar datos
        validateCustomerData(customer);
        
        // Verificar si ya existe por email o teléfono
        if (customer.getEmail() != null) {
            Optional<Customer> existingByEmail = customerRepository.findByBusinessIdAndEmail(
                    customer.getBusinessId(), customer.getEmail());
            if (existingByEmail.isPresent()) {
                throw new IllegalArgumentException("Ya existe un cliente con este email en este negocio");
            }
        }
        
        if (customer.getPhone() != null) {
            Optional<Customer> existingByPhone = customerRepository.findByBusinessIdAndPhone(
                    customer.getBusinessId(), customer.getPhone());
            if (existingByPhone.isPresent()) {
                throw new IllegalArgumentException("Ya existe un cliente con este teléfono en este negocio");
            }
        }
        
        // Inicializar campos si no están definidos
        if (customer.getLoyaltyPoints() == null) {
            customer.setLoyaltyPoints(0);
        }
        
        if (customer.getTotalAppointments() == null) {
            customer.setTotalAppointments(0);
        }
        
        if (customer.getAppointmentsAttended() == null) {
            customer.setAppointmentsAttended(0);
        }
        
        // Generar código de referido
        if (customer.getReferralCode() == null) {
            customer.setReferralCode(generateReferralCode());
        }
        
        // Guardar cliente
        Customer savedCustomer = customerRepository.save(customer);
        log.info("Cliente creado con ID: {}", savedCustomer.getId());
        
        return savedCustomer;
    }

    /**
     * Validar datos del cliente
     * @param customer Cliente a validar
     */
    private void validateCustomerData(Customer customer) {
        if (customer.getBusinessId() == null) {
            throw new IllegalArgumentException("ID de negocio es requerido");
        }
        
        if (customer.getFirstName() == null || customer.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre es requerido");
        }
        
        if (customer.getLastName() == null || customer.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Apellido es requerido");
        }
        
        if (customer.getEmail() == null && customer.getPhone() == null) {
            throw new IllegalArgumentException("Al menos email o teléfono es requerido");
        }
    }

    /**
     * Generar un código de referido único
     * @return Código de referido generado
     */
    private String generateReferralCode() {
        StringBuilder code = new StringBuilder(REFERRAL_CODE_LENGTH);
        for (int i = 0; i < REFERRAL_CODE_LENGTH; i++) {
            code.append(REFERRAL_CODE_CHARS.charAt(random.nextInt(REFERRAL_CODE_CHARS.length())));
        }
        
        // Verificar que no exista ya
        Optional<Customer> existingWithCode = customerRepository.findByReferralCode(code.toString());
        if (existingWithCode.isPresent()) {
            // Si existe, generar otro recursivamente
            return generateReferralCode();
        }
        
        return code.toString();
    }

    /**
     * Obtener un cliente por su ID
     * @param customerId ID del cliente
     * @return Cliente encontrado
     * @throws RuntimeException si no se encuentra
     */
    @Transactional(readOnly = true)
    public Customer getCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + customerId));
    }

    /**
     * Buscar un cliente por su email en un negocio
     * @param businessId ID del negocio
     * @param email Email del cliente
     * @return Cliente encontrado o vacío
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByEmail(UUID businessId, String email) {
        return customerRepository.findByBusinessIdAndEmail(businessId, email);
    }

    /**
     * Buscar un cliente por su teléfono en un negocio
     * @param businessId ID del negocio
     * @param phone Teléfono del cliente
     * @return Cliente encontrado o vacío
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByPhone(UUID businessId, String phone) {
        return customerRepository.findByBusinessIdAndPhone(businessId, phone);
    }

    /**
     * Buscar un cliente por su código de referido
     * @param referralCode Código de referido
     * @return Cliente encontrado o vacío
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByReferralCode(String referralCode) {
        return customerRepository.findByReferralCode(referralCode);
    }

    /**
     * Obtener clientes de un negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de clientes
     */
    @Transactional(readOnly = true)
    public Page<Customer> getCustomersByBusiness(UUID businessId, Pageable pageable) {
        return customerRepository.findByBusinessId(businessId, pageable);
    }

    /**
     * Buscar clientes por nombre o apellido
     * @param businessId ID del negocio
     * @param searchTerm Término de búsqueda
     * @param pageable Paginación
     * @return Página de clientes
     */
    @Transactional(readOnly = true)
    public Page<Customer> searchCustomersByName(UUID businessId, String searchTerm, Pageable pageable) {
        return customerRepository.findByNameContaining(businessId, searchTerm, pageable);
    }

    /**
     * Actualizar un cliente
     * @param customerId ID del cliente
     * @param customerDetails Detalles actualizados
     * @return Cliente actualizado
     */
    @Transactional
    public Customer updateCustomer(UUID customerId, Customer customerDetails) {
        Customer existingCustomer = getCustomerById(customerId);
        
        // Actualizar campos básicos
        existingCustomer.setFirstName(customerDetails.getFirstName());
        existingCustomer.setLastName(customerDetails.getLastName());
        
        // Actualizar email si cambió, verificando que no exista
        if (customerDetails.getEmail() != null && 
                !customerDetails.getEmail().equals(existingCustomer.getEmail())) {
            
            Optional<Customer> existingByEmail = customerRepository.findByBusinessIdAndEmail(
                    existingCustomer.getBusinessId(), customerDetails.getEmail());
            
            if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(customerId)) {
                throw new IllegalArgumentException("Ya existe otro cliente con este email");
            }
            
            existingCustomer.setEmail(customerDetails.getEmail());
        }
        
        // Actualizar teléfono si cambió, verificando que no exista
        if (customerDetails.getPhone() != null && 
                !customerDetails.getPhone().equals(existingCustomer.getPhone())) {
            
            Optional<Customer> existingByPhone = customerRepository.findByBusinessIdAndPhone(
                    existingCustomer.getBusinessId(), customerDetails.getPhone());
            
            if (existingByPhone.isPresent() && !existingByPhone.get().getId().equals(customerId)) {
                throw new IllegalArgumentException("Ya existe otro cliente con este teléfono");
            }
            
            existingCustomer.setPhone(customerDetails.getPhone());
        }
        
        // Guardar cliente actualizado
        Customer updatedCustomer = customerRepository.save(existingCustomer);
        log.info("Cliente actualizado: {}", customerId);
        
        return updatedCustomer;
    }

    /**
     * Actualizar un campo personalizado de un cliente
     * @param customerId ID del cliente
     * @param fieldDefinitionId ID de la definición del campo
     * @param value Valor del campo
     * @return Cliente actualizado
     */
    @Transactional
    public Customer updateCustomField(UUID customerId, UUID fieldDefinitionId, String value) {
        Customer customer = getCustomerById(customerId);
        
        // Validar que el campo exista
        CustomFieldDefinition fieldDefinition = customFieldDefinitionRepository.findById(fieldDefinitionId)
                .orElseThrow(() -> new RuntimeException("Definición de campo no encontrada: " + fieldDefinitionId));
        
        // Validar que corresponda al mismo negocio
        if (!fieldDefinition.getBusiness().getId().equals(customer.getBusinessId())) {
            throw new IllegalArgumentException("El campo personalizado no pertenece al negocio del cliente");
        }
        
        // Validar el valor según el tipo de campo
        if (!fieldDefinition.isValidValue(value)) {
            throw new IllegalArgumentException("Valor inválido para el tipo de campo: " + fieldDefinition.getFieldType());
        }
        
        // Actualizar o crear el campo personalizado
        customer.setCustomFieldValue(fieldDefinitionId, value);
        
        // Guardar cliente actualizado
        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Campo personalizado actualizado para cliente: {}", customerId);
        
        return updatedCustomer;
    }

    /**
     * Eliminar un cliente (soft delete)
     * @param customerId ID del cliente
     */
    @Transactional
    public void deleteCustomer(UUID customerId) {
        Customer customer = getCustomerById(customerId);
        customerRepository.delete(customer);
        log.info("Cliente eliminado (soft delete): {}", customerId);
    }

    /**
     * Obtener clientes con puntos a punto de expirar
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Lista de clientes
     */
    @Transactional(readOnly = true)
    public List<Customer> getCustomersWithExpiringPoints(UUID businessId, Pageable pageable) {
        return customerRepository.findWithExpiringPoints(businessId, pageable);
    }

    /**
     * Obtener clientes inactivos
     * @param businessId ID del negocio
     * @param days Número de días de inactividad
     * @param pageable Paginación
     * @return Lista de clientes inactivos
     */
    @Transactional(readOnly = true)
    public List<Customer> getInactiveCustomers(UUID businessId, int days, Pageable pageable) {
        return customerRepository.findInactiveCustomers(businessId, days, pageable);
    }
}