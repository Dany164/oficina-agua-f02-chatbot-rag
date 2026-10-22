'use client'

import { useChat } from '@/hooks/use-chat'
import { ChatPanel } from './chat-panel'

/** Chat sin héroe ni fondo decorativo, pensado para mostrarse dentro de un iframe (widget). */
export function EmbeddedChat() {
  const chat = useChat()
  return <ChatPanel chat={chat} compact />
}
