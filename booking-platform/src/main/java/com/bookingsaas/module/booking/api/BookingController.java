package com.bookingsaas.module.booking.api;

import com.bookingsaas.module.booking.domain.entity.Booking;
import com.bookingsaas.module.booking.domain.entity.Customer;
import com.bookingsaas.module.booking.domain.service.BookingService;
import com.bookingsaas.module.booking.domain.service.CustomerService;
import com.bookingsaas.module.identity.domain.entity.User;
import com.bookingsaas.module.identity.domain.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para operaciones de reservas
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;
    private final CustomerService customerService;
    private final AuthService authService;

    /**
     * Crear una nueva reserva
     * @param businessId ID del negocio
     * @param booking Datos de la reserva
     * @return Reserva creada
     */
    @PostMapping("/businesses/{businessId}/bookings")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST')")
    public ResponseEntity<Booking> createBooking(
            @PathVariable UUID businessId,
            @Valid @RequestBody Booking booking) {
        
        // Asegurar que el businessId de la URL coincida con el de la reserva
        booking.setBusinessId(businessId);
        
        // Crear la reserva
        Booking createdBooking = bookingService.createBooking(booking);
        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    /**
     * Crear una reserva para un cliente externo (API pública)
     * @param businessId ID del negocio
     * @param booking Datos de la reserva
     * @param customer Datos del cliente
     * @return Reserva creada
     */
    @PostMapping("/public/businesses/{businessId}/bookings")
    public ResponseEntity<Booking> createPublicBooking(
            @PathVariable UUID businessId,
            @Valid @RequestBody Booking booking,
            @Valid @RequestBody Customer customer) {
        
        // Buscar cliente existente o crear uno nuevo
        Customer existingCustomer = null;
        
        if (customer.getEmail() != null) {
            existingCustomer = customerService.findByEmail(businessId, customer.getEmail()).orElse(null);
        } else if (customer.getPhone() != null) {
            existingCustomer = customerService.findByPhone(businessId, customer.getPhone()).orElse(null);
        }
        
        if (existingCustomer == null) {
            // Crear nuevo cliente
            customer.setBusinessId(businessId);
            existingCustomer = customerService.createCustomer(customer);
        }
        
        // Asociar cliente a la reserva
        booking.setCustomer(existingCustomer);
        booking.setBusinessId(businessId);
        
        // Crear la reserva
        Booking createdBooking = bookingService.createBooking(booking);
        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    /**
     * Obtener una reserva por su ID
     * @param businessId ID del negocio
     * @param id ID de la reserva
     * @return Reserva encontrada
     */
    @GetMapping("/businesses/{businessId}/bookings/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'PROFESSIONAL')")
    public ResponseEntity<Booking> getBookingById(
            @PathVariable UUID businessId,
            @PathVariable UUID id) {
        
        Booking booking = bookingService.getBookingById(id);
        
        // Verificar que la reserva pertenezca al negocio
        if (!booking.getBusinessId().equals(businessId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(booking);
    }

    /**
     * Obtener reservas de un negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de reservas
     */
    @GetMapping("/businesses/{businessId}/bookings")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'PROFESSIONAL')")
    public ResponseEntity<Page<Booking>> getBusinessBookings(
            @PathVariable UUID businessId,
            Pageable pageable) {
        
        return ResponseEntity.ok(bookingService.getBookingsByBusiness(businessId, pageable));
    }

    /**
     * Obtener reservas para una fecha específica
     * @param businessId ID del negocio
     * @param date Fecha
     * @return Lista de reservas
     */
    @GetMapping("/businesses/{businessId}/bookings/byDate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'PROFESSIONAL')")
    public ResponseEntity<List<Booking>> getBookingsByDate(
            @PathVariable UUID businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        
        return ResponseEntity.ok(bookingService.getBookingsByDate(businessId, date));
    }

    /**
     * Obtener reservas de un cliente
     * @param businessId ID del negocio
     * @param customerId ID del cliente
     * @param pageable Paginación
     * @return Página de reservas
     */
    @GetMapping("/businesses/{businessId}/customers/{customerId}/bookings")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST')")
    public ResponseEntity<Page<Booking>> getCustomerBookings(
            @PathVariable UUID businessId,
            @PathVariable UUID customerId,
            Pageable pageable) {
        
        Customer customer = customerService.getCustomerById(customerId);
        
        // Verificar que el cliente pertenezca al negocio
        if (!customer.getBusinessId().equals(businessId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(bookingService.getBookingsByCustomer(customerId, pageable));
    }

    /**
     * Confirmar una reserva
     * @param businessId ID del negocio
     * @param id ID de la reserva
     * @return Reserva confirmada
     */
    @PutMapping("/businesses/{businessId}/bookings/{id}/confirm")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST')")
    public ResponseEntity<Booking> confirmBooking(
            @PathVariable UUID businessId,
            @PathVariable UUID id) {
        
        Booking booking = bookingService.getBookingById(id);
        
        // Verificar que la reserva pertenezca al negocio
        if (!booking.getBusinessId().equals(businessId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(bookingService.confirmBooking(id));
    }

    /**
     * Cancelar una reserva
     * @param businessId ID del negocio
     * @param id ID de la reserva
     * @return Reserva cancelada
     */
    @PutMapping("/businesses/{businessId}/bookings/{id}/cancel")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST')")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable UUID businessId,
            @PathVariable UUID id) {
        
        Booking booking = bookingService.getBookingById(id);
        
        // Verificar que la reserva pertenezca al negocio
        if (!booking.getBusinessId().equals(businessId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }

    /**
     * Validar asistencia a una reserva
     * @param businessId ID del negocio
     * @param attendanceCode Código de asistencia
     * @return Reserva validada
     */
    @PutMapping("/businesses/{businessId}/bookings/validateAttendance")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'PROFESSIONAL')")
    public ResponseEntity<Booking> validateAttendance(
            @PathVariable UUID businessId,
            @RequestParam String attendanceCode) {
        
        // Obtener usuario actual para registro de validación
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Booking booking = bookingService.validateAttendance(attendanceCode, currentUser.getId());
        
        // Verificar que la reserva pertenezca al negocio
        if (!booking.getBusinessId().equals(businessId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(booking);
    }

    /**
     * Marcar una reserva como no-show
     * @param businessId ID del negocio
     * @param id ID de la reserva
     * @return Reserva marcada como no-show
     */
    @PutMapping("/businesses/{businessId}/bookings/{id}/noShow")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'PROFESSIONAL')")
    public ResponseEntity<Booking> markAsNoShow(
            @PathVariable UUID businessId,
            @PathVariable UUID id) {
        
        Booking booking = bookingService.getBookingById(id);
        
        // Verificar que la reserva pertenezca al negocio
        if (!booking.getBusinessId().equals(businessId)) {
            return ResponseEntity.notFound().build();
        }
        
        // Obtener usuario actual para registro
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        return ResponseEntity.ok(bookingService.markAsNoShow(id, currentUser.getId()));
    }

    /**
     * Reprogramar una reserva
     * @param businessId ID del negocio
     * @param id ID de la reserva
     * @param newStartTime Nueva hora de inicio
     * @param newProfessionalId ID del nuevo profesional (opcional)
     * @return Reserva reprogramada
     */
    @PutMapping("/businesses/{businessId}/bookings/{id}/reschedule")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @authService.userHasRole(authentication.principal.subject, #businessId, 'BUSINESS_ADMIN') " +
                  "or @authService.userHasRole(authentication.principal.subject, #businessId, 'RECEPTIONIST')")
    public ResponseEntity<Booking> rescheduleBooking(
            @PathVariable UUID businessId,
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newStartTime,
            @RequestParam(required = false) UUID newProfessionalId) {
        
        Booking booking = bookingService.getBookingById(id);
        
        // Verificar que la reserva pertenezca al negocio
        if (!booking.getBusinessId().equals(businessId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(bookingService.rescheduleBooking(id, newStartTime, newProfessionalId));
    }
}