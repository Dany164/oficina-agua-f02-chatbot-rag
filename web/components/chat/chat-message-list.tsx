import { ChatMessage } from '@/lib/chatTypes'
import { ChatMessageItem } from './chat-message'

export function ChatMessageList({ messages, openSources, onToggleSource }: { messages: ChatMessage[]; openSources: Record<string, boolean>; onToggleSource: (id: string) => void }) {
  return <div className="flex flex-col gap-4">{messages.map((message) => <ChatMessageItem key={message.id} message={message} open={!!openSources[message.id]} onToggle={() => onToggleSource(message.id)} />)}</div>
}
