export type ValidationResult =
    | { ok: true; message: string }
    | { ok: false; error: string };

/**
 * Valida el cuerpo de POST /api/chat con las mismas reglas que el contrato del backend:
 * texto obligatorio, no en blanco y de como máximo maxLength caracteres.
 */
export function validateChatRequest(body: unknown, maxLength: number): ValidationResult {
    if (typeof body !== 'object' || body === null || Array.isArray(body)) {
        return { ok: false, error: 'La solicitud no es válida.' };
    }

    const message = (body as Record<string, unknown>).message;

    if (typeof message !== 'string') {
        return { ok: false, error: 'El campo "message" es obligatorio y debe ser texto.' };
    }
    if (message.trim().length === 0) {
        return { ok: false, error: 'El mensaje no puede estar vacío.' };
    }
    if (message.length > maxLength) {
        return { ok: false, error: `El mensaje no puede superar ${maxLength} caracteres.` };
    }

    return { ok: true, message };
}