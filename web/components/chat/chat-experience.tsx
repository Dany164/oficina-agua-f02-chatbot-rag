'use client'

import { useChat } from '@/hooks/use-chat'
import { ChatHero } from './chat-hero'
import { ChatPanel } from './chat-panel'

export function ChatExperience() {
  const chat = useChat()
  const state = chat.status === 'loading' ? 'thinking' : chat.status === 'error' ? 'error' : 'idle'

  return <div className="mx-auto grid min-h-dvh w-full max-w-6xl items-center px-0 lg:grid-cols-[minmax(0,1fr)_42rem] lg:gap-12 lg:px-6"><ChatHero state={state} /><ChatPanel chat={chat} /></div>
}
