import { ChatExperience } from '@/components/chat/chat-experience'

export default function Page() {
  return <main className="relative flex min-h-dvh items-center justify-center overflow-hidden bg-[var(--water-page)] p-0 text-[var(--water-ink)] sm:p-4"><div aria-hidden="true" className="water-background pointer-events-none absolute inset-0 overflow-hidden"><span className="water-background__blob water-background__blob--one" /><span className="water-background__blob water-background__blob--two" /><span className="water-background__bubble water-background__bubble--one" /><span className="water-background__bubble water-background__bubble--two" /><span className="water-background__bubble water-background__bubble--three" /></div><div className="relative z-10 w-full"><ChatExperience /></div></main>
}
