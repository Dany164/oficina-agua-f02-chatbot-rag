import { MessageSquarePlus } from 'lucide-react'
import { WaterMascot } from './water-mascot'

export function ChatHeader({ hasMessages, onReset }: { hasMessages: boolean; onReset: () => void }) {
  return <header className="shrink-0 border-b border-[var(--water-border)] bg-[var(--water-surface)] px-5 py-4 sm:px-7"><div className="flex items-center gap-3"><div className="grid size-12 shrink-0 place-items-center"><WaterMascot size="sm" /></div><div className="min-w-0 flex-1"><h1 className="text-xl font-bold tracking-tight text-[var(--water-ink)]">Asistente virtual</h1><p className="text-sm font-medium text-[var(--water-muted)]">Oficina de Agua</p></div>{hasMessages && <button type="button" onClick={onReset} aria-label="Nueva conversación" title="Nueva conversación" className="grid size-10 shrink-0 place-items-center rounded-xl text-[var(--water-primary)] transition hover:bg-[var(--water-tint)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--water-primary)]"><MessageSquarePlus aria-hidden="true" /></button>}</div></header>
}
