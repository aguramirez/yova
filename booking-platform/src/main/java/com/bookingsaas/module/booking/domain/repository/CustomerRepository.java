package com.bookingsaas.module.booking.domain.repository;

import com.bookingsaas.module.booking.domain.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos con clientes
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Busca clientes por negocio
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de clientes del negocio
     */
    Page<Customer> findByBusinessId(UUID businessId, Pageable pageable);

    /**
     * Busca un cliente por su negocio y correo electrónico
     * @param businessId ID del negocio
     * @param email Correo electrónico del cliente
     * @return Cliente si existe
     */
    Optional<Customer> findByBusinessIdAndEmail(UUID businessId, String email);

    /**
     * Busca un cliente por su negocio y teléfono
     * @param businessId ID del negocio
     * @param phone Teléfono del cliente
     * @return Cliente si existe
     */
    Optional<Customer> findByBusinessIdAndPhone(UUID businessId, String phone);

    /**
     * Busca un cliente por su código de referido
     * @param referralCode Código de referido
     * @return Cliente si existe
     */
    Optional<Customer> findByReferralCode(String referralCode);

    /**
     * Busca clientes por parte de su nombre o apellido
     * @param businessId ID del negocio
     * @param searchTerm Término de búsqueda
     * @param pageable Paginación
     * @return Página de clientes que coinciden con la búsqueda
     */
    @Query("SELECT c FROM Customer c WHERE c.businessId = :businessId AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Customer> findByNameContaining(@Param("businessId") UUID businessId, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Busca clientes con más puntos de fidelidad
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de clientes ordenados por puntos de fidelidad
     */
    Page<Customer> findByBusinessIdOrderByLoyaltyPointsDesc(UUID businessId, Pageable pageable);

    /**
     * Busca clientes con más citas asistidas
     * @param businessId ID del negocio
     * @param pageable Paginación
     * @return Página de clientes ordenados por citas asistidas
     */
    Page<Customer> findByBusinessIdOrderByAppointmentsAttendedDesc(UUID businessId, Pageable pageable);

    /**
     * Cuenta clientes por negocio
     * @param businessId ID del negocio
     * @return Número de clientes
     */
    long countByBusinessId(UUID businessId);

    /**
     * Encuentra los clientes con más puntos de fidelidad próximos a vencer
     * @param businessId ID del negocio
     * @return Lista de clientes
     */
    @Query(value = "SELECT c.* FROM customers c " +
           "JOIN loyalty_transactions lt ON c.id = lt.customer_id " +
           "WHERE c.business_id = :businessId " +
           "AND lt.expires_at IS NOT NULL " +
           "AND lt.expires_at > CURRENT_TIMESTAMP " +
           "AND lt.expires_at < CURRENT_TIMESTAMP + INTERVAL '30 day' " +
           "GROUP BY c.id " +
           "ORDER BY SUM(lt.points_awarded) DESC",
           nativeQuery = true)
    List<Customer> findWithExpiringPoints(@Param("businessId") UUID businessId, Pageable pageable);

    /**
     * Encuentra clientes que no han hecho reservas en un tiempo determinado
     * @param businessId ID del negocio
     * @param days Número de días inactivos
     * @return Lista de clientes inactivos
     */
    @Query(value = "SELECT c.* FROM customers c " +
           "LEFT JOIN (" +
           "  SELECT customer_id, MAX(start_time) as last_booking " +
           "  FROM bookings WHERE business_id = :businessId " +
           "  GROUP BY customer_id" +
           ") b ON c.id = b.customer_id " +
           "WHERE c.business_id = :businessId " +
           "AND (b.last_booking IS NULL OR b.last_booking < CURRENT_TIMESTAMP - INTERVAL ':days day')",
           nativeQuery = true)
    List<Customer> findInactiveCustomers(@Param("businessId") UUID businessId, @Param("days") int days, Pageable pageable);
}