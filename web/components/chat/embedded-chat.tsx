'use client'

import { useEffect } from 'react'
import { useChat } from '@/hooks/use-chat'
import { ChatPanel } from './chat-panel'

/** Chat sin héroe ni fondo decorativo, pensado para mostrarse dentro de un iframe (widget). */
export function EmbeddedChat() {
  const chat = useChat()

  useEffect(() => {
    // Dentro del iframe el foco queda aquí y la página anfitriona no recibe las teclas:
    // se le pide al widget que se cierre. El mensaje no lleva datos sensibles.
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && window.parent !== window) {
        window.parent.postMessage({ type: 'oficina-agua-chat:close' }, '*')
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  return <ChatPanel chat={chat} compact />
}