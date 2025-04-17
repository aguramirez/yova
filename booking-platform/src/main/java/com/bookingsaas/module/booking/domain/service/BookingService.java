package com.bookingsaas.module.booking.domain.service;

import com.bookingsaas.module.booking.domain.entity.Booking;
import com.bookingsaas.module.booking.domain.entity.Customer;
import com.bookingsaas.module.booking.domain.entity.Professional;
import com.bookingsaas.module.booking.domain.entity.Service;
import com.bookingsaas.module.booking.domain.repository.BookingRepository;
import com.bookingsaas.module.booking.domain.repository.CustomerRepository;
import com.bookingsaas.module.booking.domain.repository.ProfessionalRepository;
import com.bookingsaas.module.booking.domain.repository.ServiceRepository;
import com.bookingsaas.module.business.domain.service.BusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Servicio para gestionar reservas
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final ProfessionalRepository professionalRepository;
    private final ServiceRepository serviceRepository;
    private final BusinessService businessService;
    private final LoyaltyService loyaltyService;

    // Expresión regular para validar códigos de asistencia
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6,8}$");

    /**
     * Crear una nueva reserva
     * @param booking Datos de la reserva
     * @return Reserva creada
     */
    @Transactional
    public Booking createBooking(Booking booking) {
        // Validaciones básicas
        validateBookingData(booking);
        
        // Verificar disponibilidad
        checkAvailability(booking);
        
        // Generar código de asistencia único
        booking.generateAttendanceCode();
        
        // Establecer estado inicial
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED || 
            booking.getStatus() == Booking.BookingStatus.NO_SHOW) {
            throw new IllegalStateException("No se pueden cancelar reservas completadas o marcadas como no-show");
        }
        
        booking.cancelBooking();
        
        log.info("Reserva cancelada: {}", bookingId);
        return bookingRepository.save(booking);
    }

    /**
     * Validar asistencia a una reserva
     * @param attendanceCode Código de asistencia
     * @param validatedBy ID del usuario que valida
     * @return Reserva validada
     */
    @Transactional
    public Booking validateAttendance(String attendanceCode, UUID validatedBy) {
        // Validar formato del código
        if (!CODE_PATTERN.matcher(attendanceCode).matches()) {
            throw new IllegalArgumentException("Formato de código de asistencia inválido");
        }
        
        // Buscar reserva por código
        Booking booking = bookingRepository.findByAttendanceCode(attendanceCode)
                .orElseThrow(() -> new RuntimeException("Código de asistencia no encontrado: " + attendanceCode));
        
        // Verificar que no esté ya validada
        if (booking.isAttendanceValidated()) {
            throw new IllegalStateException("La asistencia ya fue validada para esta reserva");
        }
        
        // Verificar que no esté cancelada
        if (booking.getStatus() == Booking.BookingStatus.CANCELED) {
            throw new IllegalStateException("No se puede validar asistencia para una reserva cancelada");
        }
        
        // Marcar como completada
        booking.completeBooking(validatedBy);
        
        // Actualizar estadísticas del cliente
        Customer customer = booking.getCustomer();
        customer.recordAttendance();
        customerRepository.save(customer);
        
        // Actualizar estadísticas del profesional si aplica
        Professional professional = booking.getProfessional();
        if (professional != null) {
            professional.updateAttendanceRate(true);
            professionalRepository.save(professional);
        }
        
        // Otorgar puntos de fidelidad si el módulo está activo
        if (businessService.isModuleActive(booking.getBusinessId(), "loyalty")) {
            loyaltyService.awardPointsForBooking(booking);
        }
        
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Asistencia validada para reserva: {}", booking.getId());
        
        return updatedBooking;
    }

    /**
     * Marcar una reserva como no-show (cliente no se presentó)
     * @param bookingId ID de la reserva
     * @param validatedBy ID del usuario que marca la no-asistencia
     * @return Reserva actualizada
     */
    @Transactional
    public Booking markAsNoShow(UUID bookingId, UUID validatedBy) {
        Booking booking = getBookingById(bookingId);
        
        // Verificar que no esté ya validada o marcada
        if (booking.isAttendanceValidated() || booking.getStatus() == Booking.BookingStatus.NO_SHOW) {
            throw new IllegalStateException("La reserva ya fue procesada");
        }
        
        // Verificar que no esté cancelada
        if (booking.getStatus() == Booking.BookingStatus.CANCELED) {
            throw new IllegalStateException("No se puede marcar no-show para una reserva cancelada");
        }
        
        // Marcar como no-show
        booking.markAsNoShow(validatedBy);
        
        // Actualizar estadísticas del profesional si aplica
        Professional professional = booking.getProfessional();
        if (professional != null) {
            professional.updateAttendanceRate(false);
            professionalRepository.save(professional);
        }
        
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Reserva marcada como no-show: {}", bookingId);
        
        return updatedBooking;
    }

    /**
     * Reprogramar una reserva
     * @param bookingId ID de la reserva
     * @param newStartTime Nueva hora de inicio
     * @param newProfessionalId ID del nuevo profesional (opcional)
     * @return Reserva reprogramada
     */
    @Transactional
    public Booking rescheduleBooking(UUID bookingId, LocalDateTime newStartTime, UUID newProfessionalId) {
        Booking booking = getBookingById(bookingId);
        
        // Verificar que no esté completada, cancelada o marcada como no-show
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED || 
            booking.getStatus() == Booking.BookingStatus.CANCELED || 
            booking.getStatus() == Booking.BookingStatus.NO_SHOW) {
            throw new IllegalStateException("No se puede reprogramar una reserva finalizada");
        }
        
        // Actualizar hora de inicio
        booking.setStartTime(newStartTime);
        
        // Calcular nueva hora de fin basada en la duración del servicio
        Service service = booking.getService();
        booking.setEndTime(service.calculateEndTime(newStartTime));
        
        // Actualizar profesional si se especificó
        if (newProfessionalId != null) {
            Professional newProfessional = professionalRepository.findById(newProfessionalId)
                    .orElseThrow(() -> new RuntimeException("Profesional no encontrado: " + newProfessionalId));
            booking.setProfessional(newProfessional);
        }
        
        // Verificar disponibilidad
        checkAvailability(booking);
        
        // Si estaba cancelada, volver a estado pendiente
        if (booking.getStatus() == Booking.BookingStatus.CANCELED) {
            booking.setStatus(Booking.BookingStatus.PENDING);
        }
        
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Reserva reprogramada: {}", bookingId);
        
        return updatedBooking;
    }

    /**
     * Buscar una reserva por su código de asistencia
     * @param attendanceCode Código de asistencia
     * @return Reserva encontrada o vacío
     */
    @Transactional(readOnly = true)
    public Optional<Booking> findByAttendanceCode(String attendanceCode) {
        return bookingRepository.findByAttendanceCode(attendanceCode);
    }

    /**
     * Obtener reservas que necesitan recordatorio
     * @param hoursBeforeAppointment Horas de anticipación
     * @return Lista de reservas para enviar recordatorio
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsForReminder(int hoursBeforeAppointment) {
        LocalDateTime fromTime = LocalDateTime.now().plusHours(hoursBeforeAppointment).minusMinutes(10);
        LocalDateTime toTime = LocalDateTime.now().plusHours(hoursBeforeAppointment).plusMinutes(10);
        
        return bookingRepository.findNeedingReminder(fromTime, toTime);
    }
}.getStatus() == null) {
            booking.setStatus(Booking.BookingStatus.PENDING);
        }
        
        // Incrementar contador de citas del cliente
        Customer customer = booking.getCustomer();
        customer.setTotalAppointments(customer.getTotalAppointments() + 1);
        customerRepository.save(customer);
        
        // Guardar la reserva
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Reserva creada: {}", savedBooking.getId());
        
        return savedBooking;
    }

    /**
     * Validar datos de la reserva
     * @param booking Reserva a validar
     */
    private void validateBookingData(Booking booking) {
        if (booking.getBusinessId() == null) {
            throw new IllegalArgumentException("ID de negocio es requerido");
        }
        
        if (booking.getCustomer() == null) {
            throw new IllegalArgumentException("Cliente es requerido");
        }
        
        if (booking.getService() == null) {
            throw new IllegalArgumentException("Servicio es requerido");
        }
        
        if (booking.getStartTime() == null) {
            throw new IllegalArgumentException("Fecha y hora de inicio son requeridas");
        }
        
        if (booking.getEndTime() == null) {
            // Calcular automáticamente la hora de fin basada en la duración del servicio
            Service service = serviceRepository.findById(booking.getService().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));
            booking.setEndTime(service.calculateEndTime(booking.getStartTime()));
        }
        
        if (!booking.isTimeRangeValid()) {
            throw new IllegalArgumentException("El rango de tiempo no es válido");
        }
    }

    /**
     * Verificar disponibilidad para la reserva
     * @param booking Reserva a verificar
     * @throws IllegalStateException si hay conflicto de disponibilidad
     */
    private void checkAvailability(Booking booking) {
        // Verificar disponibilidad del profesional si está asignado
        if (booking.getProfessional() != null) {
            UUID professionalId = booking.getProfessional().getId();
            
            List<Booking> overlappingBookings = bookingRepository.findOverlappingForProfessional(
                    professionalId,
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getId() != null ? booking.getId() : UUID.randomUUID()
            );
            
            if (!overlappingBookings.isEmpty()) {
                throw new IllegalStateException("El profesional no está disponible en el horario seleccionado");
            }
        }
        
        // Verificar disponibilidad del recurso si está asignado
        if (booking.getResource() != null) {
            UUID resourceId = booking.getResource().getId();
            
            List<Booking> overlappingBookings = bookingRepository.findOverlappingForResource(
                    resourceId,
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getId() != null ? booking.getId() : UUID.randomUUID()
            );
            
            if (!overlappingBookings.isEmpty()) {
                throw new IllegalStateException("El recurso no está disponible en el horario seleccionado");
            }
        }
    }

    /**
     * Obtener una reserva por su ID
     * @param bookingId ID de la reserva
     * @return Reserva encontrada
     * @throws RuntimeException si no se encuentra
     */
    @Transactional(readOnly = true)
    public Booking getBookingById(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + bookingId));
    }

    /**
     * Obtener reservas por negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de reservas
     */
    @Transactional(readOnly = true)
    public Page<Booking> getBookingsByBusiness(UUID businessId, Pageable pageable) {
        return bookingRepository.findByBusinessId(businessId, pageable);
    }

    /**
     * Obtener reservas de un día específico
     * @param businessId ID del negocio
     * @param date Fecha
     * @return Lista de reservas
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByDate(UUID businessId, LocalDateTime date) {
        return bookingRepository.findByBusinessIdAndDate(businessId, date);
    }

    /**
     * Obtener reservas de un cliente
     * @param customerId ID del cliente
     * @param pageable Paginación
     * @return Página de reservas
     */
    @Transactional(readOnly = true)
    public Page<Booking> getBookingsByCustomer(UUID customerId, Pageable pageable) {
        return bookingRepository.findByCustomerId(customerId, pageable);
    }

    /**
     * Obtener próximas reservas de un cliente
     * @param customerId ID del cliente
     * @param pageable Paginación
     * @return Página de próximas reservas
     */
    @Transactional(readOnly = true)
    public Page<Booking> getUpcomingBookingsByCustomer(UUID customerId, Pageable pageable) {
        return bookingRepository.findUpcomingByCustomer(customerId, LocalDateTime.now(), pageable);
    }

    /**
     * Confirmar una reserva
     * @param bookingId ID de la reserva
     * @return Reserva confirmada
     */
    @Transactional
    public Booking confirmBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden confirmar reservas pendientes");
        }
        
        booking.confirmBooking();
        
        log.info("Reserva confirmada: {}", bookingId);
        return bookingRepository.save(booking);
    }

    /**
     * Cancelar una reserva
     * @param bookingId ID de la reserva
     * @return Reserva cancelada
     */
    @Transactional
    public Booking cancelBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        
        if (booking