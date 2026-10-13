/**
 * Límite de caracteres de una pregunta. Debe coincidir con el del backend y el BFF;
 * la validación real sigue estando en el servidor, esto solo evita que la interfaz envíe de más.
 */
export const MAX_MESSAGE_LENGTH = 500;

export interface ChatSource {
    title: string;
    documentId?: string;
}

export type ChatRole = 'user' | 'assistant';

export interface ChatMessage {
    id: string;
    role: ChatRole;
    content: string;
    sources: ChatSource[];
}

export type ChatErrorKind = 'invalid' | 'rate_limited' | 'unavailable' | 'network' | 'internal';

export interface ChatError {
    kind: ChatErrorKind;
    message: string;
    /** Solo en rate_limited: segundos que debe esperar el usuario antes de reintentar. */
    retryAfterSeconds?: number;
}

export type ChatStatus = 'idle' | 'loading' | 'error';
