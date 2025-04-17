package com.bookingsaas.module.booking.domain.repository;

import com.bookingsaas.module.booking.domain.entity.LoyaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con transacciones de puntos de fidelidad
 */
@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {

    /**
     * Busca transacciones por cliente
     * @param customerId ID del cliente
     * @param pageable Paginación
     * @return Página de transacciones del cliente
     */
    Page<LoyaltyTransaction> findByCustomerId(UUID customerId, Pageable pageable);

    /**
     * Busca transacciones por negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de transacciones del negocio
     */
    Page<LoyaltyTransaction> findByBusinessId(UUID businessId, Pageable pageable);

    /**
     * Busca transacciones por cliente y negocio
     * @param customerId ID del cliente
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de transacciones del cliente en el negocio
     */
    Page<LoyaltyTransaction> findByCustomerIdAndBusinessId(UUID customerId, UUID businessId, Pageable pageable);

    /**
     * Busca transacciones por tipo
     * @param customerId ID del cliente
     * @param transactionType Tipo de transacción
     * @param pageable Paginación
     * @return Página de transacciones del tipo especificado
     */
    Page<LoyaltyTransaction> findByCustomerIdAndTransactionType(UUID customerId, LoyaltyTransaction.TransactionType transactionType, Pageable pageable);

    /**
     * Busca transacciones asociadas a una reserva
     * @param bookingId ID de la reserva
     * @return Lista de transacciones
     */
    List<LoyaltyTransaction> findByBookingId(UUID bookingId);

    /**
     * Busca transacciones por vencer
     * @param fromDate Fecha desde
     * @param toDate Fecha hasta
     * @return Lista de transacciones por vencer
     */
    @Query("SELECT lt FROM LoyaltyTransaction lt WHERE lt.expiresAt BETWEEN :fromDate AND :toDate AND lt.transactionType <> 'EXPIRATION'")
    List<LoyaltyTransaction> findAboutToExpire(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    /**
     * Suma los puntos activos de un cliente
     * @param customerId ID del cliente
     * @param businessId ID del negocio
     * @param currentDate Fecha actual
     * @return Total de puntos activos
     */
    @Query("SELECT COALESCE(SUM(lt.pointsAwarded), 0) FROM LoyaltyTransaction lt " +
           "WHERE lt.customerId = :customerId AND lt.businessId = :businessId " +
           "AND (lt.expiresAt IS NULL OR lt.expiresAt > :currentDate)")
    Integer sumActivePoints(@Param("customerId") UUID customerId, @Param("businessId") UUID businessId, @Param("currentDate") LocalDateTime currentDate);

    /**
     * Busca las transacciones más recientes de un cliente
     * @param customerId ID del cliente
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de transacciones recientes
     */
    Page<LoyaltyTransaction> findByCustomerIdAndBusinessIdOrderByCreatedAtDesc(UUID customerId, UUID businessId, Pageable pageable);

    /**
     * Cuenta transacciones por tipo
     * @param businessId ID del negocio
     * @param transactionType Tipo de transacción
     * @return Número de transacciones
     */
    long countByBusinessIdAndTransactionType(UUID businessId, LoyaltyTransaction.TransactionType transactionType);

    /**
     * Calcula la suma total de puntos otorgados
     * @param businessId ID del negocio
     * @param transactionType Tipo de transacción
     * @return Suma de puntos
     */
    @Query("SELECT COALESCE(SUM(lt.pointsAwarded), 0) FROM LoyaltyTransaction lt " +
           "WHERE lt.businessId = :businessId AND lt.transactionType = :transactionType")
    Integer sumPointsByTransactionType(@Param("businessId") UUID businessId, @Param("transactionType") LoyaltyTransaction.TransactionType transactionType);
}