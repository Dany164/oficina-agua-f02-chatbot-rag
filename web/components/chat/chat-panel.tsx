'use client'

import { useState } from 'react'
import type { useChat } from '@/hooks/use-chat'
import { ChatComposer } from './chat-composer'
import { ChatEmptyState } from './chat-empty-state'
import { ChatHeader } from './chat-header'
import { ChatMessageList } from './chat-message-list'
import { ChatStatusNotice } from './chat-status-notice'
import { SlowResponseNotice } from './slow-response-notice'
import { TypingIndicator } from './typing-indicator'

export function ChatPanel({ chat }: { chat: ReturnType<typeof useChat> }) {
  const [input, setInput] = useState('')
  const [openSources, setOpenSources] = useState<Record<string, boolean>>({})
  const hasMessages = chat.messages.length > 0

  return (
      <section
          className="flex h-dvh min-h-0 w-full max-w-[42rem] flex-col overflow-hidden bg-[var(--water-surface)] shadow-[0_24px_70px_rgba(7,72,94,0.16)] sm:h-[min(calc(100dvh-2rem),52rem)] sm:rounded-[2rem] sm:border sm:border-[var(--water-border)]"
          aria-label="Chat con el Asistente virtual"
      >
        <ChatHeader
            hasMessages={hasMessages}
            onReset={() => {
              chat.reset()
              setOpenSources({})
            }}
        />
        <div className="min-h-0 flex-1 overflow-y-auto px-4 py-5 sm:px-7" aria-live="polite">
          {!hasMessages ? (
              <ChatEmptyState onSelect={chat.send} />
          ) : (
              <ChatMessageList
                  messages={chat.messages}
                  openSources={openSources}
                  onToggleSource={(id) => setOpenSources((current) => ({ ...current, [id]: !current[id] }))}
              />
          )}
          {chat.status === 'loading' && (
              <div className="mt-4 flex flex-col gap-2 self-start">
                <TypingIndicator />
                <SlowResponseNotice />
              </div>
          )}
          {chat.error && <ChatStatusNotice error={chat.error} onRetry={chat.retry} />}
        </div>
        <ChatComposer
            value={input}
            onChange={setInput}
            onSubmit={(text) => {
              chat.send(text)
              setInput('')
            }}
            disabled={chat.status === 'loading'}
        />
      </section>
  )
}