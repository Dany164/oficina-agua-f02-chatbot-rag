import type { ChatError, ChatMessage, ChatStatus } from '@/lib/chatTypes';

export interface ChatState {
    messages: ChatMessage[];
    status: ChatStatus;
    error: ChatError | null;
    /** Id de la petición en curso; null si no hay ninguna. */
    requestId: number | null;
}

export type ChatAction =
    | { type: 'send'; requestId: number; message: ChatMessage }
    | { type: 'retry'; requestId: number }
    | { type: 'receive'; requestId: number; message: ChatMessage }
    | { type: 'fail'; requestId: number; error: ChatError }
    | { type: 'reset' };

export const initialChatState: ChatState = {
    messages: [],
    status: 'idle',
    error: null,
    requestId: null,
};

/**
 * Una respuesta solo cuenta si hay una petición en curso y es la misma que la pidió.
 * Así se descartan las que llegan tarde (por ejemplo, después de "Nueva conversación").
 */
function isCurrentRequest(state: ChatState, requestId: number): boolean {
    return state.status === 'loading' && state.requestId === requestId;
}

/** Estado de la conversación como función pura: no muta nada y no hace llamadas de red. */
export function chatReducer(state: ChatState, action: ChatAction): ChatState {
    switch (action.type) {
        case 'send':
            // Una sola petición a la vez.
            if (state.status === 'loading') {
                return state;
            }
            return {
                messages: [...state.messages, action.message],
                status: 'loading',
                error: null,
                requestId: action.requestId,
            };

        case 'retry':
            // Solo se reintenta tras un error, y la pregunta ya está en la lista: no se duplica.
            if (state.status !== 'error') {
                return state;
            }
            return { ...state, status: 'loading', error: null, requestId: action.requestId };

        case 'receive':
            if (!isCurrentRequest(state, action.requestId)) {
                return state;
            }
            return {
                messages: [...state.messages, action.message],
                status: 'idle',
                error: null,
                requestId: null,
            };

        case 'fail':
            if (!isCurrentRequest(state, action.requestId)) {
                return state;
            }
            return { ...state, status: 'error', error: action.error, requestId: null };

        case 'reset':
            return initialChatState;
    }
}