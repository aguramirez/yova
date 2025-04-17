package com.bookingsaas.module.identity.domain.entity;

import com.bookingsaas.common.domain.entity.AuditableEntity;
import com.bookingsaas.module.business.domain.entity.Business;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Entidad que relaciona usuarios, negocios y roles
 */
@Entity
@Table(name = "user_business_roles", schema = "public")
@SQLDelete(sql = "UPDATE public.user_business_roles SET deleted = true, deleted_at = now() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserBusinessRole extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    /**
     * Verifica si el rol asociado tiene un permiso específico
     * @param permissionName Nombre del permiso a verificar
     * @return true si el rol tiene el permiso
     */
    public boolean hasPermission(String permissionName) {
        return role.hasPermission(permissionName);
    }
}