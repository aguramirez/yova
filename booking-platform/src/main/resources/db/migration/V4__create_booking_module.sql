-- Módulo de reservas y clientes
-- Este script crea las tablas para cada esquema de negocio

-- Función para crear tablas del módulo en cada esquema de negocio
CREATE OR REPLACE FUNCTION create_booking_module_tables(schema_name VARCHAR)
RETURNS VOID AS $$
BEGIN
    -- Tabla de clientes
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.customers (
        id UUID PRIMARY KEY,
        business_id UUID NOT NULL,
        first_name VARCHAR(100) NOT NULL,
        last_name VARCHAR(100) NOT NULL,
        email VARCHAR(255),
        phone VARCHAR(50),
        loyalty_points INTEGER NOT NULL DEFAULT 0,
        referral_code VARCHAR(20),
        total_appointments INTEGER NOT NULL DEFAULT 0,
        appointments_attended INTEGER NOT NULL DEFAULT 0,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at TIMESTAMP,
        deleted_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT customers_email_business_unique UNIQUE (business_id, email),
        CONSTRAINT customers_phone_business_unique UNIQUE (business_id, phone),
        CONSTRAINT customers_referral_code_unique UNIQUE (referral_code)
    )';
    
    -- Tabla de valores de campos personalizados para clientes
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.customer_custom_fields (
        id UUID PRIMARY KEY,
        customer_id UUID NOT NULL,
        field_definition_id UUID NOT NULL,
        field_value TEXT,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT customer_custom_fields_unique UNIQUE (customer_id, field_definition_id),
        CONSTRAINT fk_customer_custom_fields_customer FOREIGN KEY (customer_id) REFERENCES ' || schema_name || '.customers(id)
    )';
    
    -- Tabla de profesionales
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.professionals (
        id UUID PRIMARY KEY,
        business_id UUID NOT NULL,
        user_id UUID,
        name VARCHAR(255) NOT NULL,
        specialization VARCHAR(255),
        active BOOLEAN NOT NULL DEFAULT TRUE,
        google_calendar_id VARCHAR(255),
        attendance_rate DECIMAL(5,2),
        average_service_time INTEGER,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at TIMESTAMP,
        deleted_by UUID,
        version BIGINT NOT NULL DEFAULT 0
    )';
    
    -- Tabla de recursos (salas, equipos, etc.)
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.resources (
        id UUID PRIMARY KEY,
        business_id UUID NOT NULL,
        name VARCHAR(255) NOT NULL,
        type VARCHAR(50) NOT NULL,
        capacity INTEGER,
        location VARCHAR(255),
        active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at TIMESTAMP,
        deleted_by UUID,
        version BIGINT NOT NULL DEFAULT 0
    )';
    
    -- Tabla de servicios
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.services (
        id UUID PRIMARY KEY,
        business_id UUID NOT NULL,
        name VARCHAR(255) NOT NULL,
        description TEXT,
        price DECIMAL(10,2) NOT NULL,
        duration_minutes INTEGER NOT NULL,
        category_id UUID,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at TIMESTAMP,
        deleted_by UUID,
        version BIGINT NOT NULL DEFAULT 0
    )';
    
    -- Tabla de reservas
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.bookings (
        id UUID PRIMARY KEY,
        business_id UUID NOT NULL,
        customer_id UUID NOT NULL,
        professional_id UUID,
        resource_id UUID,
        service_id UUID NOT NULL,
        start_time TIMESTAMP NOT NULL,
        end_time TIMESTAMP NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT ''PENDING'', -- PENDING, CONFIRMED, CANCELED, COMPLETED, NO_SHOW
        google_event_id VARCHAR(255),
        attendance_code VARCHAR(20),
        attendance_validated BOOLEAN NOT NULL DEFAULT FALSE,
        validated_at TIMESTAMP,
        validated_by UUID,
        notes TEXT,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at TIMESTAMP,
        deleted_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES ' || schema_name || '.customers(id),
        CONSTRAINT fk_bookings_professional FOREIGN KEY (professional_id) REFERENCES ' || schema_name || '.professionals(id),
        CONSTRAINT fk_bookings_resource FOREIGN KEY (resource_id) REFERENCES ' || schema_name || '.resources(id),
        CONSTRAINT fk_bookings_service FOREIGN KEY (service_id) REFERENCES ' || schema_name || '.services(id)
    )';
    
    -- Tabla de referidos
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.referrals (
        id UUID PRIMARY KEY,
        referrer_customer_id UUID NOT NULL,
        referred_customer_id UUID NOT NULL,
        business_id UUID NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT ''PENDING'', -- PENDING, CONVERTED, EXPIRED, CANCELED
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        converted_at TIMESTAMP,
        points_awarded INTEGER,
        created_by UUID,
        updated_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT fk_referrals_referrer FOREIGN KEY (referrer_customer_id) REFERENCES ' || schema_name || '.customers(id),
        CONSTRAINT fk_referrals_referred FOREIGN KEY (referred_customer_id) REFERENCES ' || schema_name || '.customers(id)
    )';
    
    -- Tabla de transacciones de puntos de fidelidad
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.loyalty_transactions (
        id UUID PRIMARY KEY,
        customer_id UUID NOT NULL,
        business_id UUID NOT NULL,
        booking_id UUID,
        points_awarded INTEGER NOT NULL,
        transaction_type VARCHAR(50) NOT NULL, -- BOOKING, REFERRAL, REDEMPTION, MANUAL, EXPIRATION
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        expires_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT fk_loyalty_transactions_customer FOREIGN KEY (customer_id) REFERENCES ' || schema_name || '.customers(id),
        CONSTRAINT fk_loyalty_transactions_booking FOREIGN KEY (booking_id) REFERENCES ' || schema_name || '.bookings(id)
    )';
    
    -- Tabla de recompensas disponibles
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.reward_items (
        id UUID PRIMARY KEY,
        business_id UUID NOT NULL,
        name VARCHAR(255) NOT NULL,
        description TEXT,
        points_cost INTEGER NOT NULL,
        type VARCHAR(50) NOT NULL, -- DISCOUNT, SERVICE, PRODUCT, EVENT, MEMBERSHIP
        status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE'', -- ACTIVE, INACTIVE, DRAFT
        image_url TEXT,
        stock INTEGER,
        limit_per_customer INTEGER,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at TIMESTAMP,
        deleted_by UUID,
        version BIGINT NOT NULL DEFAULT 0
    )';
    
    -- Tabla de redención de recompensas
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.reward_redemptions (
        id UUID PRIMARY KEY,
        customer_id UUID NOT NULL,
        business_id UUID NOT NULL,
        reward_item_id UUID NOT NULL,
        points_spent INTEGER NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT ''PENDING'', -- PENDING, COMPLETED, CANCELED
        redeemed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        expires_at TIMESTAMP,
        redemption_code VARCHAR(20),
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT fk_reward_redemptions_customer FOREIGN KEY (customer_id) REFERENCES ' || schema_name || '.customers(id),
        CONSTRAINT fk_reward_redemptions_reward FOREIGN KEY (reward_item_id) REFERENCES ' || schema_name || '.reward_items(id)
    )';
    
    -- Índices para mejorar rendimiento de consultas frecuentes
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_customers_business_id ON ' || schema_name || '.customers(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_customers_email ON ' || schema_name || '.customers(email)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_customers_phone ON ' || schema_name || '.customers(phone)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_customers_referral_code ON ' || schema_name || '.customers(referral_code)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_customer_custom_fields_customer_id ON ' || schema_name || '.customer_custom_fields(customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_customer_custom_fields_field_definition_id ON ' || schema_name || '.customer_custom_fields(field_definition_id)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_professionals_business_id ON ' || schema_name || '.professionals(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_professionals_user_id ON ' || schema_name || '.professionals(user_id)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_resources_business_id ON ' || schema_name || '.resources(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_resources_type ON ' || schema_name || '.resources(type)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_services_business_id ON ' || schema_name || '.services(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_services_category_id ON ' || schema_name || '.services(category_id)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_bookings_business_id ON ' || schema_name || '.bookings(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_bookings_customer_id ON ' || schema_name || '.bookings(customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_bookings_professional_id ON ' || schema_name || '.bookings(professional_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_bookings_resource_id ON ' || schema_name || '.bookings(resource_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_bookings_service_id ON ' || schema_name || '.bookings(service_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_bookings_start_time ON ' || schema_name || '.bookings(start_time)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_bookings_end_time ON ' || schema_name || '.bookings(end_time)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_bookings_status ON ' || schema_name || '.bookings(status)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_bookings_attendance_code ON ' || schema_name || '.bookings(attendance_code)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_referrals_referrer_customer_id ON ' || schema_name || '.referrals(referrer_customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_referrals_referred_customer_id ON ' || schema_name || '.referrals(referred_customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_referrals_business_id ON ' || schema_name || '.referrals(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_referrals_status ON ' || schema_name || '.referrals(status)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_customer_id ON ' || schema_name || '.loyalty_transactions(customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_business_id ON ' || schema_name || '.loyalty_transactions(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_booking_id ON ' || schema_name || '.loyalty_transactions(booking_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_transaction_type ON ' || schema_name || '.loyalty_transactions(transaction_type)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_expires_at ON ' || schema_name || '.loyalty_transactions(expires_at)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_reward_items_business_id ON ' || schema_name || '.reward_items(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_reward_items_status ON ' || schema_name || '.reward_items(status)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_reward_redemptions_customer_id ON ' || schema_name || '.reward_redemptions(customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_reward_redemptions_business_id ON ' || schema_name || '.reward_redemptions(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_reward_redemptions_reward_item_id ON ' || schema_name || '.reward_redemptions(reward_item_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_reward_redemptions_status ON ' || schema_name || '.reward_redemptions(status)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_reward_redemptions_redemption_code ON ' || schema_name || '.reward_redemptions(redemption_code)';
END;
$$ LANGUAGE plpgsql;

-- Crear tablas para el esquema de negocio de ejemplo
SELECT create_booking_module_tables('business_1');

-- Trigger para crear automáticamente tablas del módulo cuando se crea un nuevo esquema
CREATE OR REPLACE FUNCTION trigger_create_booking_module_tables()
RETURNS TRIGGER AS $$
BEGIN
    -- Llamar a la función para crear las tablas en el nuevo esquema
    PERFORM create_booking_module_tables(NEW.schema_name);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Registrar trigger para ejecutarse después de insertar nuevos esquemas
DROP TRIGGER IF EXISTS after_insert_business_schema ON public.business_schemas;
CREATE TRIGGER after_insert_business_schema
AFTER INSERT ON public.business_schemas
FOR EACH ROW
EXECUTE FUNCTION trigger_create_booking_module_tables();