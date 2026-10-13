export interface Point {
    x: number;
    y: number;
}

export interface Size {
    width: number;
    height: number;
}

/**
 * Convierte una coordenada de pantalla en un valor de -1 (inicio) a 1 (final),
 * con 0 en el centro. Cualquier dato no válido se trata como "centro".
 */
function normalize(position: number, length: number): number {
    if (!Number.isFinite(position) || !Number.isFinite(length) || length <= 0) {
        return 0;
    }
    const centered = (position - length / 2) / (length / 2);
    const clamped = Math.min(1, Math.max(-1, centered));
    // Se redondea a 2 decimales; "|| 0" evita devolver -0, que Object.is distingue de 0.
    return Math.round(clamped * 100) / 100 || 0;
}

/** Inclinación de la mascota (-1 a 1 en cada eje) según dónde está el puntero en la ventana. */
export function computeTilt(pointer: Point, viewport: Size): Point {
    return {
        x: normalize(pointer.x, viewport.width),
        y: normalize(pointer.y, viewport.height),
    };
}