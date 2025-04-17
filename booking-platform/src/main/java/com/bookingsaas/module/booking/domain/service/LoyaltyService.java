package com.bookingsaas.module.booking.domain.service;

import com.bookingsaas.module.booking.domain.entity.*;
import com.bookingsaas.module.booking.domain.repository.*;
import com.bookingsaas.module.business.domain.entity.Business;
import com.bookingsaas.module.business.domain.entity.BusinessSetting;
import com.bookingsaas.module.business.domain.repository.BusinessRepository;
import com.bookingsaas.module.business.domain.repository.BusinessSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para gestionar el sistema de fidelización y puntos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final CustomerRepository customerRepository;
    private final ReferralRepository referralRepository;
    private final RewardItemRepository rewardItemRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final BusinessRepository businessRepository;
    private final BusinessSettingRepository businessSettingRepository;

    /**
     * Otorgar puntos por asistencia a una cita
     * @param booking Reserva completada
     * @return Transacción de puntos creada
     */
    @Transactional
    public LoyaltyTransaction awardPointsForBooking(Booking booking) {
        Customer customer = booking.getCustomer();
        UUID businessId = booking.getBusinessId();
        
        // Determinar cuántos puntos otorgar (basado en configuración del negocio)
        int pointsToAward = getPointsPerBooking(businessId);
        
        // Verificar si es primera cita para bonificación
        if (customer.getAppointmentsAttended() == 1) {
            int firstBookingBonus = getFirstBookingBonus(businessId);
            if (firstBookingBonus > 0) {
                pointsToAward += firstBookingBonus;
                log.info("Bonificación de primera cita aplicada: +{} puntos", firstBookingBonus);
            }
        }
        
        // Crear transacción de puntos
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customerId(customer.getId())
                .businessId(businessId)
                .bookingId(booking.getId())
                .pointsAwarded(pointsToAward)
                .transactionType(LoyaltyTransaction.TransactionType.BOOKING)
                .expiresAt(calculateExpirationDate(businessId))
                .build();
        
        // Guardar transacción
        LoyaltyTransaction savedTransaction = loyaltyTransactionRepository.save(transaction);
        
        // Actualizar puntos del cliente
        customer.addLoyaltyPoints(pointsToAward);
        customerRepository.save(customer);
        
        log.info("Puntos otorgados por reserva: {} puntos para cliente {}", 
                pointsToAward, customer.getId());
        
        return savedTransaction;
    }

    /**
     * Otorgar puntos por referido convertido
     * @param referralId ID del referido
     * @return Transacción de puntos creada
     */
    @Transactional
    public LoyaltyTransaction awardPointsForReferral(UUID referralId) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new RuntimeException("Referido no encontrado: " + referralId));
        
        // Verificar que el referido no haya sido convertido ya
        if (referral.getStatus() != Referral.ReferralStatus.PENDING) {
            throw new IllegalStateException("El referido ya fue procesado");
        }
        
        // Obtener cliente referente
        Customer referrer = customerRepository.findById(referral.getReferrerCustomerId())
                .orElseThrow(() -> new RuntimeException("Cliente referente no encontrado"));
        
        // Obtener cliente referido
        Customer referred = customerRepository.findById(referral.getReferredCustomerId())
                .orElseThrow(() -> new RuntimeException("Cliente referido no encontrado"));
        
        // Determinar puntos por referido (configuración del negocio)
        int pointsToAward = getReferralPoints(referral.getBusinessId());
        
        // Crear transacción de puntos
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customerId(referrer.getId())
                .businessId(referral.getBusinessId())
                .pointsAwarded(pointsToAward)
                .transactionType(LoyaltyTransaction.TransactionType.REFERRAL)
                .expiresAt(calculateExpirationDate(referral.getBusinessId()))
                .build();
        
        // Guardar transacción
        LoyaltyTransaction savedTransaction = loyaltyTransactionRepository.save(transaction);
        
        // Actualizar puntos del cliente referente
        referrer.addLoyaltyPoints(pointsToAward);
        customerRepository.save(referrer);
        
        // Marcar el referido como convertido y guardar los puntos otorgados
        referral.convert(pointsToAward);
        referralRepository.save(referral);
        
        log.info("Puntos otorgados por referido: {} puntos para cliente {}", 
                pointsToAward, referrer.getId());
        
        return savedTransaction;
    }

    /**
     * Canjear puntos por una recompensa
     * @param customerId ID del cliente
     * @param rewardItemId ID de la recompensa
     * @return Redención creada
     */
    @Transactional
    public RewardRedemption redeemPoints(UUID customerId, UUID rewardItemId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + customerId));
        
        RewardItem reward = rewardItemRepository.findById(rewardItemId)
                .orElseThrow(() -> new RuntimeException("Recompensa no encontrada: " + rewardItemId));
        
        // Verificar que la recompensa esté activa
        if (reward.getStatus() != RewardItem.RewardStatus.ACTIVE) {
            throw new IllegalStateException("La recompensa no está disponible actualmente");
        }
        
        // Verificar que haya stock si aplica
        if (!reward.isAvailable()) {
            throw new IllegalStateException("La recompensa está agotada");
        }
        
        // Verificar que el cliente tenga suficientes puntos
        if (customer.getLoyaltyPoints() < reward.getPointsCost()) {
            throw new IllegalStateException("Puntos insuficientes para esta recompensa");
        }
        
        // Crear redención
        RewardRedemption redemption = RewardRedemption.builder()
                .customerId(customerId)
                .businessId(reward.getBusinessId())
                .rewardItemId(rewardItemId)
                .pointsSpent(reward.getPointsCost())
                .status(RewardRedemption.RedemptionStatus.PENDING)
                .redeemedAt(LocalDateTime.now())
                .build();
        
        // Generar código de redención
        redemption.generateRedemptionCode();
        
        // Para recompensas que expiran, establecer fecha
        if (reward.getType() == RewardItem.RewardType.DISCOUNT || 
            reward.getType() == RewardItem.RewardType.EVENT) {
            redemption.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30 días por defecto
        }
        
        // Guardar redención
        RewardRedemption savedRedemption = rewardRedemptionRepository.save(redemption);
        
        // Reducir puntos del cliente
        customer.redeemLoyaltyPoints(reward.getPointsCost());
        customerRepository.save(customer);
        
        // Registrar transacción de puntos (negativa)
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customerId(customerId)
                .businessId(reward.getBusinessId())
                .pointsAwarded(-reward.getPointsCost())
                .transactionType(LoyaltyTransaction.TransactionType.REDEMPTION)
                .build();
        loyaltyTransactionRepository.save(transaction);
        
        // Actualizar stock de la recompensa si aplica
        reward.decrementStock();
        rewardItemRepository.save(reward);
        
        log.info("Puntos canjeados: {} puntos por recompensa {}", 
                reward.getPointsCost(), rewardItemId);
        
        return savedRedemption;
    }

    /**
     * Marcar una redención como completada
     * @param redemptionId ID de la redención
     * @return Redención actualizada
     */
    @Transactional
    public RewardRedemption completeRedemption(UUID redemptionId) {
        RewardRedemption redemption = rewardRedemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new RuntimeException("Redención no encontrada: " + redemptionId));
        
        if (!redemption.complete()) {
            throw new IllegalStateException("No se puede completar la redención en su estado actual");
        }
        
        RewardRedemption updatedRedemption = rewardRedemptionRepository.save(redemption);
        log.info("Redención completada: {}", redemptionId);
        
        return updatedRedemption;
    }

    /**
     * Cancelar una redención
     * @param redemptionId ID de la redención
     * @return Redención actualizada
     */
    @Transactional
    public RewardRedemption cancelRedemption(UUID redemptionId) {
        RewardRedemption redemption = rewardRedemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new RuntimeException("Redención no encontrada: " + redemptionId));
        
        if (!redemption.cancel()) {
            throw new IllegalStateException("No se puede cancelar la redención en su estado actual");
        }
        
        // Recuperar los puntos gastados al cliente
        Customer customer = customerRepository.findById(redemption.getCustomerId())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        
        customer.addLoyaltyPoints(redemption.getPointsSpent());
        customerRepository.save(customer);
        
        // Registrar transacción de devolución de puntos
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customerId(customer.getId())
                .businessId(redemption.getBusinessId())
                .pointsAwarded(redemption.getPointsSpent())
                .transactionType(LoyaltyTransaction.TransactionType.MANUAL)
                .build();
        loyaltyTransactionRepository.save(transaction);
        
        // Recuperar stock de la recompensa
        RewardItem reward = rewardItemRepository.findById(redemption.getRewardItemId())
                .orElseThrow(() -> new RuntimeException("Recompensa no encontrada"));
        
        reward.incrementStock(1);
        rewardItemRepository.save(reward);
        
        RewardRedemption updatedRedemption = rewardRedemptionRepository.save(redemption);
        log.info("Redención cancelada: {}", redemptionId);
        
        return updatedRedemption;
    }

    /**
     * Obtener historial de transacciones de puntos de un cliente
     * @param customerId ID del cliente
     * @param pageable Paginación
     * @return Página de transacciones
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyTransaction> getCustomerTransactions(UUID customerId, Pageable pageable) {
        return loyaltyTransactionRepository.findByCustomerId(customerId, pageable);
    }

    /**
     * Obtener historial de recompensas canjeadas por un cliente
     * @param customerId ID del cliente
     * @param pageable Paginación
     * @return Página de redenciones
     */
    @Transactional(readOnly = true)
    public Page<RewardRedemption> getCustomerRedemptions(UUID customerId, Pageable pageable) {
        return rewardRedemptionRepository.findByCustomerId(customerId, pageable);
    }

    /**
     * Obtener puntos actuales de un cliente
     * @param customerId ID del cliente
     * @return Puntos actuales
     */
    @Transactional(readOnly = true)
    public int getCustomerPoints(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + customerId));
        
        return customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
    }

    /**
     * Obtener puntos activos (no expirados) de un cliente
     * @param customerId ID del cliente
     * @param businessId ID del negocio
     * @return Puntos activos
     */
    @Transactional(readOnly = true)
    public int getActivePoints(UUID customerId, UUID businessId) {
        return loyaltyTransactionRepository.sumActivePoints(customerId, businessId, LocalDateTime.now());
    }

    /**
     * Crear un nuevo referido
     * @param referrerCode Código del referente
     * @param referredCustomerId ID del cliente referido
     * @param businessId ID del negocio
     * @return Referido creado
     */
    @Transactional
    public Referral createReferral(String referrerCode, UUID referredCustomerId, UUID businessId) {
        // Buscar el cliente referente por código
        Customer referrer = customerRepository.findByReferralCode(referrerCode)
                .orElseThrow(() -> new RuntimeException("Código de referido inválido"));
        
        // Buscar el cliente referido
        Customer referred = customerRepository.findById(referredCustomerId)
                .orElseThrow(() -> new RuntimeException("Cliente referido no encontrado"));
        
        // Verificar que el negocio coincida con el del referente
        if (!referrer.getBusinessId().equals(businessId)) {
            throw new IllegalArgumentException("El código de referido no pertenece a este negocio");
        }
        
        // Verificar que no sea auto-referencia
        if (referrer.getId().equals(referredCustomerId)) {
            throw new IllegalArgumentException("Un cliente no puede referirse a sí mismo");
        }
        
        // Verificar que no exista ya un referido entre estos clientes
        Optional<Referral> existingReferral = referralRepository.findByReferrerCustomerIdAndReferredCustomerId(
                referrer.getId(), referredCustomerId);
        
        if (existingReferral.isPresent()) {
            throw new IllegalStateException("Ya existe un referido entre estos clientes");
        }
        
        // Crear referido
        Referral referral = Referral.builder()
                .referrerCustomerId(referrer.getId())
                .referredCustomerId(referredCustomerId)
                .businessId(businessId)
                .status(Referral.ReferralStatus.PENDING)
                .build();
        
        Referral savedReferral = referralRepository.save(referral);
        log.info("Referido creado: {} -> {}", referrer.getId(), referredCustomerId);
        
        return savedReferral;
    }

    /**
     * Ajustar manualmente los puntos de un cliente
     * @param customerId ID del cliente
     * @param points Puntos a ajustar (positivo o negativo)
     * @param description Descripción del ajuste
     * @return Transacción creada
     */
    @Transactional
    public LoyaltyTransaction adjustPoints(UUID customerId, int points, String description) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + customerId));
        
        // Validar que no resulte en puntos negativos
        if (points < 0 && (customer.getLoyaltyPoints() + points) < 0) {
            throw new IllegalArgumentException("El ajuste resultaría en puntos negativos");
        }
        
        // Crear transacción
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customerId(customerId)
                .businessId(customer.getBusinessId())
                .pointsAwarded(points)
                .transactionType(LoyaltyTransaction.TransactionType.MANUAL)
                .build();
        
        // Establecer fecha de expiración solo si son puntos positivos
        if (points > 0) {
            transaction.setExpiresAt(calculateExpirationDate(customer.getBusinessId()));
        }
        
        // Guardar transacción
        LoyaltyTransaction savedTransaction = loyaltyTransactionRepository.save(transaction);
        
        // Actualizar puntos del cliente
        if (points > 0) {
            customer.addLoyaltyPoints(points);
        } else {
            customer.redeemLoyaltyPoints(Math.abs(points));
        }
        customerRepository.save(customer);
        
        log.info("Ajuste manual de puntos: {} para cliente {}", points, customerId);
        
        return savedTransaction;
    }

    /**
     * Tarea programada para procesar puntos expirados diariamente
     */
    @Scheduled(cron = "0 0 3 * * ?") // 3 AM todos los días
    @Transactional
    public void processExpiredPoints() {
        log.info("Iniciando procesamiento de puntos expirados");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Buscar transacciones expiradas
        List<LoyaltyTransaction> expiringTransactions = loyaltyTransactionRepository.findAboutToExpire(
                now.minusDays(1), now);
        
        for (LoyaltyTransaction transaction : expiringTransactions) {
            if (transaction.isPositive()) {
                Customer customer = customerRepository.findById(transaction.getCustomerId()).orElse(null);
                
                if (customer != null) {
                    // Crear transacción de expiración negativa
                    LoyaltyTransaction expirationTransaction = LoyaltyTransaction.builder()
                            .customerId(transaction.getCustomerId())
                            .businessId(transaction.getBusinessId())
                            .pointsAwarded(-transaction.getPointsAwarded())
                            .transactionType(LoyaltyTransaction.TransactionType.EXPIRATION)
                            .build();
                    
                    loyaltyTransactionRepository.save(expirationTransaction);
                    
                    // Actualizar puntos del cliente
                    customer.redeemLoyaltyPoints(transaction.getPointsAwarded());
                    customerRepository.save(customer);
                    
                    log.info("Puntos expirados: {} para cliente {}", 
                            transaction.getPointsAwarded(), customer.getId());
                }
            }
        }
        
        log.info("Procesamiento de puntos expirados completado");
    }

    /**
     * Calcular fecha de expiración de puntos basada en configuración del negocio
     * @param businessId ID del negocio
     * @return Fecha de expiración
     */
    private LocalDateTime calculateExpirationDate(UUID businessId) {
        Integer expirationDays = getPointsExpirationDays(businessId);
        
        if (expirationDays != null && expirationDays > 0) {
            return LocalDateTime.now().plusDays(expirationDays);
        }
        
        // Por defecto, 365 días si no hay configuración
        return LocalDateTime.now().plusDays(365);
    }

    /**
     * Obtener configuración de puntos por reserva
     * @param businessId ID del negocio
     * @return Puntos por reserva
     */
    private int getPointsPerBooking(UUID businessId) {
        Optional<BusinessSetting> setting = businessSettingRepository.findByBusinessIdAndSettingKey(
                businessId, BusinessSetting.SettingKeys.LOYALTY_POINTS_PER_BOOKING);
        
        return setting.map(s -> s.getValueAsInteger(10)).orElse(10); // 10 puntos por defecto
    }

    /**
     * Obtener bonificación por primera reserva
     * @param businessId ID del negocio
     * @return Puntos de bonificación
     */
    private int getFirstBookingBonus(UUID businessId) {
        Optional<BusinessSetting> setting = businessSettingRepository.findByBusinessIdAndSettingKey(
                businessId, BusinessSetting.SettingKeys.LOYALTY_FIRST_BOOKING_BONUS);
        
        return setting.map(s -> s.getValueAsInteger(20)).orElse(20); // 20 puntos por defecto
    }

    /**
     * Obtener puntos por referido
     * @param businessId ID del negocio
     * @return Puntos por referido
     */
    private int getReferralPoints(UUID businessId) {
        Optional<BusinessSetting> setting = businessSettingRepository.findByBusinessIdAndSettingKey(
                businessId, BusinessSetting.SettingKeys.LOYALTY_REFERRAL_POINTS);
        
        return setting.map(s -> s.getValueAsInteger(50)).orElse(50); // 50 puntos por defecto
    }

    /**
     * Obtener días de expiración de puntos
     * @param businessId ID del negocio
     * @return Días de expiración
     */
    private Integer getPointsExpirationDays(UUID businessId) {
        Optional<BusinessSetting> setting = businessSettingRepository.findByBusinessIdAndSettingKey(
                businessId, BusinessSetting.SettingKeys.LOYALTY_POINTS_EXPIRATION_DAYS);
        
        return setting.map(s -> s.getValueAsInteger(365)).orElse(365); // 365 días por defecto
    }
}