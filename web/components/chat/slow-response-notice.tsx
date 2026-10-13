'use client'

import { useEffect, useState } from 'react'

const SLOW_RESPONSE_DELAY_MS = 8000

/**
 * Aviso que aparece si la respuesta tarda más de lo habitual. Solo existe mientras se espera:
 * al desmontarse (llegó la respuesta o hubo un error) se reinicia por sí solo.
 */
export function SlowResponseNotice() {
    const [visible, setVisible] = useState(false)

    useEffect(() => {
        const timer = window.setTimeout(() => setVisible(true), SLOW_RESPONSE_DELAY_MS)
        return () => window.clearTimeout(timer)
    }, [])

    if (!visible) return null

    return (
        <p className="text-xs leading-5 text-[var(--water-muted)]">
            La respuesta está tardando más de lo habitual. Puedes esperar un momento.
        </p>
    )
}