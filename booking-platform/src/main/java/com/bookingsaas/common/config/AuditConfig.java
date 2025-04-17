package com.bookingsaas.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * Configuración para habilitar la auditoría automática en JPA
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    /**
     * Bean que proporciona el auditor actual basado en la autenticación de Spring Security
     */
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.empty();
            }
            
            // Extraer el ID de usuario del token JWT
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                String subject = jwt.getSubject();
                // El subject puede ser el ID de usuario o un identificador de Auth0/Firebase
                try {
                    return Optional.of(UUID.fromString(subject));
                } catch (IllegalArgumentException e) {
                    // Si no es un UUID válido, generamos uno basado en el hash del subject
                    return Optional.of(UUID.nameUUIDFromBytes(subject.getBytes()));
                }
            }
            
            // Fallback para autenticación simulada en desarrollo
            String username = authentication.getName();
            return Optional.of(UUID.nameUUIDFromBytes(username.getBytes()));
        };
    }
}