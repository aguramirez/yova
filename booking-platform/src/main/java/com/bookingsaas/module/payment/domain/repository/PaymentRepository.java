package com.bookingsaas.module.payment.domain.repository;

import com.bookingsaas.module.payment.domain.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con pagos
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Busca un pago por su ID de transacción
     * @param transactionId ID de transacción externo
     * @return Pago si existe
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Busca pagos por reserva
     * @param bookingId ID de la reserva
     * @return Lista de pagos de la reserva
     */
    List<Payment> findByBookingId(UUID bookingId);

    /**
     * Busca pagos por cliente
     * @param customerId ID del cliente
     * @param pageable Paginación
     * @return Página de pagos del cliente
     */
    Page<Payment> findByCustomerId(UUID customerId, Pageable pageable);

    /**
     * Busca pagos por negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de pagos del negocio
     */
    Page<Payment> findByBusinessId(UUID businessId, Pageable pageable);

    /**
     * Busca pagos por estado
     * @param businessId ID del negocio
     * @param status Estado del pago
     * @param pageable Paginación
     * @return Página de pagos con el estado especificado
     */
    Page<Payment> findByBusinessIdAndStatus(UUID businessId, Payment.PaymentStatus status, Pageable pageable);

    /**
     * Busca pagos por rango de fechas
     * @param businessId ID del negocio
     * @param startDate Fecha inicial
     * @param endDate Fecha final
     * @param pageable Paginación
     * @return Página de pagos en el rango de fechas
     */
    @Query("SELECT p FROM Payment p WHERE p.businessId = :businessId AND p.paymentDate BETWEEN :startDate AND :endDate")
    Page<Payment> findByDateRange(@Param("businessId") UUID businessId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * Suma los pagos completados por negocio en un rango de fechas
     * @param businessId ID del negocio
     * @param startDate Fecha inicial
     * @param endDate Fecha final
     * @return Suma total de pagos
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.businessId = :businessId AND p.status = 'COMPLETED' AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumPaymentsByDateRange(@Param("businessId") UUID businessId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Cuenta pagos por estado
     * @param businessId ID del negocio
     * @param status Estado del pago
     * @return Número de pagos
     */
    long countByBusinessIdAndStatus(UUID businessId, Payment.PaymentStatus status);

    /**
     * Encuentra pagos pendientes que llevan mucho tiempo sin completarse
     * @param businessId ID del negocio
     * @param olderThan Fecha límite
     * @return Lista de pagos antiguos pendientes
     */
    @Query("SELECT p FROM Payment p WHERE p.businessId = :businessId AND p.status = 'PENDING' AND p.createdAt < :olderThan")
    List<Payment> findStalePayments(@Param("businessId") UUID businessId, @Param("olderThan") LocalDateTime olderThan);

    /**
     * Busca pagos recientes de un cliente
     * @param customerId ID del cliente
     * @param limit Número máximo de resultados
     * @return Lista de pagos recientes
     */
    @Query("SELECT p FROM Payment p WHERE p.customerId = :customerId ORDER BY p.paymentDate DESC")
    List<Payment> findRecentPaymentsByCustomer(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Obtiene estadísticas de pagos por método de pago
     * @param businessId ID del negocio
     * @param startDate Fecha inicial
     * @param endDate Fecha final
     * @return Lista de resultados agrupados por método de pago
     */
    @Query("SELECT p.paymentMethod, COUNT(p) as count, SUM(p.amount) as total FROM Payment p " +
           "WHERE p.businessId = :businessId AND p.status = 'COMPLETED' AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY p.paymentMethod")
    List<Object[]> getPaymentMethodStats(@Param("businessId") UUID businessId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}