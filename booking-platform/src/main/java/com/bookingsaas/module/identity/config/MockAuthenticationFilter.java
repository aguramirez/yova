package com.bookingsaas.module.identity.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro para autenticación simulada durante desarrollo.
 * Este filtro solo debe usarse en entornos de desarrollo.
 */
@Slf4j
@Component
@Profile("dev")
public class MockAuthenticationFilter extends OncePerRequestFilter {

    private final boolean allowMockAuth;

    public MockAuthenticationFilter(boolean allowMockAuth) {
        this.allowMockAuth = allowMockAuth;
    }

    /**
     * Procesa la solicitud para aplicar autenticación simulada si se proporciona la cabecera especial.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Solo aplicar si está habilitada la autenticación simulada
        if (!allowMockAuth) {
            filterChain.doFilter(request, response);
            return;
        }

        // Verificar si se incluye la cabecera de autenticación simulada
        String mockUserHeader = request.getHeader("X-Mock-User");
        String mockRoleHeader = request.getHeader("X-Mock-Role");

        if (mockUserHeader != null && !mockUserHeader.isBlank()) {
            // Determinar los roles a aplicar
            List<SimpleGrantedAuthority> authorities;
            if (mockRoleHeader != null && !mockRoleHeader.isBlank()) {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_" + mockRoleHeader.toUpperCase()));
            } else {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }

            // Crear una autenticación simulada
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    mockUserHeader, null, authorities);
            
            // Establecer la autenticación en el contexto de seguridad
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.warn("⚠️ Usando autenticación simulada para usuario: {} con roles: {}",
                    mockUserHeader, authorities);
        }

        filterChain.doFilter(request, response);
    }
}