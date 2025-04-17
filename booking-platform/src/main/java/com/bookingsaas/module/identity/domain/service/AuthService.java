package com.bookingsaas.module.identity.domain.service;

import com.bookingsaas.module.business.domain.entity.Business;
import com.bookingsaas.module.business.domain.repository.BusinessRepository;
import com.bookingsaas.module.identity.domain.entity.Role;
import com.bookingsaas.module.identity.domain.entity.User;
import com.bookingsaas.module.identity.domain.repository.RoleRepository;
import com.bookingsaas.module.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para gestionar autenticación y autorización
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BusinessRepository businessRepository;

    /**
     * Registrar un nuevo usuario o actualizar si ya existe por su ID externo
     * @param email Email del usuario
     * @param firstName Nombre del usuario
     * @param lastName Apellido del usuario
     * @param authProviderId ID en Auth0/Firebase
     * @return Usuario creado o actualizado
     */
    @Transactional
    public User registerUser(String email, String firstName, String lastName, String authProviderId) {
        // Verificar si ya existe por authProviderId
        Optional<User> existingUserByAuth = userRepository.findByAuthProviderId(authProviderId);
        if (existingUserByAuth.isPresent()) {
            User user = existingUserByAuth.get();
            
            // Actualizar información si cambió
            boolean updated = false;
            
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                updated = true;
            }
            
            if (firstName != null && !firstName.equals(user.getFirstName())) {
                user.setFirstName(firstName);
                updated = true;
            }
            
            if (lastName != null && !lastName.equals(user.getLastName())) {
                user.setLastName(lastName);
                updated = true;
            }
            
            if (updated) {
                log.info("Actualizando información de usuario existente: {}", email);
                return userRepository.save(user);
            }
            
            return user;
        }
        
        // Verificar si ya existe por email
        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            User user = existingUserByEmail.get();
            user.setAuthProviderId(authProviderId);
            
            if (firstName != null && !firstName.equals(user.getFirstName())) {
                user.setFirstName(firstName);
            }
            
            if (lastName != null && !lastName.equals(user.getLastName())) {
                user.setLastName(lastName);
            }
            
            log.info("Vinculando usuario existente con nuevo proveedor de autenticación: {}", email);
            return userRepository.save(user);
        }
        
        // Crear nuevo usuario
        User newUser = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .authProviderId(authProviderId)
                .active(true)
                .build();
        
        log.info("Creando nuevo usuario: {}", email);
        return userRepository.save(newUser);
    }

    /**
     * Asignar un rol a un usuario en un negocio
     * @param userId ID del usuario
     * @param businessId ID del negocio
     * @param roleName Nombre del rol
     * @return Usuario actualizado
     */
    @Transactional
    public User assignRoleToUser(UUID userId, UUID businessId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));
        
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado con ID: " + businessId));
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + roleName));
        
        // Verificar si ya tiene este rol
        if (user.hasRole(businessId, roleName)) {
            log.info("El usuario ya tiene el rol {} en el negocio {}", roleName, businessId);
            return user;
        }
        
        // Asignar nuevo rol
        user.addBusinessRole(business, role);
        
        log.info("Rol {} asignado al usuario {} en negocio {}", roleName, userId, businessId);
        return userRepository.save(user);
    }

    /**
     * Verificar si un usuario tiene un rol específico en un negocio
     * @param userId ID del usuario
     * @param businessId ID del negocio
     * @param roleName Nombre del rol
     * @return true si tiene el rol
     */
    @Transactional(readOnly = true)
    public boolean userHasRole(UUID userId, UUID businessId, String roleName) {
        return userRepository.hasRole(userId, businessId, roleName);
    }

    /**
     * Verificar si un permiso está disponible para un usuario en un negocio
     * @param userId ID del usuario
     * @param businessId ID del negocio
     * @param permissionName Nombre del permiso
     * @return true si tiene el permiso
     */
    @Transactional(readOnly = true)
    public boolean userHasPermission(UUID userId, UUID businessId, String permissionName) {
        // Implementación simple - en producción esto debería optimizarse con consultas específicas
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));
        
        return user.getBusinessRoles().stream()
                .filter(br -> br.getBusiness().getId().equals(businessId))
                .anyMatch(br -> br.hasPermission(permissionName));
    }

    /**
     * Obtener el usuario autenticado actualmente
     * @return Usuario autenticado o null si no hay autenticación
     */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        
        // Extraer identificador del token JWT
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            
            // Intentar encontrar por authProviderId
            Optional<User> user = userRepository.findByAuthProviderId(subject);
            if (user.isPresent()) {
                return user.get();
            }
            
            // Si no se encuentra, podría ser un token de desarrollo
            log.warn("Usuario autenticado no encontrado en base de datos: {}", subject);
            return null;
        }
        
        // Para pruebas/desarrollo: si es autenticación simple, usar el nombre
        String username = authentication.getName();
        return userRepository.findByEmail(username).orElse(null);
    }

    /**
     * Desactivar un usuario
     * @param userId ID del usuario
     */
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));
        
        user.setActive(false);
        userRepository.save(user);
        log.info("Usuario desactivado: {}", userId);
    }

    /**
     * Reactivar un usuario
     * @param userId ID del usuario
     */
    @Transactional
    public void reactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));
        
        user.setActive(true);
        userRepository.save(user);
        log.info("Usuario reactivado: {}", userId);
    }
}