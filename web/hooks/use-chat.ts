'use client';

import { useCallback, useEffect, useReducer, useRef } from 'react';
import { postChatMessage } from '@/lib/chatApi';
import { chatReducer, initialChatState } from '@/lib/chatState';
import { MAX_MESSAGE_LENGTH } from '@/lib/chatTypes';
import type { ChatError, ChatMessage, ChatRole, ChatSource, ChatStatus } from '@/lib/chatTypes';

export interface UseChat {
    messages: ChatMessage[];
    status: ChatStatus;
    error: ChatError | null;
    send: (text: string) => void;
    retry: () => void;
    reset: () => void;
}

/**
 * Conecta el reducer de la conversación con el cliente de /api/chat.
 * La lógica con ramas vive en chatReducer y postChatMessage (ambos con pruebas);
 * aquí solo se orquesta el orden de las llamadas.
 */
export function useChat(): UseChat {
    const [state, dispatch] = useReducer(chatReducer, initialChatState);

    // Contador para ids de mensajes y de peticiones.
    const sequence = useRef(0);
    // Petición en curso, marcada de forma síncrona: evita dos llamadas por un doble clic
    // y permite descartar respuestas que llegan tras un reset o al desmontar.
    const activeRequest = useRef<number | null>(null);
    // Último estado, para que retry() lea la última pregunta sin cambiar de identidad.
    const stateRef = useRef(state);

    useEffect(() => {
        stateRef.current = state;
    }, [state]);

    useEffect(() => {
        return () => {
            activeRequest.current = null;
        };
    }, []);

    const newMessage = useCallback((role: ChatRole, content: string, sources: ChatSource[] = []): ChatMessage => {
        sequence.current += 1;
        return { id: `${role}-${sequence.current}`, role, content, sources };
    }, []);

    const run = useCallback(
        async (requestId: number, text: string) => {
            const result = await postChatMessage(text);
            if (activeRequest.current !== requestId) {
                return; // hubo un reset (o se desmontó) mientras esperábamos
            }
            activeRequest.current = null;
            if (result.ok) {
                dispatch({
                    type: 'receive',
                    requestId,
                    message: newMessage('assistant', result.answer, result.sources),
                });
            } else {
                dispatch({ type: 'fail', requestId, error: result.error });
            }
        },
        [newMessage],
    );

    const send = useCallback(
        (rawText: string) => {
            const text = rawText.trim();
            if (!text || text.length > MAX_MESSAGE_LENGTH || activeRequest.current !== null) {
                return;
            }
            sequence.current += 1;
            const requestId = sequence.current;
            activeRequest.current = requestId;
            dispatch({ type: 'send', requestId, message: newMessage('user', text) });
            void run(requestId, text);
        },
        [newMessage, run],
    );

    const retry = useCallback(() => {
        const current = stateRef.current;
        if (current.status !== 'error' || activeRequest.current !== null) {
            return;
        }
        const lastQuestion = [...current.messages].reverse().find((message) => message.role === 'user');
        if (!lastQuestion) {
            return;
        }
        sequence.current += 1;
        const requestId = sequence.current;
        activeRequest.current = requestId;
        dispatch({ type: 'retry', requestId });
        void run(requestId, lastQuestion.content);
    }, [run]);

    const reset = useCallback(() => {
        activeRequest.current = null;
        dispatch({ type: 'reset' });
    }, []);

    return { messages: state.messages, status: state.status, error: state.error, send, retry, reset };
}