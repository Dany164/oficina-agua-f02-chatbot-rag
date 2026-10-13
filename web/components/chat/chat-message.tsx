import { ChatMessage } from '@/lib/chatTypes'
import { ChatSources } from './chat-sources'

export function ChatMessageItem({ message, open, onToggle }: { message: ChatMessage; open: boolean; onToggle: () => void }) {
  const isUser = message.role === 'user'
  return <div className={`flex flex-col gap-2 ${isUser ? 'items-end' : 'items-start'}`}><div className={`relative max-w-[88%] whitespace-pre-wrap px-4 py-3 text-base leading-7 ${isUser ? 'rounded-2xl rounded-br-sm bg-[var(--water-primary)] text-white before:absolute before:bottom-0 before:right-3 before:-mb-2 before:border-8 before:border-transparent before:border-t-[var(--water-primary)] before:border-r-0' : 'rounded-2xl rounded-bl-sm bg-[var(--water-assistant)] text-[var(--water-ink)] before:absolute before:bottom-0 before:left-3 before:-mb-2 before:border-8 before:border-transparent before:border-t-[var(--water-assistant)] before:border-l-0'}`}>{message.content}</div>{!isUser && <ChatSources sources={message.sources} open={open} onToggle={onToggle} />}</div>
}
