import { describe, expect, it } from 'vitest';
import { getClientIp } from '@/lib/clientIp';

describe('getClientIp', () => {
    it('ignora las cabeceras si no se confía en el proxy', () => {
        const headers = new Headers({ 'x-forwarded-for': '203.0.113.9', 'x-real-ip': '203.0.113.9' });

        expect(getClientIp(headers, false)).toBe('unknown');
    });

    it('usa la IP de X-Forwarded-For cuando hay una sola', () => {
        const headers = new Headers({ 'x-forwarded-for': '203.0.113.9' });

        expect(getClientIp(headers, true)).toBe('203.0.113.9');
    });

    it('usa la última IP cuando hay varias, porque las anteriores las puede falsificar el cliente', () => {
        const headers = new Headers({ 'x-forwarded-for': '1.1.1.1, 2.2.2.2, 203.0.113.9' });

        expect(getClientIp(headers, true)).toBe('203.0.113.9');
    });

    it('quita los espacios alrededor de cada entrada', () => {
        const headers = new Headers({ 'x-forwarded-for': '10.0.0.1 ,   203.0.113.9  ' });

        expect(getClientIp(headers, true)).toBe('203.0.113.9');
    });

    it('usa X-Real-IP si no hay X-Forwarded-For', () => {
        const headers = new Headers({ 'x-real-ip': '198.51.100.7' });

        expect(getClientIp(headers, true)).toBe('198.51.100.7');
    });

    it('devuelve unknown si confía en el proxy pero no hay cabeceras', () => {
        expect(getClientIp(new Headers(), true)).toBe('unknown');
    });
});