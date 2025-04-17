-- Módulo de negocios y configuración

-- Tabla de negocios (en esquema público)
CREATE TABLE IF NOT EXISTS public.businesses (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- Tipo de negocio (peluquería, consultorio, restaurante, etc.)
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    subscription_status VARCHAR(20) NOT NULL DEFAULT 'TRIAL', -- TRIAL, ACTIVE, SUSPENDED, CANCELED
    subscription_plan VARCHAR(20) NOT NULL DEFAULT 'BASIC', -- BASIC, STANDARD, PREMIUM
    trial_ends_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT businesses_email_unique UNIQUE (contact_email)
);

-- Índices para negocios
CREATE INDEX IF NOT EXISTS idx_businesses_type ON public.businesses(type);
CREATE INDEX IF NOT EXISTS idx_businesses_subscription_status ON public.businesses(subscription_status);

-- Tabla de módulos activos por negocio
CREATE TABLE IF NOT EXISTS public.business_modules (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    module_name VARCHAR(50) NOT NULL, -- loyalty, payment, notification, document, analytics
    active BOOLEAN NOT NULL DEFAULT FALSE,
    settings JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT business_modules_unique UNIQUE (business_id, module_name),
    CONSTRAINT fk_business_modules_business FOREIGN KEY (business_id) REFERENCES public.businesses(id)
);

-- Índices para business_modules
CREATE INDEX IF NOT EXISTS idx_business_modules_business_id ON public.business_modules(business_id);
CREATE INDEX IF NOT EXISTS idx_business_modules_active ON public.business_modules(active);

-- Tabla de ubicaciones de negocio
CREATE TABLE IF NOT EXISTS public.business_locations (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    phone VARCHAR(50),
    email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_business_locations_business FOREIGN KEY (business_id) REFERENCES public.businesses(id)
);

-- Índices para business_locations
CREATE INDEX IF NOT EXISTS idx_business_locations_business_id ON public.business_locations(business_id);

-- Tabla de configuración por negocio
CREATE TABLE IF NOT EXISTS public.business_settings (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT business_settings_unique UNIQUE (business_id, setting_key),
    CONSTRAINT fk_business_settings_business FOREIGN KEY (business_id) REFERENCES public.businesses(id)
);

-- Índices para business_settings
CREATE INDEX IF NOT EXISTS idx_business_settings_business_id ON public.business_settings(business_id);
CREATE INDEX IF NOT EXISTS idx_business_settings_key ON public.business_settings(setting_key);

-- Tabla de facturación para negocios
CREATE TABLE IF NOT EXISTS public.business_billing (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, OVERDUE, CANCELED
    payment_method VARCHAR(50),
    payment_date TIMESTAMP,
    invoice_number VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_business_billing_business FOREIGN KEY (business_id) REFERENCES public.businesses(id)
);

-- Índices para business_billing
CREATE INDEX IF NOT EXISTS idx_business_billing_business_id ON public.business_billing(business_id);
CREATE INDEX IF NOT EXISTS idx_business_billing_status ON public.business_billing(status);
CREATE INDEX IF NOT EXISTS idx_business_billing_period ON public.business_billing(billing_period_start, billing_period_end);

-- Creación de tablas para campos personalizados por negocio
CREATE TABLE IF NOT EXISTS public.custom_field_definitions (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL, -- TEXT, NUMBER, DATE, DROPDOWN, CHECKBOX, etc.
    required BOOLEAN NOT NULL DEFAULT FALSE,
    possible_values TEXT, -- Valores separados por comas para dropdown o opciones múltiples
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT custom_field_definitions_unique UNIQUE (business_id, name),
    CONSTRAINT fk_custom_field_definitions_business FOREIGN KEY (business_id) REFERENCES public.businesses(id)
);

-- Índices para custom_field_definitions
CREATE INDEX IF NOT EXISTS idx_custom_field_definitions_business_id ON public.custom_field_definitions(business_id);

-- Tabla de políticas de retención de documentos por negocio
CREATE TABLE IF NOT EXISTS public.business_retention_policies (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    retention_days INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT business_retention_policies_unique UNIQUE (business_id, document_type),
    CONSTRAINT fk_business_retention_policies_business FOREIGN KEY (business_id) REFERENCES public.businesses(id)
);

-- Índices para business_retention_policies
CREATE INDEX IF NOT EXISTS idx_business_retention_policies_business_id ON public.business_retention_policies(business_id);

-- Tabla de categorías de servicios
CREATE TABLE IF NOT EXISTS public.service_categories (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_service_categories_business FOREIGN KEY (business_id) REFERENCES public.businesses(id)
);

-- Índices para service_categories
CREATE INDEX IF NOT EXISTS idx_service_categories_business_id ON public.service_categories(business_id);

-- Insertar negocio de ejemplo para desarrollo
INSERT INTO public.businesses (
    id, name, type, contact_email, contact_phone, time_zone, 
    subscription_status, subscription_plan, trial_ends_at
)
VALUES (
    '98f0f1d7-41a7-4b1a-8348-99d9a566682c',
    'Salón de Belleza Demo',
    'SALON',
    'demo@bookingsaas.com',
    '+1234567890',
    'America/New_York',
    'TRIAL',
    'BASIC',
    CURRENT_TIMESTAMP + INTERVAL '30 days'
)
ON CONFLICT (id) DO NOTHING;

-- Activar módulos para el negocio de ejemplo
INSERT INTO public.business_modules (
    id, business_id, module_name, active
)
VALUES 
    (gen_random_uuid(), '98f0f1d7-41a7-4b1a-8348-99d9a566682c', 'loyalty', true),
    (gen_random_uuid(), '98f0f1d7-41a7-4b1a-8348-99d9a566682c', 'payment', true),
    (gen_random_uuid(), '98f0f1d7-41a7-4b1a-8348-99d9a566682c', 'notification', true),
    (gen_random_uuid(), '98f0f1d7-41a7-4b1a-8348-99d9a566682c', 'document', false),
    (gen_random_uuid(), '98f0f1d7-41a7-4b1a-8348-99d9a566682c', 'analytics', false)
ON CONFLICT (business_id, module_name) DO NOTHING;