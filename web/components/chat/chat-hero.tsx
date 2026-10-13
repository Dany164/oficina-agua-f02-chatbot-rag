import { WaterMascot } from './water-mascot'

type ChatHeroProps = { state: 'idle' | 'thinking' | 'error' }

export function ChatHero({ state }: ChatHeroProps) {
  return <section className="hidden min-w-0 flex-col items-end justify-center text-right lg:flex" aria-label="Presentación del asistente"><WaterMascot size="xl" state={state} /><div className="max-w-[28rem] pr-4"><p className="mt-5 text-2xl font-bold tracking-tight text-[var(--water-ink)]">Tu asistente de la Oficina de Agua</p><p className="mt-2 text-base leading-7 text-[var(--water-muted)]">Consulta horarios, requisitos, tarifas, pagos y reportes de fugas.</p></div></section>
}
