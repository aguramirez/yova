package com.bookingsaas.module.identity.domain.entity;

import com.bookingsaas.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad que representa a un usuario del sistema
 */
@Entity
@Table(name = "users", schema = "public")
@SQLDelete(sql = "UPDATE public.users SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "auth_provider_id", nullable = false, unique = true)
    private String authProviderId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserBusinessRole> businessRoles = new HashSet<>();

    /**
     * Obtiene el nombre completo del usuario
     * @return Nombre completo (nombre + apellido)
     */
    @Transient
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        
        if (firstName != null && !firstName.isBlank()) {
            sb.append(firstName);
        }
        
        if (lastName != null && !lastName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName);
        }
        
        return sb.toString();
    }

    /**
     * Verifica si el usuario tiene un rol específico en un negocio
     * @param businessId ID del negocio
     * @param roleName Nombre del rol a verificar
     * @return true si el usuario tiene el rol en el negocio
     */
    public boolean hasRole(UUID businessId, String roleName) {
        return businessRoles.stream()
                .filter(br -> br.getBusiness().getId().equals(businessId))
                .anyMatch(br -> br.getRole().getName().equals(roleName));
    }

    /**
     * Asigna un rol a un usuario en un negocio específico
     * @param business Negocio
     * @param role Rol a asignar
     */
    public void addBusinessRole(com.bookingsaas.module.business.domain.entity.Business business, Role role) {
        UserBusinessRole userBusinessRole = UserBusinessRole.builder()
                .user(this)
                .business(business)
                .role(role)
                .build();
        businessRoles.add(userBusinessRole);
    }

    /**
     * Remueve un rol de un usuario en un negocio específico
     * @param businessId ID del negocio
     * @param roleId ID del rol a remover
     */
    public void removeBusinessRole(UUID businessId, UUID roleId) {
        businessRoles.removeIf(br -> 
            br.getBusiness().getId().equals(businessId) && 
            br.getRole().getId().equals(roleId)
        );
    }
}