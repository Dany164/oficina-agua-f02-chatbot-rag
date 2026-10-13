'use client'

import { useEffect, useState } from 'react'
import type { ChatError } from '@/lib/chatTypes'

export function ChatStatusNotice({ error, onRetry }: { error: ChatError; onRetry: () => void }) {
  // Este aviso se monta cuando aparece un error y se desmonta al reintentar o al enviar otra
  // pregunta, así que la cuenta atrás solo necesita arrancar con el valor inicial.
  const [remaining, setRemaining] = useState(error.retryAfterSeconds ?? 0)

  useEffect(() => {
    if (remaining <= 0) return
    const timer = window.setTimeout(() => setRemaining(remaining - 1), 1000)
    return () => window.clearTimeout(timer)
  }, [remaining])

  return (
      <div
          role="alert"
          className="mt-4 flex flex-col gap-2 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-900"
      >
        <strong>{error.message}</strong>
        {error.kind === 'rate_limited' && <span className="text-sm">Podrás intentarlo de nuevo en {remaining} s.</span>}
        <button
            type="button"
            onClick={onRetry}
            disabled={remaining > 0}
            className="self-start font-bold underline underline-offset-4 disabled:cursor-not-allowed disabled:opacity-50"
        >
          Reintentar
        </button>
      </div>
  )
}