import type { Metadata } from 'next'
import { EmbeddedChat } from '@/components/chat/embedded-chat'

export const metadata: Metadata = {
  title: 'Asistente virtual · Oficina de Agua',
  robots: { index: false, follow: false },
}

export default function EmbedPage() {
  return (
    <main className="h-dvh w-full bg-[var(--water-surface)]">
      <EmbeddedChat />
    </main>
  )
}
