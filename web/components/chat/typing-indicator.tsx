import { LOADING_TEXT } from '@/lib/chatCopy'

export function TypingIndicator() { return <div className="flex items-center gap-2 rounded-2xl rounded-bl-sm bg-[var(--water-assistant)] px-4 py-3 text-base font-medium text-[var(--water-ink)]"><span className="size-2 animate-pulse rounded-full bg-[var(--water-primary)]" />{LOADING_TEXT}</div> }
