package com.oficinaagua.chatbot.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Aplica el límite de peticiones por cliente antes de ejecutar el controller.
 * <p>
 * Por defecto el cliente es la IP de la conexión. Con {@code trustForwardedFor} se usa el último valor de
 * X-Forwarded-For: el que añadió el BFF (el único proxy de confianza). Los valores anteriores los puede
 * falsificar el cliente, y la propia cabecera solo es fiable si el backend es accesible únicamente a través del BFF.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    static final String FORWARDED_FOR = "X-Forwarded-For";

    private final RateLimiter rateLimiter;
    private final boolean trustForwardedFor;

    public RateLimitInterceptor(RateLimiter rateLimiter, boolean trustForwardedFor) {
        this.rateLimiter = rateLimiter;
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        rateLimiter.check(clientKey(request));
        return true;
    }

    private String clientKey(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = lastEntry(request.getHeader(FORWARDED_FOR));
            if (forwarded != null) {
                return forwarded;
            }
        }
        return request.getRemoteAddr();
    }

    /** Último valor de la cabecera, o null si falta o está vacío (entonces se usa la IP de la conexión). */
    private static String lastEntry(String header) {
        if (header == null) {
            return null;
        }
        String[] entries = header.split(",", -1);
        String last = entries[entries.length - 1].trim();
        return last.isEmpty() ? null : last;
    }
}
