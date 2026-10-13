import type { ChatErrorKind } from '@/lib/chatTypes';

/** Mensajes de respaldo cuando el servidor no envía uno propio. Nunca incluyen detalles técnicos. */
export const FALLBACK_ERROR_MESSAGES: Record<ChatErrorKind, string> = {
    invalid: 'La solicitud no es válida.',
    rate_limited: 'Demasiadas solicitudes. Intenta de nuevo en unos segundos.',
    unavailable: 'El servicio no está disponible temporalmente. Intenta de nuevo más tarde.',
    network: 'No se pudo conectar con el servicio. Revisa tu conexión e inténtalo de nuevo.',
    internal: 'Ocurrió un error interno.',
};

/** Textos de la interfaz del chat. */
export const GREETING =
    '¡Hola! Soy el asistente virtual de la Oficina de Agua. Puedo ayudarte con horarios, requisitos, tarifas, pagos y reportes de fugas.';

export const SUGGESTED_QUESTIONS: readonly string[] = [
    '¿Cuál es el horario de atención?',
    '¿Qué requisitos necesito para una nueva conexión?',
    '¿Cuáles son las tarifas del servicio?',
    '¿Cómo puedo pagar mi factura?',
    '¿Cómo reporto una fuga de agua?',
];

export const LOADING_TEXT = 'Buscando en la información de la oficina…';

/** Se muestra cuando una respuesta no trae fuentes (la pregunta quedó fuera de la información disponible). */
export const FALLBACK_NOTE = 'Prueba con una pregunta más concreta sobre horarios, requisitos, tarifas, pagos o fugas.';

export const COMPOSER_NOTICE = 'Cada pregunta se responde por separado: escribe preguntas completas.';