package com.bookingsaas.multitenancy.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor para extraer y establecer el ID del tenant de las cabeceras HTTP
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    @Value("${app.multitenancy.defaultSchema}")
    private String defaultSchema;

    @Value("${app.multitenancy.tenantHeaderName}")
    private String tenantHeaderName;

    /**
     * Extrae el ID del tenant de la cabecera HTTP y lo establece en el contexto.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(tenantHeaderName);
        
        // Usar el esquema por defecto si no se proporciona un tenant ID
        if (tenantId == null || tenantId.isBlank()) {
            log.debug("No se proporcionó ID de tenant, usando esquema por defecto: {}", defaultSchema);
            tenantId = defaultSchema;
        }
        
        TenantContext.setTenantId(tenantId);
        log.debug("Tenant ID establecido a: {}", tenantId);
        return true;
    }

    /**
     * Limpia el contexto del tenant después de completar la solicitud.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
        log.debug("Contexto de tenant limpiado");
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // No action required here
    }
}