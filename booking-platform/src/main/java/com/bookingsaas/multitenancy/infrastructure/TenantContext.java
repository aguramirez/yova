package com.bookingsaas.multitenancy.infrastructure;

import org.springframework.stereotype.Component;

/**
 * Clase utilitaria para mantener el contexto del tenant actual
 * utilizando ThreadLocal para aislamiento entre hilos.
 */
@Component
public class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Establece el ID del tenant para el hilo de ejecución actual.
     * @param tenantId ID del tenant a establecer
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Obtiene el ID del tenant del hilo de ejecución actual.
     * @return ID del tenant actual
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Limpia el ID del tenant del hilo de ejecución actual.
     * Importante llamar a este método al final de cada solicitud.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}