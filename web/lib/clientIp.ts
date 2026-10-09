const UNKNOWN_CLIENT = 'unknown';

/**
 * Devuelve la clave de cliente para el limitador de peticiones.
 *
 * Si no se confía en el proxy, las cabeceras se ignoran (cualquiera puede falsificarlas).
 * Si se confía, se usa la última entrada de X-Forwarded-For: con un único proxy de confianza,
 * esa es la que añadió el proxy y el cliente no puede controlarla.
 */
export function getClientIp(headers: Headers, trustProxyHeaders: boolean): string {
    if (!trustProxyHeaders) {
        return UNKNOWN_CLIENT;
    }

    const forwardedFor = headers.get('x-forwarded-for');
    if (forwardedFor !== null) {
        const entries = forwardedFor
            .split(',')
            .map((entry) => entry.trim())
            .filter((entry) => entry.length > 0);
        const last = entries.at(-1);
        if (last !== undefined) {
            return last;
        }
    }

    const realIp = headers.get('x-real-ip')?.trim();
    return realIp ? realIp : UNKNOWN_CLIENT;
}