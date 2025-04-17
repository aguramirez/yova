-- Módulo de notificaciones
-- Este script crea las tablas para cada esquema de negocio

-- Función para crear tablas del módulo de notificaciones en cada esquema de negocio
CREATE OR REPLACE FUNCTION create_notification_module_tables(schema_name VARCHAR)
RETURNS VOID AS $$
BEGIN
    -- Tabla de plantillas de notificación
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.notification_templates (
        id UUID PRIMARY KEY,
        business_id UUID NOT NULL,
        type VARCHAR(50) NOT NULL, -- BOOKING_CONFIRMATION, REMINDER, CANCELATION, LOYALTY_POINTS, REFERRAL, etc.
        name VARCHAR(255) NOT NULL,
        subject VARCHAR(255),
        content_template TEXT NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at TIMESTAMP,
        deleted_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT notification_templates_unique UNIQUE (business_id, type)
    )';
    
    -- Tabla de preferencias de notificación para clientes
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.notification_preferences (
        id UUID PRIMARY KEY,
        customer_id UUID NOT NULL,
        channel_type VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH, IN_APP
        is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT notification_preferences_unique UNIQUE (customer_id, channel_type),
        CONSTRAINT fk_notification_preferences_customer FOREIGN KEY (customer_id) REFERENCES ' || schema_name || '.customers(id)
    )';
    
    -- Tabla de notificaciones enviadas
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.notifications (
        id UUID PRIMARY KEY,
        business_id UUID NOT NULL,
        customer_id UUID,
        professional_id UUID,
        template_id UUID,
        notification_type VARCHAR(50) NOT NULL,
        channel VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH, IN_APP
        subject VARCHAR(255),
        content TEXT,
        status VARCHAR(20) NOT NULL DEFAULT ''PENDING'', -- PENDING, SENT, FAILED, DELIVERED, READ
        sent_at TIMESTAMP,
        delivered_at TIMESTAMP,
        read_at TIMESTAMP,
        external_id VARCHAR(255), -- ID del proveedor externo (OneSignal, etc.)
        error_message TEXT,
        related_entity_type VARCHAR(50), -- BOOKING, PAYMENT, LOYALTY, etc.
        related_entity_id UUID,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by UUID
    )';
    
    -- Tabla de configuración de notificaciones por negocio
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || schema_name || '.notification_settings (
        id UUID PRIMARY KEY,
        business_id UUID NOT NULL,
        provider VARCHAR(50), -- ONESIGNAL, TWILIO, SENDGRID, etc.
        channel VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH
        api_key_encrypted TEXT,
        api_secret_encrypted TEXT,
        active BOOLEAN NOT NULL DEFAULT TRUE,
        settings JSONB,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP,
        created_by UUID,
        updated_by UUID,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT notification_settings_unique UNIQUE (business_id, channel, provider)
    )';
    
    -- Índices para mejorar rendimiento de consultas frecuentes
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notification_templates_business_id ON ' || schema_name || '.notification_templates(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notification_templates_type ON ' || schema_name || '.notification_templates(type)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notification_preferences_customer_id ON ' || schema_name || '.notification_preferences(customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notification_preferences_channel_type ON ' || schema_name || '.notification_preferences(channel_type)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notification_preferences_is_enabled ON ' || schema_name || '.notification_preferences(is_enabled)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notifications_business_id ON ' || schema_name || '.notifications(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notifications_customer_id ON ' || schema_name || '.notifications(customer_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notifications_professional_id ON ' || schema_name || '.notifications(professional_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notifications_template_id ON ' || schema_name || '.notifications(template_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notifications_notification_type ON ' || schema_name || '.notifications(notification_type)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notifications_channel ON ' || schema_name || '.notifications(channel)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notifications_status ON ' || schema_name || '.notifications(status)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notifications_sent_at ON ' || schema_name || '.notifications(sent_at)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notifications_related_entity ON ' || schema_name || '.notifications(related_entity_type, related_entity_id)';
    
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notification_settings_business_id ON ' || schema_name || '.notification_settings(business_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notification_settings_channel ON ' || schema_name || '.notification_settings(channel)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notification_settings_provider ON ' || schema_name || '.notification_settings(provider)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_notification_settings_active ON ' || schema_name || '.notification_settings(active)';
    
    -- Insertar plantillas predeterminadas
    EXECUTE 'INSERT INTO ' || schema_name || '.notification_templates 
        (id, business_id, type, name, subject, content_template, created_at)
    VALUES 
        (gen_random_uuid(), ''98f0f1d7-41a7-4b1a-8348-99d9a566682c'', ''BOOKING_CONFIRMATION'', ''Confirmación de Reserva'', 
         ''Confirmación de su reserva en {{business.name}}'', 
         ''Estimado/a {{customer.firstName}},\n\nSu reserva ha sido confirmada para el {{booking.date}} a las {{booking.time}} con {{professional.name}}.\n\nServicio: {{service.name}}\nDuración: {{service.duration}} minutos\n\nCódigo de asistencia: {{booking.attendanceCode}}\n\nSi necesita cancelar o reprogramar, por favor contacte con anticipación.\n\nSaludos,\n{{business.name}}'',
         CURRENT_TIMESTAMP),
        
        (gen_random_uuid(), ''98f0f1d7-41a7-4b1a-8348-99d9a566682c'', ''REMINDER'', ''Recordatorio de Cita'', 
         ''Recordatorio: Su cita mañana en {{business.name}}'', 
         ''Hola {{customer.firstName}},\n\nLe recordamos que tiene una cita programada para mañana {{booking.date}} a las {{booking.time}} con {{professional.name}}.\n\nServicio: {{service.name}}\nDuración: {{service.duration}} minutos\n\nNo olvide traer su código de asistencia: {{booking.attendanceCode}}\n\nEsperamos verle pronto,\n{{business.name}}'',
         CURRENT_TIMESTAMP),
         
        (gen_random_uuid(), ''98f0f1d7-41a7-4b1a-8348-99d9a566682c'', ''CANCELLATION'', ''Cancelación de Reserva'', 
         ''Su reserva en {{business.name}} ha sido cancelada'', 
         ''Estimado/a {{customer.firstName}},\n\nSu reserva para el {{booking.date}} a las {{booking.time}} ha sido cancelada.\n\nSi desea programar una nueva cita, puede hacerlo a través de nuestra plataforma o contactándonos directamente.\n\nSaludos,\n{{business.name}}'',
         CURRENT_TIMESTAMP),
         
        (gen_random_uuid(), ''98f0f1d7-41a7-4b1a-8348-99d9a566682c'', ''LOYALTY_POINTS'', ''Puntos de Fidelidad Ganados'', 
         ''¡Ha ganado puntos de fidelidad en {{business.name}}!'', 
         ''¡Felicidades {{customer.firstName}}!\n\nHa ganado {{loyalty.pointsAwarded}} puntos por su reciente visita a {{business.name}}.\n\nSu balance actual es: {{customer.loyaltyPoints}} puntos.\n\nPuede canjear sus puntos por descuentos y servicios en su próxima visita.\n\nGracias por su preferencia,\n{{business.name}}'',
         CURRENT_TIMESTAMP),
         
        (gen_random_uuid(), ''98f0f1d7-41a7-4b1a-8348-99d9a566682c'', ''REFERRAL_CONVERTED'', ''Referido Exitoso'', 
         ''¡Su referido ha realizado su primera visita a {{business.name}}!'', 
         ''¡Gracias {{customer.firstName}}!\n\nSu referido {{referral.referredName}} ha asistido a su primera cita con nosotros.\n\nComo agradecimiento, hemos añadido {{referral.pointsAwarded}} puntos a su cuenta de fidelidad.\n\nSu balance actual es: {{customer.loyaltyPoints}} puntos.\n\nApreciamos mucho su recomendación,\n{{business.name}}'',
         CURRENT_TIMESTAMP)
    ON CONFLICT DO NOTHING';
END;
$$ LANGUAGE plpgsql;

-- Crear tablas para el esquema de negocio de ejemplo
SELECT create_notification_module_tables('business_1');

-- Trigger para crear automáticamente tablas del módulo cuando se crea un nuevo esquema
CREATE OR REPLACE FUNCTION trigger_create_notification_module_tables()
RETURNS TRIGGER AS $$
BEGIN
    -- Llamar a la función para crear las tablas en el nuevo esquema
    PERFORM create_notification_module_tables(NEW.schema_name);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Registrar trigger para ejecutarse después de insertar nuevos esquemas
DROP TRIGGER IF EXISTS after_insert_business_schema_notification ON public.business_schemas;
CREATE TRIGGER after_insert_business_schema_notification
AFTER INSERT ON public.business_schemas
FOR EACH ROW
EXECUTE FUNCTION trigger_create_notification_module_tables();