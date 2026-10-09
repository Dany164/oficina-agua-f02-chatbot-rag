package com.oficinaagua.chatbot.ratelimit;

import com.oficinaagua.chatbot.exception.RateLimitExceededException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de ventana fija en memoria: como máximo {@code maxRequests} peticiones
 * por clave de cliente dentro de cada ventana de {@code window}.
 * Solo válido para una instancia del backend (el estado no se comparte).
 */
public class RateLimiter {

    /** Si el mapa supera este tamaño se purgan las ventanas expiradas. */
    private static final int PURGE_THRESHOLD = 10_000;

    private record Window(Instant start, int count) {
    }

    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, Duration window, Clock clock) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("maxRequests debe ser >= 1");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window debe ser positiva");
        }
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    /** Registra una petición del cliente; lanza RateLimitExceededException si supera el límite. */
    public void check(String clientKey) {
        Instant now = clock.instant();
        if (windows.size() > PURGE_THRESHOLD) {
            windows.values().removeIf(w -> isExpired(w, now));
        }

        Window current = windows.compute(clientKey, (key, previous) -> {
            if (previous == null || isExpired(previous, now)) {
                return new Window(now, 1);
            }
            // Se limita a maxRequests + 1 para que el contador nunca desborde.
            return new Window(previous.start(), Math.min(previous.count() + 1, maxRequests + 1));
        });

        if (current.count() > maxRequests) {
            Duration remaining = Duration.between(now, current.start().plus(window));
            long retryAfterSeconds = Math.max(1, (remaining.toMillis() + 999) / 1000);
            throw new RateLimitExceededException(retryAfterSeconds);
        }
    }

    private boolean isExpired(Window w, Instant now) {
        return !now.isBefore(w.start().plus(window));
    }
}