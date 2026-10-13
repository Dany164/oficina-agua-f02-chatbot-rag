import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DEFAULT_RETRY_AFTER_SECONDS, postChatMessage } from '@/lib/chatApi';
import { FALLBACK_ERROR_MESSAGES } from '@/lib/chatCopy';

function jsonResponse(body: unknown, status = 200, headers: Record<string, string> = {}): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json', ...headers },
    });
}

/** Cuerpo de error con la misma forma que devuelve el BFF. */
function errorBody(status: number, message: string) {
    return { timestamp: '2026-01-01T00:00:00Z', status, error: 'Error', message };
}

describe('postChatMessage', () => {
    const fetchMock = vi.fn<typeof fetch>();

    beforeEach(() => {
        fetchMock.mockReset();
    });

    it('envía POST /api/chat con el mensaje en JSON', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ answer: 'ok', sources: [] }));

        await postChatMessage('¿Cuál es el horario?', fetchMock);

        expect(fetchMock).toHaveBeenCalledTimes(1);
        const [url, init] = fetchMock.mock.calls[0];
        expect(url).toBe('/api/chat');
        expect(init?.method).toBe('POST');
        expect(new Headers(init?.headers).get('Content-Type')).toBe('application/json');
        expect(JSON.parse(String(init?.body))).toEqual({ message: '¿Cuál es el horario?' });
    });

    it('devuelve la respuesta y las fuentes cuando el servidor responde 200', async () => {
        fetchMock.mockResolvedValue(
            jsonResponse({ answer: 'De 8 a 16', sources: [{ title: 'Horario', documentId: 'doc-1' }] }),
        );

        const result = await postChatMessage('horario', fetchMock);

        expect(result).toEqual({
            ok: true,
            answer: 'De 8 a 16',
            sources: [{ title: 'Horario', documentId: 'doc-1' }],
        });
    });

    it('acepta fuentes sin documentId', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ answer: 'ok', sources: [{ title: 'Tarifas' }] }));

        const result = await postChatMessage('tarifas', fetchMock);

        expect(result).toEqual({ ok: true, answer: 'ok', sources: [{ title: 'Tarifas' }] });
    });

    it.each([
        ['sin answer', { sources: [] }],
        ['answer que no es texto', { answer: 42, sources: [] }],
        ['sources que no es una lista', { answer: 'x', sources: 'nope' }],
        ['fuente sin título', { answer: 'x', sources: [{ documentId: 'd' }] }],
    ])('trata un 200 con forma inesperada como error interno (%s)', async (_caso, body) => {
        fetchMock.mockResolvedValue(jsonResponse(body));

        const result = await postChatMessage('hola', fetchMock);

        expect(result).toEqual({
            ok: false,
            error: { kind: 'internal', message: FALLBACK_ERROR_MESSAGES.internal },
        });
    });

    it('trata un 200 que no es JSON como error interno', async () => {
        fetchMock.mockResolvedValue(new Response('<html>oops</html>', { status: 200 }));

        const result = await postChatMessage('hola', fetchMock);

        expect(result).toEqual({
            ok: false,
            error: { kind: 'internal', message: FALLBACK_ERROR_MESSAGES.internal },
        });
    });

    it('400: usa el mensaje del servidor', async () => {
        fetchMock.mockResolvedValue(jsonResponse(errorBody(400, 'El mensaje no puede estar vacío.'), 400));

        const result = await postChatMessage(' ', fetchMock);

        expect(result).toEqual({
            ok: false,
            error: { kind: 'invalid', message: 'El mensaje no puede estar vacío.' },
        });
    });

    it('400 sin cuerpo JSON: usa el mensaje de respaldo', async () => {
        fetchMock.mockResolvedValue(new Response('', { status: 400 }));

        const result = await postChatMessage('hola', fetchMock);

        expect(result).toEqual({
            ok: false,
            error: { kind: 'invalid', message: FALLBACK_ERROR_MESSAGES.invalid },
        });
    });

    it('429: devuelve rate_limited con los segundos de Retry-After', async () => {
        fetchMock.mockResolvedValue(
            jsonResponse(errorBody(429, 'Demasiadas solicitudes. Intenta de nuevo en unos segundos.'), 429, {
                'Retry-After': '12',
            }),
        );

        const result = await postChatMessage('hola', fetchMock);

        expect(result).toEqual({
            ok: false,
            error: {
                kind: 'rate_limited',
                message: 'Demasiadas solicitudes. Intenta de nuevo en unos segundos.',
                retryAfterSeconds: 12,
            },
        });
    });

    it('429 sin Retry-After: usa el valor por defecto', async () => {
        fetchMock.mockResolvedValue(jsonResponse(errorBody(429, 'Demasiadas solicitudes.'), 429));

        const result = await postChatMessage('hola', fetchMock);

        expect(result).toMatchObject({
            ok: false,
            error: { kind: 'rate_limited', retryAfterSeconds: DEFAULT_RETRY_AFTER_SECONDS },
        });
    });

    it.each(['abc', '0', '-3', '1.5', ''])(
        'usa el valor por defecto cuando Retry-After no es un entero positivo (%j)',
        async (value) => {
            fetchMock.mockResolvedValue(
                jsonResponse(errorBody(429, 'Demasiadas solicitudes.'), 429, { 'Retry-After': value }),
            );

            const result = await postChatMessage('hola', fetchMock);

            expect(result).toMatchObject({
                ok: false,
                error: { kind: 'rate_limited', retryAfterSeconds: DEFAULT_RETRY_AFTER_SECONDS },
            });
        },
    );

    it('503: devuelve unavailable', async () => {
        fetchMock.mockResolvedValue(
            jsonResponse(errorBody(503, 'El servicio no está disponible temporalmente. Intenta de nuevo más tarde.'), 503),
        );

        const result = await postChatMessage('hola', fetchMock);

        expect(result).toEqual({
            ok: false,
            error: {
                kind: 'unavailable',
                message: 'El servicio no está disponible temporalmente. Intenta de nuevo más tarde.',
            },
        });
    });

    it('500: devuelve internal', async () => {
        fetchMock.mockResolvedValue(jsonResponse(errorBody(500, 'Ocurrió un error interno.'), 500));

        const result = await postChatMessage('hola', fetchMock);

        expect(result).toEqual({
            ok: false,
            error: { kind: 'internal', message: 'Ocurrió un error interno.' },
        });
    });

    it('otros códigos sin JSON (502 de un proxy): internal con mensaje de respaldo', async () => {
        fetchMock.mockResolvedValue(new Response('<html>Bad Gateway</html>', { status: 502 }));

        const result = await postChatMessage('hola', fetchMock);

        expect(result).toEqual({
            ok: false,
            error: { kind: 'internal', message: FALLBACK_ERROR_MESSAGES.internal },
        });
    });

    it('fallo de red: devuelve network sin filtrar el mensaje técnico', async () => {
        fetchMock.mockRejectedValue(new TypeError('fetch failed ECONNREFUSED 127.0.0.1:3000'));

        const result = await postChatMessage('hola', fetchMock);

        expect(result).toEqual({
            ok: false,
            error: { kind: 'network', message: FALLBACK_ERROR_MESSAGES.network },
        });
        expect(JSON.stringify(result)).not.toContain('ECONNREFUSED');
    });
});