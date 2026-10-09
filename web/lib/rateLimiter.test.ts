import { beforeEach, describe, expect, it } from 'vitest';
import { RateLimiter } from '@/lib/rateLimiter';

describe('RateLimiter', () => {
    let time: number;
    let limiter: RateLimiter;

    beforeEach(() => {
        time = 0;
        limiter = new RateLimiter(3, 60_000, () => time);
    });

    it('permite peticiones hasta el límite', () => {
        expect(limiter.check('1.1.1.1')).toEqual({ allowed: true });
        expect(limiter.check('1.1.1.1')).toEqual({ allowed: true });
        expect(limiter.check('1.1.1.1')).toEqual({ allowed: true });
    });

    it('rechaza la petición que supera el límite e indica cuánto esperar', () => {
        for (let i = 0; i < 3; i++) {
            limiter.check('1.1.1.1');
        }

        expect(limiter.check('1.1.1.1')).toEqual({ allowed: false, retryAfterSeconds: 60 });
    });

    it('reduce el tiempo de espera a medida que avanza la ventana', () => {
        for (let i = 0; i < 3; i++) {
            limiter.check('1.1.1.1');
        }
        time = 20_000;

        expect(limiter.check('1.1.1.1')).toEqual({ allowed: false, retryAfterSeconds: 40 });
    });

    it('limita a cada cliente por separado', () => {
        for (let i = 0; i < 3; i++) {
            limiter.check('1.1.1.1');
        }

        expect(limiter.check('2.2.2.2')).toEqual({ allowed: true });
    });

    it('vuelve a permitir peticiones cuando expira la ventana', () => {
        for (let i = 0; i < 3; i++) {
            limiter.check('1.1.1.1');
        }
        time = 60_000;

        expect(limiter.check('1.1.1.1')).toEqual({ allowed: true });
    });

    it.each([0, -1, 1.5])('rechaza un máximo de peticiones inválido (%j)', (max) => {
        expect(() => new RateLimiter(max, 60_000)).toThrow('maxRequests');
    });
});