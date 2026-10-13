import { describe, expect, it } from 'vitest';
import { computeTilt } from '@/lib/tilt';

const viewport = { width: 1000, height: 600 };

describe('computeTilt', () => {
    it('devuelve 0, 0 en el centro de la ventana', () => {
        expect(computeTilt({ x: 500, y: 300 }, viewport)).toEqual({ x: 0, y: 0 });
    });

    it('devuelve -1 y 1 en los bordes izquierdo y derecho', () => {
        expect(computeTilt({ x: 0, y: 300 }, viewport).x).toBe(-1);
        expect(computeTilt({ x: 1000, y: 300 }, viewport).x).toBe(1);
    });

    it('devuelve -1 y 1 en los bordes superior e inferior', () => {
        expect(computeTilt({ x: 500, y: 0 }, viewport).y).toBe(-1);
        expect(computeTilt({ x: 500, y: 600 }, viewport).y).toBe(1);
    });

    it('interpola entre el centro y el borde', () => {
        expect(computeTilt({ x: 250, y: 150 }, viewport)).toEqual({ x: -0.5, y: -0.5 });
    });

    it('recorta a -1 y 1 cuando el puntero queda fuera de la ventana', () => {
        expect(computeTilt({ x: -50, y: 700 }, viewport)).toEqual({ x: -1, y: 1 });
        expect(computeTilt({ x: 2000, y: -10 }, viewport)).toEqual({ x: 1, y: -1 });
    });

    it('redondea a 2 decimales', () => {
        expect(computeTilt({ x: 333, y: 300 }, viewport).x).toBe(-0.33);
    });

    it('nunca devuelve -0', () => {
        const result = computeTilt({ x: 499.9, y: 299.9 }, viewport);

        expect(Object.is(result.x, 0)).toBe(true);
        expect(Object.is(result.y, 0)).toBe(true);
    });

    it('devuelve 0, 0 si la ventana no tiene tamaño', () => {
        expect(computeTilt({ x: 10, y: 10 }, { width: 0, height: 0 })).toEqual({ x: 0, y: 0 });
    });

    it('devuelve 0, 0 si el puntero no es un número finito', () => {
        expect(computeTilt({ x: Number.NaN, y: Number.POSITIVE_INFINITY }, viewport)).toEqual({ x: 0, y: 0 });
    });
});