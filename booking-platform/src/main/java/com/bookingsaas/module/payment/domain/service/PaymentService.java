package com.bookingsaas.module.payment.domain.service;

import com.bookingsaas.module.booking.domain.entity.Booking;
import com.bookingsaas.module.booking.domain.repository.BookingRepository;
import com.bookingsaas.module.payment.domain.entity.Invoice;
import com.bookingsaas.module.payment.domain.entity.Payment;
import com.bookingsaas.module.payment.domain.entity.PaymentMethod;
import com.bookingsaas.module.payment.domain.repository.InvoiceRepository;
import com.bookingsaas.module.payment.domain.repository.PaymentMethodRepository;
import com.bookingsaas.module.payment.domain.repository.PaymentRepository;
import com.bookingsaas.module.payment.domain.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio para gestionar pagos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingRepository bookingRepository;

    // Contador atómico para generar números de factura
    private static final AtomicLong invoiceCounter = new AtomicLong(1000);

    /**
     * Crear un nuevo pago para una reserva
     * 
     * @param booking       Reserva
     * @param amount        Monto a pagar
     * @param paymentMethod Método de pago
     * @return Pago creado
     */
    @Transactional
    public Payment createPayment(Booking booking, BigDecimal amount, String paymentMethod) {
        // Validar datos básicos
        if (booking == null) {
            throw new IllegalArgumentException("La reserva es requerida");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("El método de pago es requerido");
        }

        // Crear objeto de pago
        Payment payment = Payment.builder()
                .bookingId(booking.getId())
                .customerId(booking.getCustomer().getId())
                .businessId(booking.getBusinessId())
                .amount(amount)
                .currency("USD") // Por defecto USD, idealmente configurable por negocio
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();

        // Guardar pago
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Pago creado: {} para reserva {}", savedPayment.getId(), booking.getId());

        return savedPayment;
    }

    /**
     * Procesar un pago pendiente
     * 
     * @param paymentId     ID del pago
     * @param transactionId ID de transacción del gateway de pago
     * @return Pago procesado
     */
    @Transactional
    public Payment processPayment(UUID paymentId, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + paymentId));

        // Validar que el pago esté pendiente
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException("El pago no está pendiente");
        }

        // Simular procesamiento de pago exitoso
        boolean paymentSuccessful = true; // En producción, esto vendría del gateway

        if (paymentSuccessful) {
            // Marcar pago como completado
            payment.complete(transactionId);

            // Generar factura si es necesario
            generateInvoice(payment);

            // Actualizar reserva relacionada
            Booking booking = bookingRepository.findById(payment.getBookingId())
                    .orElse(null);

            if (booking != null && booking.getStatus() == Booking.BookingStatus.PENDING) {
                booking.confirmBooking();
                bookingRepository.save(booking);
            }

            log.info("Pago procesado exitosamente: {} con transacción {}", paymentId, transactionId);
        } else {
            // Marcar pago como fallido
            payment.fail("Error de procesamiento");
            log.warn("Pago fallido: {}", paymentId);
        }

        return paymentRepository.save(payment);
    }

    /**
     * Generar factura para un pago
     * 
     * @param payment Pago completado
     * @return Factura generada
     */
    private Invoice generateInvoice(Payment payment) {
        // Generar número de factura único
        String invoiceNumber = generateInvoiceNumber(payment.getBusinessId());

        // Crear factura
        Invoice invoice = Invoice.builder()
                .paymentId(payment.getId())
                .businessId(payment.getBusinessId())
                .customerId(payment.getCustomerId())
                .invoiceNumber(invoiceNumber)
                .issuedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30)) // Vencimiento a 30 días
                .status(Invoice.InvoiceStatus.PAID) // Ya pagado
                .totalAmount(payment.getAmount())
                .taxAmount(calculateTax(payment.getAmount())) // Cálculo simplificado
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Factura generada: {} para pago {}", savedInvoice.getId(), payment.getId());

        return savedInvoice;
    }

    /**
     * Generar número de factura único
     * 
     * @param businessId ID del negocio
     * @return Número de factura
     */
    private String generateInvoiceNumber(UUID businessId) {
        // Formato: INV-{últimos 4 caracteres del businessId}-{año actual}-{contador}
        long counter = invoiceCounter.incrementAndGet();
        String businessSuffix = businessId.toString().substring(businessId.toString().length() - 4);
        int year = LocalDate.now().getYear();

        return String.format("INV-%s-%d-%04d", businessSuffix, year, counter);
    }

    /**
     * Calcular impuestos para una factura
     * 
     * @param amount Monto base
     * @return Monto de impuestos
     */
    private BigDecimal calculateTax(BigDecimal amount) {
        // Implementación simplificada: 10% de impuestos
        // En producción, esto dependería de la configuración del negocio y las leyes
        // fiscales
        return amount.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Cancelar un pago pendiente
     * 
     * @param paymentId ID del pago
     * @return Pago cancelado
     */
    @Transactional
    public Payment cancelPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + paymentId));

        if (!payment.cancel()) {
            throw new IllegalStateException("El pago no puede ser cancelado en su estado actual");
        }

        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Pago cancelado: {}", paymentId);

        return updatedPayment;
    }

    /**
     * Procesar reembolso de un pago
     * 
     * @param paymentId ID del pago
     * @param amount    Monto a reembolsar (null para reembolso total)
     * @return Pago reembolsado
     */
    @Transactional
    public Payment processRefund(UUID paymentId, BigDecimal amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + paymentId));

        // Validar que el pago esté completado
        if (!payment.isCompleted()) {
            throw new IllegalStateException("Solo se pueden reembolsar pagos completados");
        }

        // Si no se especifica monto, reembolsar todo
        BigDecimal refundAmount = amount != null ? amount : payment.getAmount();

        // Validar que el monto a reembolsar no sea mayor que el pago original
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("El monto a reembolsar no puede ser mayor que el pago original");
        }

        // Simular procesamiento de reembolso exitoso
        boolean refundSuccessful = true; // En producción, esto vendría del gateway

        if (refundSuccessful) {
            // Marcar pago como reembolsado
            payment.refund();

            // Actualizar factura si existe
            List<Invoice> invoices = invoiceRepository.findByPaymentId(paymentId);
            invoices.forEach(invoice -> {
                invoice.cancel();
                invoiceRepository.save(invoice);
            });

            log.info("Reembolso procesado para pago: {}, monto: {}", paymentId, refundAmount);
        } else {
            throw new RuntimeException("Error al procesar el reembolso");
        }

        return paymentRepository.save(payment);
    }

    /**
     * Obtener un pago por su ID
     * 
     * @param paymentId ID del pago
     * @return Pago encontrado
     * @throws RuntimeException si no se encuentra
     */
    @Transactional(readOnly = true)
    public Payment getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + paymentId));
    }

    /**
     * Buscar pagos por reserva
     * 
     * @param bookingId ID de la reserva
     * @return Lista de pagos
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByBooking(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    /**
     * Buscar pagos por cliente
     * 
     * @param customerId ID del cliente
     * @param pageable   Paginación
     * @return Página de pagos
     */
    @Transactional(readOnly = true)
    public Page<Payment> getPaymentsByCustomer(UUID customerId, Pageable pageable) {
        return paymentRepository.findByCustomerId(customerId, pageable);
    }

    /**
     * Buscar pagos por negocio
     * 
     * @param businessId ID del negocio
     * @param pageable   Paginación
     * @return Página de pagos
     */
    @Transactional(readOnly = true)
    public Page<Payment> getPaymentsByBusiness(UUID businessId, Pageable pageable) {
        return paymentRepository.findByBusinessId(businessId, pageable);
    }

    /**
     * Buscar pagos por negocio y rango de fechas
     * 
     * @param businessId ID del negocio
     * @param startDate  Fecha inicial
     * @param endDate    Fecha final
     * @param pageable   Paginación
     * @return Página de pagos
     */
    @Transactional(readOnly = true)
    public Page<Payment> getPaymentsByDateRange(UUID businessId, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable) {
        return paymentRepository.findByDateRange(businessId, startDate, endDate, pageable);
    }

    /**
     * Calcular total de ingresos en un período
     * 
     * @param businessId ID del negocio
     * @param startDate  Fecha inicial
     * @param endDate    Fecha final
     * @return Total de ingresos
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateRevenue(UUID businessId, LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.sumPaymentsByDateRange(businessId, startDate, endDate);
    }

    /**
     * Guardar un método de pago para un cliente
     * 
     * @param paymentMethod Método de pago
     * @return Método de pago guardado
     */
    // Corregir el método savePaymentMethod
    @Transactional
    public PaymentMethod savePaymentMethod(PaymentMethod paymentMethod) {
        // Validar datos básicos
        if (paymentMethod.getCustomerId() == null) {
            throw new IllegalArgumentException("ID de cliente es requerido");
        }

        if (paymentMethod.getBusinessId() == null) {
            throw new IllegalArgumentException("ID de negocio es requerido");
        }

        if (paymentMethod.getType() == null || paymentMethod.getType().isBlank()) {
            throw new IllegalArgumentException("Tipo de método de pago es requerido");
        }

        // Si se marca como predeterminado, desmarcar otros
        if (paymentMethod.isDefault()) {
            Optional<PaymentMethod> existingDefault = paymentMethodRepository
                    .findByCustomerIdAndBusinessIdAndIsDefaultTrue(
                            paymentMethod.getCustomerId(), paymentMethod.getBusinessId());

            existingDefault.ifPresent(pm -> {
                pm.setDefault(false);
                paymentMethodRepository.save(pm);
            });
        }

        // Guardar método de pago
        PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Método de pago guardado: {} para cliente {}",
                savedMethod.getId(), paymentMethod.getCustomerId());

        return savedMethod;
    }

    /**
     * Eliminar un método de pago
     * 
     * @param paymentMethodId ID del método de pago
     */
    @Transactional
    public void deletePaymentMethod(UUID paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new RuntimeException("Método de pago no encontrado: " + paymentMethodId));

        paymentMethodRepository.delete(paymentMethod);
        log.info("Método de pago eliminado: {}", paymentMethodId);
    }

    /**
     * Obtener métodos de pago de un cliente
     * 
     * @param customerId ID del cliente
     * @param businessId ID del negocio
     * @return Lista de métodos de pago
     */
    @Transactional(readOnly = true)
    public List<PaymentMethod> getCustomerPaymentMethods(UUID customerId, UUID businessId) {
        return paymentMethodRepository.findByCustomerIdAndBusinessId(customerId, businessId);
    }

    /**
     * Obtener método de pago predeterminado de un cliente
     * 
     * @param customerId ID del cliente
     * @param businessId ID del negocio
     * @return Método de pago predeterminado o vacío
     */
    @Transactional(readOnly = true)
    public Optional<PaymentMethod> getCustomerDefaultPaymentMethod(UUID customerId, UUID businessId) {
        return paymentMethodRepository.findByCustomerIdAndBusinessIdAndIsDefaultTrue(customerId, businessId);
    }
}