export interface BffConfig {
    /** URL base de chatbot-service, sin barra final. Solo se usa en el servidor. */
    chatApiUrl: string;
    /** Tiempo máximo de espera al backend (el modelo puede tardar unos segundos en arrancar). */
    requestTimeoutMs: number;
    rateLimitMaxRequests: number;
    rateLimitWindowSeconds: number;
    /** Fijado por el contrato OpenAPI del backend (maxLength de ChatRequest.message). */
    maxMessageLength: number;
}

function readPositiveInt(name: string, fallback: number): number {
    const raw = process.env[name];
    if (raw === undefined || raw.trim() === '') {
        return fallback;
    }
    const value = Number(raw);
    if (!Number.isInteger(value) || value < 1) {
        throw new Error(`La variable ${name} debe ser un entero positivo`);
    }
    return value;
}

export function loadConfig(): BffConfig {
    const rawUrl = process.env.CHATBOT_API_URL;
    const chatApiUrl = (rawUrl && rawUrl.trim() !== '' ? rawUrl : 'http://localhost:8080').replace(/\/+$/, '');

    return {
        chatApiUrl,
        requestTimeoutMs: readPositiveInt('CHATBOT_TIMEOUT_MS', 30_000),
        rateLimitMaxRequests: readPositiveInt('BFF_RATE_LIMIT_MAX_REQUESTS', 10),
        rateLimitWindowSeconds: readPositiveInt('BFF_RATE_LIMIT_WINDOW_SECONDS', 60),
        maxMessageLength: 500,
    };
}