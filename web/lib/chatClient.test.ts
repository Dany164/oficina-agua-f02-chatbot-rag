import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { sendChatMessage } from '@/lib/chatClient';

const config = { chatApiUrl: 'http://backend:8080', requestTimeoutMs: 5000 };

function jsonResponse(body: unknown, status = 200, headers: Record<string, string> = {}): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json', ...headers },
    });
}

describe('sendChatMessage', () => {
    const fetchMock = vi.fn<typeof fetch>();

    beforeEach(() => {
        fetchMock.mockReset();
        // Los fallos se registran en el log; se silencian para que la salida de los tests sea limpia.
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('envía la petición esperada y devuelve la respuesta', async () => {
        fetchMock.mockResolvedValue(
            jsonResponse({ answer: 'De 8 a 16', sources: [{ title: 'Horario', documentId: 'doc-1' }] }),
        );

        const result = await sendChatMessage('hola', '203.0.113.5', config, fetchMock);

        expect(result).toEqual({
            kind: 'ok',
            data: { answer: 'De 8 a 16', sources: [{ title: 'Horario', documentId: 'doc-1' }] },
        });
        const [url, init] = fetchMock.mock.calls[0];
        expect(url).toBe('http://backend:8080/api/v1/chat/messages');
        expect(init?.method).toBe('POST');
        expect(init?.body).toBe(JSON.stringify({ message: 'hola' }));
        expect(init?.headers).toMatchObject({
            'Content-Type': 'application/json',
            'X-Forwarded-For': '203.0.113.5',
        });
        expect(init?.signal).toBeInstanceOf(AbortSignal);
    });

    it('devuelve solo los campos del contrato', async () => {
        fetchMock.mockResolvedValue(
            jsonResponse({
                answer: 'ok',
                secret: 'no debe salir',
                sources: [{ title: 'Tarifas', documentId: null, internal: 'x' }],
            }),
        );

        const result = await sendChatMessage('hola', '1.1.1.1', config, fetchMock);

        expect(result).toEqual({
            kind: 'ok',
            data: { answer: 'ok', sources: [{ title: 'Tarifas' }] },
        });
    });

    it('acepta una respuesta sin fuentes', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ answer: 'No tengo información', sources: [] }));

        const result = await sendChatMessage('hola', '1.1.1.1', config, fetchMock);

        expect(result).toEqual({ kind: 'ok', data: { answer: 'No tengo información', sources: [] } });
    });

    it('devuelve rate_limited con el Retry-After del backend', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ status: 429 }, 429, { 'Retry-After': '42' }));

        const result = await sendChatMessage('hola', '1.1.1.1', config, fetchMock);

        expect(result).toEqual({ kind: 'rate_limited', retryAfterSeconds: 42 });
    });

    it('usa 60 segundos si el 429 no trae un Retry-After válido', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ status: 429 }, 429));

        const result = await sendChatMessage('hola', '1.1.1.1', config, fetchMock);

        expect(result).toEqual({ kind: 'rate_limited', retryAfterSeconds: 60 });
    });

    it.each([400, 500, 503])('devuelve unavailable si el backend responde %i', async (status) => {
        fetchMock.mockResolvedValue(jsonResponse({ message: 'detalle interno' }, status));

        const result = await sendChatMessage('hola', '1.1.1.1', config, fetchMock);

        expect(result).toEqual({ kind: 'unavailable' });
    });

    it.each([
        ['conexión rechazada', new Error('ECONNREFUSED')],
        ['tiempo agotado', new DOMException('The operation timed out.', 'TimeoutError')],
    ])('devuelve unavailable si fetch falla: %s', async (_label, error) => {
        fetchMock.mockRejectedValue(error);

        const result = await sendChatMessage('hola', '1.1.1.1', config, fetchMock);

        expect(result).toEqual({ kind: 'unavailable' });
    });

    it('devuelve unavailable si el cuerpo no es JSON', async () => {
        fetchMock.mockResolvedValue(new Response('<html>error</html>', { status: 200 }));

        const result = await sendChatMessage('hola', '1.1.1.1', config, fetchMock);

        expect(result).toEqual({ kind: 'unavailable' });
    });

    it.each([
        {},
        { answer: 'x' },
        { answer: 1, sources: [] },
        { answer: 'x', sources: 'no es una lista' },
        { answer: 'x', sources: [{}] },
    ])('devuelve unavailable si la respuesta no cumple el contrato (%j)', async (body) => {
        fetchMock.mockResolvedValue(jsonResponse(body));

        const result = await sendChatMessage('hola', '1.1.1.1', config, fetchMock);

        expect(result).toEqual({ kind: 'unavailable' });
    });
});