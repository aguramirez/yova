package com.bookingsaas.module.identity.domain.entity;

import com.bookingsaas.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un rol de usuario en el sistema
 */
@Entity
@Table(name = "roles", schema = "public")
@SQLDelete(sql = "UPDATE public.roles SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolePermission> permissions = new HashSet<>();

    /**
     * Constantes para los nombres de roles predefinidos
     */
    public static final class RoleNames {
        public static final String SUPER_ADMIN = "SUPER_ADMIN";
        public static final String BUSINESS_ADMIN = "BUSINESS_ADMIN";
        public static final String BUSINESS_MANAGER = "BUSINESS_MANAGER";
        public static final String PROFESSIONAL = "PROFESSIONAL";
        public static final String RECEPTIONIST = "RECEPTIONIST";
        public static final String CUSTOMER = "CUSTOMER";
        public static final String ACCOUNTANT = "ACCOUNTANT";
        public static final String MARKETING_MANAGER = "MARKETING_MANAGER";

        private RoleNames() {
            // Constructor privado para evitar instanciación
        }
    }

    /**
     * Verifica si el rol tiene un permiso específico
     * @param permissionName Nombre del permiso a verificar
     * @return true si el rol tiene el permiso
     */
    public boolean hasPermission(String permissionName) {
        return permissions.stream()
                .anyMatch(permission -> permission.getPermission().getName().equals(permissionName));
    }

    /**
     * Agrega un permiso al rol
     * @param permission Permiso a agregar
     */
    public void addPermission(Permission permission) {
        RolePermission rolePermission = RolePermission.builder()
                .role(this)
                .permission(permission)
                .build();
        permissions.add(rolePermission);
    }

    /**
     * Remueve un permiso del rol
     * @param permissionId ID del permiso a remover
     */
    public void removePermission(Permission permission) {
        permissions.removeIf(rp -> rp.getPermission().equals(permission));
    }
}