package com.oficinaagua.chatbot.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Aplica el límite de peticiones por IP antes de ejecutar el controller. */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    public RateLimitInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // TODO (rama del BFF): detrás del BFF la IP real llegará en X-Forwarded-For,
        // que solo debe confiarse si está habilitado por configuración.
        rateLimiter.check(request.getRemoteAddr());
        return true;
    }
}
