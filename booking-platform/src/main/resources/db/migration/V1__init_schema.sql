-- Inicialización de esquemas para multi-tenancy

-- Habilitar extensión UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Esquema público para tablas compartidas
CREATE SCHEMA IF NOT EXISTS public;

-- Esquema para el primer negocio de ejemplo
CREATE SCHEMA IF NOT EXISTS business_1;

-- Función para crear automáticamente esquemas para nuevos negocios
CREATE OR REPLACE FUNCTION create_business_schema(schema_name VARCHAR)
RETURNS VOID AS $$
BEGIN
    EXECUTE 'CREATE SCHEMA IF NOT EXISTS ' || schema_name;
END;
$$ LANGUAGE plpgsql;

-- Tabla de control de esquemas por negocio (en esquema público)
CREATE TABLE IF NOT EXISTS public.business_schemas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL,
    schema_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT business_schemas_schema_name_unique UNIQUE (schema_name),
    CONSTRAINT business_schemas_business_id_unique UNIQUE (business_id)
);

-- Índices para la tabla de esquemas
CREATE INDEX IF NOT EXISTS idx_business_schemas_business_id ON public.business_schemas(business_id);
CREATE INDEX IF NOT EXISTS idx_business_schemas_schema_name ON public.business_schemas(schema_name);

-- Insertar esquema de ejemplo
INSERT INTO public.business_schemas (id, business_id, schema_name, active)
VALUES (
    '2f01639c-75a3-4150-a47e-54d9b6d5f322',
    '98f0f1d7-41a7-4b1a-8348-99d9a566682c',
    'business_1',
    true
)
ON CONFLICT (schema_name) DO NOTHING;

-- Tabla de configuración global del sistema
CREATE TABLE IF NOT EXISTS public.system_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT system_settings_key_unique UNIQUE (setting_key)
);

-- Tabla de versiones por esquema
CREATE TABLE IF NOT EXISTS public.schema_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    schema_name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    CONSTRAINT schema_versions_schema_version_unique UNIQUE (schema_name, version)
);

-- Inserta configuración inicial del sistema
INSERT INTO public.system_settings (id, setting_key, setting_value, description)
VALUES
    (uuid_generate_v4(), 'system.version', '1.0.0', 'Versión actual del sistema'),
    (uuid_generate_v4(), 'system.maintenance_mode', 'false', 'Sistema en modo mantenimiento'),
    (uuid_generate_v4(), 'email.sender', 'noreply@bookingsaas.com', 'Email remitente para notificaciones'),
    (uuid_generate_v4(), 'system.default_timezone', 'UTC', 'Zona horaria por defecto')
ON CONFLICT (setting_key) DO NOTHING;