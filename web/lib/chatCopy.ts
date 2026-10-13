import type { ChatErrorKind } from '@/lib/chatTypes';

/** Mensajes de respaldo cuando el servidor no envía uno propio. Nunca incluyen detalles técnicos. */
export const FALLBACK_ERROR_MESSAGES: Record<ChatErrorKind, string> = {
    invalid: 'La solicitud no es válida.',
    rate_limited: 'Demasiadas solicitudes. Intenta de nuevo en unos segundos.',
    unavailable: 'El servicio no está disponible temporalmente. Intenta de nuevo más tarde.',
    network: 'No se pudo conectar con el servicio. Revisa tu conexión e inténtalo de nuevo.',
    internal: 'Ocurrió un error interno.',
};
