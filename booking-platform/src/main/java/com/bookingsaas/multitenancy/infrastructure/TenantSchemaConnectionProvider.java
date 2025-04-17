package com.bookingsaas.multitenancy.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Proveedor de conexiones multitenant para implementar esquemas separados por tenant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaConnectionProvider implements MultiTenantConnectionProvider {

    private static final long serialVersionUID = 1L;

    private final DataSource dataSource;

    @Value("${app.multitenancy.defaultSchema}")
    private String defaultSchema;

    /**
     * Obtiene una conexión y establece el esquema por defecto.
     */
    @Override
    public Connection getAnyConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setSchema(defaultSchema);
        return connection;
    }

    /**
     * Libera cualquier conexión.
     */
    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.setSchema(defaultSchema);
        connection.close();
    }

    /**
     * Obtiene una conexión y establece el esquema específico del tenant.
     */
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        connection.setSchema(tenantIdentifier);
        log.debug("Conexión establecida para esquema: {}", tenantIdentifier);
        return connection;
    }

    /**
     * Libera la conexión y restablece el esquema por defecto.
     */
    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.setSchema(defaultSchema);
        releaseAnyConnection(connection);
    }

    /**
     * Indica si este proveedor de conexiones puede ser liberado.
     */
    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    /**
     * Determina si es posible envolver este proveedor como el tipo dado.
     */
    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (unwrapType.isInstance(this)) {
            return (T) this;
        }
        if (unwrapType.equals(ConnectionProvider.class)) {
            return (T) this;
        }
        throw new UnknownUnwrapTypeException(unwrapType);
    }
}