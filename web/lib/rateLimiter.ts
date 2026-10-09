export type RateLimitResult =
    | { allowed: true }
    | { allowed: false; retryAfterSeconds: number };

interface Window {
    start: number;
    count: number;
}

/** Si el mapa supera este tamaño se purgan las ventanas expiradas. */
const PURGE_THRESHOLD = 10_000;

/**
 * Limitador de ventana fija en memoria: como máximo maxRequests peticiones por clave
 * de cliente dentro de cada ventana de windowMs milisegundos.
 * Solo vale para una instancia del BFF (el estado no se comparte).
 */
export class RateLimiter {
    private readonly maxRequests: number;
    private readonly windowMs: number;
    private readonly now: () => number;
    private readonly windows = new Map<string, Window>();

    constructor(maxRequests: number, windowMs: number, now: () => number = Date.now) {
        if (!Number.isInteger(maxRequests) || maxRequests < 1) {
            throw new Error('maxRequests debe ser un entero >= 1');
        }
        if (!(windowMs > 0)) {
            throw new Error('windowMs debe ser positivo');
        }
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        this.now = now;
    }

    /** Registra una petición del cliente y dice si puede continuar. */
    check(clientKey: string): RateLimitResult {
        const now = this.now();
        if (this.windows.size > PURGE_THRESHOLD) {
            this.purgeExpired(now);
        }

        const current = this.windows.get(clientKey);
        if (!current || this.isExpired(current, now)) {
            this.windows.set(clientKey, { start: now, count: 1 });
            return { allowed: true };
        }

        if (current.count >= this.maxRequests) {
            const remainingMs = current.start + this.windowMs - now;
            return { allowed: false, retryAfterSeconds: Math.max(1, Math.ceil(remainingMs / 1000)) };
        }

        current.count += 1;
        return { allowed: true };
    }

    private isExpired(window: Window, now: number): boolean {
        return now >= window.start + this.windowMs;
    }

    private purgeExpired(now: number): void {
        for (const [key, window] of this.windows) {
            if (this.isExpired(window, now)) {
                this.windows.delete(key);
            }
        }
    }
}