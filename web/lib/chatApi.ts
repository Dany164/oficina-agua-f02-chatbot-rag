import { FALLBACK_ERROR_MESSAGES } from '@/lib/chatCopy';
import type { ChatError, ChatErrorKind, ChatSource } from '@/lib/chatTypes';

export type ChatApiResult =
    | { ok: true; answer: string; sources: ChatSource[] }
    | { ok: false; error: ChatError };

/** Segundos de espera cuando un 429 llega sin un Retry-After válido. */
export const DEFAULT_RETRY_AFTER_SECONDS = 10;

const CHAT_ENDPOINT = '/api/chat';

function failure(kind: ChatErrorKind, message?: string, retryAfterSeconds?: number): ChatApiResult {
    const error: ChatError = { kind, message: message ?? FALLBACK_ERROR_MESSAGES[kind] };
    if (retryAfterSeconds !== undefined) {
        error.retryAfterSeconds = retryAfterSeconds;
    }
    return { ok: false, error };
}

/** Lee el cuerpo como JSON; si no lo es (HTML de un proxy, cuerpo vacío...) devuelve null. */
async function readJson(response: Response): Promise<unknown> {
    try {
        return await response.json();
    } catch {
        return null;
    }
}

/** Solo se acepta un entero positivo de segundos; cualquier otra cosa usa el valor por defecto. */
function parseRetryAfter(value: string | null): number {
    if (value !== null && /^[0-9]+$/.test(value.trim())) {
        const seconds = Number(value);
        if (seconds > 0) {
            return seconds;
        }
    }
    return DEFAULT_RETRY_AFTER_SECONDS;
}

/** Devuelve el "message" del cuerpo de error del BFF, o undefined si no hay uno utilizable. */
function serverMessage(body: unknown): string | undefined {
    if (typeof body !== 'object' || body === null) {
        return undefined;
    }
    const message = (body as Record<string, unknown>).message;
    return typeof message === 'string' && message.trim() !== '' ? message : undefined;
}

function kindForStatus(status: number): ChatErrorKind {
    switch (status) {
        case 400:
            return 'invalid';
        case 429:
            return 'rate_limited';
        case 503:
            return 'unavailable';
        default:
            return 'internal';
    }
}

/** Valida la lista de fuentes del contrato. Devuelve null si algo no cumple. */
function parseSources(value: unknown): ChatSource[] | null {
    if (!Array.isArray(value)) {
        return null;
    }
    const sources: ChatSource[] = [];
    for (const item of value) {
        if (typeof item !== 'object' || item === null) {
            return null;
        }
        const { title, documentId } = item as Record<string, unknown>;
        if (typeof title !== 'string') {
            return null;
        }
        sources.push(typeof documentId === 'string' ? { title, documentId } : { title });
    }
    return sources;
}

/** Valida el cuerpo de un 200: { answer: string, sources: [{ title, documentId? }] }. */
function parseSuccess(body: unknown): ChatApiResult | null {
    if (typeof body !== 'object' || body === null) {
        return null;
    }
    const { answer, sources } = body as Record<string, unknown>;
    if (typeof answer !== 'string') {
        return null;
    }
    const parsedSources = parseSources(sources);
    return parsedSources === null ? null : { ok: true, answer, sources: parsedSources };
}

/**
 * Envía una pregunta a POST /api/chat. Nunca lanza: siempre devuelve un resultado,
 * ya sea la respuesta o un error traducido para la interfaz.
 */
export async function postChatMessage(
    message: string,
    fetchImpl: typeof fetch = fetch,
): Promise<ChatApiResult> {
    let response: Response;
    try {
        response = await fetchImpl(CHAT_ENDPOINT, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message }),
        });
    } catch {
        // El motivo técnico (ECONNREFUSED, etc.) no se muestra al usuario.
        return failure('network');
    }

    const body = await readJson(response);

    if (response.ok) {
        return parseSuccess(body) ?? failure('internal');
    }

    const kind = kindForStatus(response.status);
    const retryAfterSeconds =
        kind === 'rate_limited' ? parseRetryAfter(response.headers.get('Retry-After')) : undefined;
    return failure(kind, serverMessage(body), retryAfterSeconds);
}