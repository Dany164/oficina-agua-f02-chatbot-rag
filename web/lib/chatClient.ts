import type { BffConfig } from '@/lib/config';

export interface ChatSource {
    title: string;
    documentId?: string;
}

export interface ChatResponse {
    answer: string;
    sources: ChatSource[];
}

export type ChatClientResult =
    | { kind: 'ok'; data: ChatResponse }
    | { kind: 'rate_limited'; retryAfterSeconds: number }
    | { kind: 'unavailable' };

const CHAT_PATH = '/api/v1/chat/messages';
const DEFAULT_RETRY_AFTER_SECONDS = 60;

/**
 * Envía el mensaje a chatbot-service y traduce su respuesta a tres resultados.
 * Los detalles de cualquier fallo se quedan en el log del servidor: el navegador nunca los ve.
 */
export async function sendChatMessage(
    message: string,
    clientIp: string,
    config: Pick<BffConfig, 'chatApiUrl' | 'requestTimeoutMs'>,
    fetchFn: typeof fetch = fetch,
): Promise<ChatClientResult> {
    let response: Response;
    try {
        response = await fetchFn(`${config.chatApiUrl}${CHAT_PATH}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-Forwarded-For': clientIp,
            },
            body: JSON.stringify({ message }),
            signal: AbortSignal.timeout(config.requestTimeoutMs),
        });
    } catch (error) {
        console.error('No se pudo contactar con chatbot-service', error);
        return { kind: 'unavailable' };
    }

    if (response.status === 429) {
        return {
            kind: 'rate_limited',
            retryAfterSeconds: parseRetryAfter(response.headers.get('Retry-After')),
        };
    }
    if (!response.ok) {
        console.error(`chatbot-service respondió con estado ${response.status}`);
        return { kind: 'unavailable' };
    }

    let body: unknown;
    try {
        body = await response.json();
    } catch (error) {
        console.error('chatbot-service devolvió un cuerpo que no es JSON válido', error);
        return { kind: 'unavailable' };
    }

    const data = parseChatResponse(body);
    if (data === null) {
        console.error('chatbot-service devolvió una respuesta con una forma inesperada');
        return { kind: 'unavailable' };
    }
    return { kind: 'ok', data };
}

function parseRetryAfter(value: string | null): number {
    const seconds = value === null ? Number.NaN : Number(value);
    return Number.isInteger(seconds) && seconds >= 1 ? seconds : DEFAULT_RETRY_AFTER_SECONDS;
}

/** Valida la forma del contrato y reconstruye el objeto con solo los campos conocidos. */
function parseChatResponse(body: unknown): ChatResponse | null {
    if (typeof body !== 'object' || body === null) {
        return null;
    }
    const { answer, sources } = body as Record<string, unknown>;
    if (typeof answer !== 'string' || !Array.isArray(sources)) {
        return null;
    }

    const parsedSources: ChatSource[] = [];
    for (const source of sources) {
        if (typeof source !== 'object' || source === null) {
            return null;
        }
        const { title, documentId } = source as Record<string, unknown>;
        if (typeof title !== 'string') {
            return null;
        }
        if (documentId !== undefined && documentId !== null && typeof documentId !== 'string') {
            return null;
        }
        parsedSources.push(typeof documentId === 'string' ? { title, documentId } : { title });
    }
    return { answer, sources: parsedSources };
}