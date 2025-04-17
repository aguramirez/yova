-- Módulo de identidad y seguridad

-- Tabla de usuarios (compartida en esquema público)
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    auth_provider_id VARCHAR(255) NOT NULL, -- ID externo de Auth0/Firebase
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_auth_provider_id_unique UNIQUE (auth_provider_id)
);

-- Índices para usuarios
CREATE INDEX IF NOT EXISTS idx_users_email ON public.users(email);
CREATE INDEX IF NOT EXISTS idx_users_auth_provider_id ON public.users(auth_provider_id);

-- Tabla de roles del sistema
CREATE TABLE IF NOT EXISTS public.roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT roles_name_unique UNIQUE (name)
);

-- Insertar roles predefinidos
INSERT INTO public.roles (id, name, description)
VALUES 
    ('3fa85f64-5717-4562-b3fc-2c963f66afa6', 'SUPER_ADMIN', 'Administrador de toda la plataforma'),
    ('4fa85f64-5717-4562-b3fc-2c963f66afa7', 'BUSINESS_ADMIN', 'Propietario o administrador de un negocio'),
    ('5fa85f64-5717-4562-b3fc-2c963f66afa8', 'BUSINESS_MANAGER', 'Rol gerencial con permisos limitados'),
    ('6fa85f64-5717-4562-b3fc-2c963f66afa9', 'PROFESSIONAL', 'Proveedor de servicios (peluquero, médico, etc.)'),
    ('7fa85f64-5717-4562-b3fc-2c963f66afaa', 'RECEPTIONIST', 'Personal de recepción que gestiona citas'),
    ('8fa85f64-5717-4562-b3fc-2c963f66afab', 'CUSTOMER', 'Cliente final que utiliza los servicios'),
    ('9fa85f64-5717-4562-b3fc-2c963f66afac', 'ACCOUNTANT', 'Acceso a reportes financieros'),
    ('afa85f64-5717-4562-b3fc-2c963f66afad', 'MARKETING_MANAGER', 'Gestión de campañas y promociones')
ON CONFLICT (name) DO NOTHING;

-- Tabla de asignación de roles a usuarios por negocio
CREATE TABLE IF NOT EXISTS public.user_business_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    business_id UUID NOT NULL,
    role_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT user_business_roles_unique UNIQUE (user_id, business_id, role_id),
    CONSTRAINT fk_user_business_roles_user FOREIGN KEY (user_id) REFERENCES public.users(id),
    CONSTRAINT fk_user_business_roles_role FOREIGN KEY (role_id) REFERENCES public.roles(id)
);

-- Índices para user_business_roles
CREATE INDEX IF NOT EXISTS idx_user_business_roles_user_id ON public.user_business_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_business_roles_business_id ON public.user_business_roles(business_id);
CREATE INDEX IF NOT EXISTS idx_user_business_roles_role_id ON public.user_business_roles(role_id);

-- Permisos del sistema
CREATE TABLE IF NOT EXISTS public.permissions (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT permissions_name_unique UNIQUE (name)
);

-- Relación entre roles y permisos
CREATE TABLE IF NOT EXISTS public.role_permissions (
    id UUID PRIMARY KEY,
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT role_permissions_unique UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES public.roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES public.permissions(id)
);

-- Índices para role_permissions
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON public.role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON public.role_permissions(permission_id);

-- Tabla de tokens de refresco (si se implementa auth local)
CREATE TABLE IF NOT EXISTS public.refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    CONSTRAINT refresh_tokens_token_unique UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- Índices para refresh_tokens
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON public.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON public.refresh_tokens(token);

-- Tabla de registro de actividad de seguridad
CREATE TABLE IF NOT EXISTS public.security_audit_log (
    id UUID PRIMARY KEY,
    user_id UUID,
    event_type VARCHAR(50) NOT NULL,
    description TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    business_id UUID,
    CONSTRAINT fk_security_audit_log_user FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- Índices para security_audit_log
CREATE INDEX IF NOT EXISTS idx_security_audit_log_user_id ON public.security_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_security_audit_log_created_at ON public.security_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_security_audit_log_event_type ON public.security_audit_log(event_type);
CREATE INDEX IF NOT EXISTS idx_security_audit_log_business_id ON public.security_audit_log(business_id);