import type { ChatClientResult } from '@/lib/chatClient';
import { getClientIp } from '@/lib/clientIp';
import type { BffConfig } from '@/lib/config';
import type { RateLimitResult } from '@/lib/rateLimiter';
import { validateChatRequest } from '@/lib/validation';

export interface ChatHandlerDeps {
    config: Pick<BffConfig, 'maxMessageLength' | 'trustProxyHeaders'>;
    rateLimiter: { check(clientKey: string): RateLimitResult };
    sendMessage: (message: string, clientIp: string) => Promise<ChatClientResult>;
}

const INVALID_REQUEST_MESSAGE = 'La solicitud no es válida.';
const TOO_MANY_REQUESTS_MESSAGE = 'Demasiadas solicitudes. Intenta de nuevo en unos segundos.';
const UNAVAILABLE_MESSAGE = 'El servicio no está disponible temporalmente. Intenta de nuevo más tarde.';
const INTERNAL_ERROR_MESSAGE = 'Ocurrió un error interno.';

const REASON_PHRASES: Record<number, string> = {
    400: 'Bad Request',
    429: 'Too Many Requests',
    500: 'Internal Server Error',
    503: 'Service Unavailable',
};

/** Error con la misma forma que el ErrorResponse del backend (sin el campo opcional details). */
function errorResponse(status: number, message: string, retryAfterSeconds?: number): Response {
    const headers = new Headers();
    if (retryAfterSeconds !== undefined) {
        headers.set('Retry-After', String(retryAfterSeconds));
    }
    return Response.json(
        {
            timestamp: new Date().toISOString(),
            status,
            error: REASON_PHRASES[status] ?? 'Error',
            message,
        },
        { status, headers },
    );
}

export function createChatHandler(deps: ChatHandlerDeps): (request: Request) => Promise<Response> {
    return async function handleChatRequest(request: Request): Promise<Response> {
        try {
            const clientIp = getClientIp(request.headers, deps.config.trustProxyHeaders);

            // El límite va primero: rechazar es barato y no hace falta ni leer el cuerpo.
            const limit = deps.rateLimiter.check(clientIp);
            if (!limit.allowed) {
                return errorResponse(429, TOO_MANY_REQUESTS_MESSAGE, limit.retryAfterSeconds);
            }

            let body: unknown;
            try {
                body = await request.json();
            } catch {
                return errorResponse(400, INVALID_REQUEST_MESSAGE);
            }

            const validation = validateChatRequest(body, deps.config.maxMessageLength);
            if (!validation.ok) {
                return errorResponse(400, validation.error);
            }

            const result = await deps.sendMessage(validation.message, clientIp);
            switch (result.kind) {
                case 'ok':
                    return Response.json(result.data);
                case 'rate_limited':
                    return errorResponse(429, TOO_MANY_REQUESTS_MESSAGE, result.retryAfterSeconds);
                case 'unavailable':
                    return errorResponse(503, UNAVAILABLE_MESSAGE);
            }
        } catch (error) {
            console.error('Error inesperado en POST /api/chat', error);
            return errorResponse(500, INTERNAL_ERROR_MESSAGE);
        }
    };
}