-- Módulo de pagos
-- Este script crea las tablas para cada esquema de negocio

-- Función para crear tablas del módulo de pagos en cada esquema de negocio
CREATE OR REPLACE FUNCTION create_payment_module_tables(schema_name VARCHAR)
RETURNS VOID AS $$
BEGIN
    -- Tabla de pagos
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.payments (
        id UUID PRIMARY KEY,
        booking_id UUID NOT NULL,
        customer_id UUID NOT NULL,
        business_id UUID NOT NULL,
        amount DECIMAL(10,2) NOT NULL,
        currency VARCHAR(3) NOT NULL DEFAULT ''USD'',
        status VARCHAR(20) NOT NULL DEFAULT ''PENDING'', -- PENDING, COMPLETED, FAILED, REFUNDED, CANCELED
        payment_method VARCHAR(50),
        transaction_id VARCHAR(255),
        payment_date TIMESTAMP,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at TIMESTAMP,
        deleted_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES ' || schema_name || '.bookings(id),
        CONSTRAINT fk_payments_customer FOREIGN KEY (customer_id) REFERENCES ' || schema_name || '.customers(id)
    )';
    
    -- Tabla de facturas
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.invoices (
        id UUID PRIMARY KEY,
        payment_id UUID NOT NULL,
        business_id UUID NOT NULL,
        customer_id UUID NOT NULL,
        invoice_number VARCHAR(50) NOT NULL,
        issued_date DATE NOT NULL,
        due_date DATE NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT ''ISSUED'', -- ISSUED, PAID, OVERDUE, CANCELED
        total_amount DECIMAL(10,2) NOT NULL,
        tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at TIMESTAMP,
        deleted_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT fk_invoices_payment FOREIGN KEY (payment_id) REFERENCES ' || schema_name || '.payments(id),
        CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES ' || schema_name || '.customers(id),
        CONSTRAINT invoices_invoice_number_unique UNIQUE (business_id, invoice_number)
    )';
    
    -- Tabla de métodos de pago guardados
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.payment_methods (
        id UUID PRIMARY KEY,
        customer_id UUID NOT NULL,
        business_id UUID NOT NULL,
        type VARCHAR(50) NOT NULL, -- CREDIT_CARD, DEBIT_CARD, PAYPAL, MERCADO_PAGO, etc.
        last_four_digits VARCHAR(4),
        expiry_date VARCHAR(7), -- MM/YYYY
        token_id VARCHAR(255), -- Token de tarjeta almacenado en gateway
        is_default BOOLEAN NOT NULL DEFAULT FALSE,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at TIMESTAMP,
        deleted_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT fk_payment_methods_customer FOREIGN KEY (customer_id) REFERENCES ' || schema_name || '.customers(id)
    )';
    
    -- Tabla de configuración de pago del negocio
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.payment_settings (
        id UUID PRIMARY KEY,
        business_id UUID NOT NULL,
        provider VARCHAR(50) NOT NULL, -- MERCADO_PAGO, STRIPE, PAYPAL, etc.
        merchant_id VARCHAR(255),
        api_key_encrypted TEXT,
        api_secret_encrypted TEXT,
        active BOOLEAN NOT NULL DEFAULT TRUE,
        test_mode BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        version BIGINT NOT NULL DEFAULT 0
    )';
    
    -- Tabla de transacciones para auditoría (incluye eventos de gateway)
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.payment_transactions (
        id UUID PRIMARY KEY,
        payment_id UUID,
        business_id UUID NOT NULL,
        transaction_type VARCHAR(50) NOT NULL, -- AUTHORIZATION, CAPTURE, REFUND, WEBHOOK, ERROR
        amount DECIMAL(10,2),
        provider_response TEXT,
        transaction_id VARCHAR(255),
        status VARCHAR(50) NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by UUID
    )';
    
    -- Índices para mejorar rendimiento de consultas frecuentes
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON ' || schema_name || '.payments(booking_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payments_customer_id ON ' || schema_name || '.payments(customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payments_business_id ON ' || schema_name || '.payments(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payments_status ON ' || schema_name || '.payments(status)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON ' || schema_name || '.payments(transaction_id)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_invoices_payment_id ON ' || schema_name || '.invoices(payment_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_invoices_customer_id ON ' || schema_name || '.invoices(customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_invoices_business_id ON ' || schema_name || '.invoices(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_invoices_status ON ' || schema_name || '.invoices(status)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_invoices_invoice_number ON ' || schema_name || '.invoices(invoice_number)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_methods_customer_id ON ' || schema_name || '.payment_methods(customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_methods_business_id ON ' || schema_name || '.payment_methods(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_methods_type ON ' || schema_name || '.payment_methods(type)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_methods_is_default ON ' || schema_name || '.payment_methods(is_default)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_settings_business_id ON ' || schema_name || '.payment_settings(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_settings_provider ON ' || schema_name || '.payment_settings(provider)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_settings_active ON ' || schema_name || '.payment_settings(active)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_transactions_payment_id ON ' || schema_name || '.payment_transactions(payment_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_transactions_business_id ON ' || schema_name || '.payment_transactions(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_transactions_transaction_type ON ' || schema_name || '.payment_transactions(transaction_type)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_transactions_status ON ' || schema_name || '.payment_transactions(status)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_payment_transactions_transaction_id ON ' || schema_name || '.payment_transactions(transaction_id)';
END;
$ LANGUAGE plpgsql;

-- Crear tablas para el esquema de negocio de ejemplo
SELECT create_payment_module_tables('business_1');

-- Trigger para crear automáticamente tablas del módulo cuando se crea un nuevo esquema
CREATE OR REPLACE FUNCTION trigger_create_payment_module_tables()
RETURNS TRIGGER AS $
BEGIN
    -- Llamar a la función para crear las tablas en el nuevo esquema
    PERFORM create_payment_module_tables(NEW.schema_name);
    RETURN NEW;
END;
$ LANGUAGE plpgsql;

-- Registrar trigger para ejecutarse después de insertar nuevos esquemas
DROP TRIGGER IF EXISTS after_insert_business_schema_payment ON public.business_schemas;
CREATE TRIGGER after_insert_business_schema_payment
AFTER INSERT ON public.business_schemas
FOR EACH ROW
EXECUTE FUNCTION trigger_create_payment_module_tables();