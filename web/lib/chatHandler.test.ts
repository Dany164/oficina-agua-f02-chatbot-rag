import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createChatHandler, type ChatHandlerDeps } from '@/lib/chatHandler';
import { RateLimiter, type RateLimitResult } from '@/lib/rateLimiter';

const config: ChatHandlerDeps['config'] = { maxMessageLength: 500, trustProxyHeaders: true };

function chatRequest(body: unknown, ip = '203.0.113.9'): Request {
    return new Request('http://localhost/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': ip },
        body: typeof body === 'string' ? body : JSON.stringify(body),
    });
}

describe('createChatHandler', () => {
    const sendMessage = vi.fn<ChatHandlerDeps['sendMessage']>();
    const check = vi.fn<(clientKey: string) => RateLimitResult>();

    function handler() {
        return createChatHandler({ config, rateLimiter: { check }, sendMessage });
    }

    beforeEach(() => {
        sendMessage.mockReset();
        check.mockReset();
        check.mockReturnValue({ allowed: true });
        // Los errores inesperados se registran en el log; se silencian para una salida limpia.
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('devuelve la respuesta del backend con 200', async () => {
        sendMessage.mockResolvedValue({
            kind: 'ok',
            data: { answer: 'De 8 a 16', sources: [{ title: 'Horario' }] },
        });

        const response = await handler()(chatRequest({ message: 'hola' }));

        expect(response.status).toBe(200);
        expect(await response.json()).toEqual({ answer: 'De 8 a 16', sources: [{ title: 'Horario' }] });
        expect(check).toHaveBeenCalledWith('203.0.113.9');
        expect(sendMessage).toHaveBeenCalledWith('hola', '203.0.113.9');
    });

    it('responde 400 con un mensaje en blanco y no llama al backend', async () => {
        const response = await handler()(chatRequest({ message: '   ' }));

        expect(response.status).toBe(400);
        const body = await response.json();
        expect(body).toMatchObject({
            status: 400,
            error: 'Bad Request',
            message: 'El mensaje no puede estar vacío.',
        });
        expect(typeof body.timestamp).toBe('string');
        expect(sendMessage).not.toHaveBeenCalled();
    });

    it('responde 400 si el cuerpo no es JSON', async () => {
        const response = await handler()(chatRequest('{esto no es json'));

        expect(response.status).toBe(400);
        expect(await response.json()).toMatchObject({ message: 'La solicitud no es válida.' });
        expect(sendMessage).not.toHaveBeenCalled();
    });

    it('responde 429 con Retry-After cuando el límite propio se supera', async () => {
        check.mockReturnValue({ allowed: false, retryAfterSeconds: 30 });

        const response = await handler()(chatRequest({ message: 'hola' }));

        expect(response.status).toBe(429);
        expect(response.headers.get('Retry-After')).toBe('30');
        expect(await response.json()).toMatchObject({
            status: 429,
            message: 'Demasiadas solicitudes. Intenta de nuevo en unos segundos.',
        });
        expect(sendMessage).not.toHaveBeenCalled();
    });

    it('aplica el límite antes de validar el cuerpo', async () => {
        check.mockReturnValue({ allowed: false, retryAfterSeconds: 10 });

        const response = await handler()(chatRequest('{esto no es json'));

        expect(response.status).toBe(429);
    });

    it('reenvía el 429 del backend con su Retry-After', async () => {
        sendMessage.mockResolvedValue({ kind: 'rate_limited', retryAfterSeconds: 42 });

        const response = await handler()(chatRequest({ message: 'hola' }));

        expect(response.status).toBe(429);
        expect(response.headers.get('Retry-After')).toBe('42');
    });

    it('responde 503 genérico si el backend no está disponible', async () => {
        sendMessage.mockResolvedValue({ kind: 'unavailable' });

        const response = await handler()(chatRequest({ message: 'hola' }));

        expect(response.status).toBe(503);
        expect(await response.json()).toMatchObject({
            status: 503,
            message: 'El servicio no está disponible temporalmente. Intenta de nuevo más tarde.',
        });
    });

    it('responde 500 genérico ante un error inesperado, sin filtrar detalles', async () => {
        sendMessage.mockRejectedValue(new Error('detalle secreto de la base'));

        const response = await handler()(chatRequest({ message: 'hola' }));

        expect(response.status).toBe(500);
        const body = await response.json();
        expect(body.message).toBe('Ocurrió un error interno.');
        expect(JSON.stringify(body)).not.toContain('secreto');
    });

    it('limita por IP usando el limitador real', async () => {
        sendMessage.mockResolvedValue({ kind: 'ok', data: { answer: 'ok', sources: [] } });
        const realHandler = createChatHandler({
            config,
            rateLimiter: new RateLimiter(2, 60_000),
            sendMessage,
        });

        expect((await realHandler(chatRequest({ message: 'hola' }, '1.1.1.1'))).status).toBe(200);
        expect((await realHandler(chatRequest({ message: 'hola' }, '1.1.1.1'))).status).toBe(200);
        expect((await realHandler(chatRequest({ message: 'hola' }, '1.1.1.1'))).status).toBe(429);
        expect((await realHandler(chatRequest({ message: 'hola' }, '2.2.2.2'))).status).toBe(200);
    });
});