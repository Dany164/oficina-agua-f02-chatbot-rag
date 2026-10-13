import { ChevronDown } from 'lucide-react'
import { FALLBACK_NOTE } from '@/lib/chatCopy'
import type { ChatSource } from '@/lib/chatTypes'

export function ChatSources({ sources, open, onToggle }: { sources: ChatSource[]; open: boolean; onToggle: () => void }) {
  if (!sources.length) return <p className="ml-2 text-xs leading-5 text-[var(--water-muted)]">{FALLBACK_NOTE}</p>
  return <><button type="button" onClick={onToggle} className="ml-2 inline-flex items-center gap-1 text-xs font-semibold text-[var(--water-muted)] hover:text-[var(--water-primary)]"><ChevronDown className={open ? 'rotate-180' : ''} aria-hidden="true" />Fuentes ({sources.length})</button>{open && <div className="ml-2 flex max-w-[88%] flex-wrap gap-2">{sources.map((source) => <span key={source.title} className="rounded-lg border border-[var(--water-border)] bg-[var(--water-tint)] px-2 py-1 text-[13px] leading-5 text-[var(--water-muted)]">{source.title}</span>)}</div>}</>
}
