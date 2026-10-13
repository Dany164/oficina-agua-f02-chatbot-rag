'use client'

import type { FormEvent, KeyboardEvent } from 'react'
import { COMPOSER_NOTICE } from '@/lib/chatCopy'
import { MAX_MESSAGE_LENGTH } from '@/lib/chatTypes'

type ChatComposerProps = {
  value: string
  onChange: (value: string) => void
  onSubmit: (value: string) => void
  disabled: boolean
}

export function ChatComposer({ value, onChange, onSubmit, disabled }: ChatComposerProps) {
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const text = value.trim()
    if (text && !disabled) onSubmit(text)
  }

  function keyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    const isComposing = event.nativeEvent.isComposing || event.keyCode === 229
    if (event.key === 'Enter' && !event.shiftKey && !isComposing) {
      event.preventDefault()
      event.currentTarget.form?.requestSubmit()
    }
  }

  return (
      <div className="shrink-0 border-t border-[var(--water-border)] bg-[var(--water-surface)] px-4 pb-[max(1rem,env(safe-area-inset-bottom))] pt-3 sm:px-7">
        <p className="mb-2 text-xs leading-5 text-[var(--water-muted)]">{COMPOSER_NOTICE}</p>
        <form
            onSubmit={submit}
            className="rounded-2xl border border-[var(--water-border)] bg-[var(--water-input)] p-2 focus-within:ring-2 focus-within:ring-[var(--water-primary)]"
            aria-label="Enviar una pregunta"
        >
          <div className="flex items-end gap-2">
          <textarea
              value={value}
              onChange={(event) => onChange(event.target.value.slice(0, MAX_MESSAGE_LENGTH))}
              onKeyDown={keyDown}
              maxLength={MAX_MESSAGE_LENGTH}
              rows={1}
              aria-label="Escribe tu pregunta"
              placeholder="Escribe tu pregunta…"
              className="min-h-11 min-w-0 flex-1 resize-none bg-transparent px-2 py-3 text-base leading-6 text-[var(--water-ink)] outline-none placeholder:text-[var(--water-muted)]"
          />
            <button
                type="submit"
                disabled={!value.trim() || disabled}
                aria-label="Enviar pregunta"
                className="grid size-11 shrink-0 place-items-center rounded-xl bg-[var(--water-primary)] text-white transition hover:bg-[var(--water-primary-strong)] disabled:cursor-not-allowed disabled:bg-[var(--water-border)] disabled:text-[var(--water-primary-strong)]"
            >
              <span aria-hidden="true">→</span>
            </button>
          </div>
          <div className="flex justify-end px-2 text-xs text-[var(--water-muted)]">
            {value.length}/{MAX_MESSAGE_LENGTH}
          </div>
        </form>
      </div>
  )
}