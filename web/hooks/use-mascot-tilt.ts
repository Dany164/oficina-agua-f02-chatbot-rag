'use client';

import { useEffect } from 'react';
import { computeTilt } from '@/lib/tilt';
import type { Point } from '@/lib/tilt';

/**
 * Hace que la mascota se incline hacia el puntero: escribe --tilt-x y --tilt-y (de -1 a 1)
 * en el elemento raíz, y el CSS de la mascota las lee. No usa estado de React, así que
 * mover el ratón no provoca renders.
 */
export function useMascotTilt(): void {
    useEffect(() => {
        // Quien pide "reducir movimiento" no recibe ningún efecto de seguimiento.
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            return;
        }

        const root = document.documentElement;
        let frame = 0;
        let latest: Point | null = null;

        function applyTilt() {
            frame = 0;
            if (latest === null) {
                return;
            }
            const tilt = computeTilt(latest, { width: window.innerWidth, height: window.innerHeight });
            root.style.setProperty('--tilt-x', String(tilt.x));
            root.style.setProperty('--tilt-y', String(tilt.y));
        }

        function handlePointerMove(event: PointerEvent) {
            // En pantallas táctiles no hay un puntero que seguir.
            if (event.pointerType === 'touch') {
                return;
            }
            latest = { x: event.clientX, y: event.clientY };
            // Un solo cálculo por fotograma, aunque lleguen muchos eventos.
            if (frame === 0) {
                frame = window.requestAnimationFrame(applyTilt);
            }
        }

        function resetTilt() {
            latest = null;
            root.style.setProperty('--tilt-x', '0');
            root.style.setProperty('--tilt-y', '0');
        }

        window.addEventListener('pointermove', handlePointerMove, { passive: true });
        root.addEventListener('mouseleave', resetTilt);
        window.addEventListener('blur', resetTilt);

        return () => {
            window.removeEventListener('pointermove', handlePointerMove);
            root.removeEventListener('mouseleave', resetTilt);
            window.removeEventListener('blur', resetTilt);
            window.cancelAnimationFrame(frame);
            root.style.removeProperty('--tilt-x');
            root.style.removeProperty('--tilt-y');
        };
    }, []);
}