import { describe, expect, it } from 'vitest';
import { chatReducer, initialChatState, type ChatState } from '@/lib/chatState';
import type { ChatError, ChatMessage } from '@/lib/chatTypes';

const question: ChatMessage = { id: 'user-1', role: 'user', content: '¿Cuál es el horario?', sources: [] };
const answer: ChatMessage = {
    id: 'assistant-1',
    role: 'assistant',
    content: 'De 8 a 16',
    sources: [{ title: 'Horario' }],
};
const networkError: ChatError = { kind: 'network', message: 'Sin conexión' };

/** Esperando respuesta a la petición 1, con la pregunta ya en la lista. */
const loading: ChatState = { messages: [question], status: 'loading', error: null, requestId: 1 };
/** La petición 1 falló: ya no hay nada en curso. */
const failed: ChatState = { messages: [question], status: 'error', error: networkError, requestId: null };

describe('chatReducer', () => {
    describe('send', () => {
        it('añade la pregunta y pasa a loading', () => {
            const state = chatReducer(initialChatState, { type: 'send', requestId: 1, message: question });

            expect(state).toEqual(loading);
        });

        it('no hace nada si ya hay una petición en curso', () => {
            const second = { ...question, id: 'user-2' };

            const state = chatReducer(loading, { type: 'send', requestId: 2, message: second });

            expect(state).toBe(loading);
        });

        it('tras un error limpia el error y conserva el historial', () => {
            const second = { ...question, id: 'user-2', content: '¿Cuánto cuesta?' };

            const state = chatReducer(failed, { type: 'send', requestId: 2, message: second });

            expect(state).toEqual({ messages: [question, second], status: 'loading', error: null, requestId: 2 });
        });
    });

    describe('retry', () => {
        it('desde un error vuelve a loading sin duplicar mensajes', () => {
            const state = chatReducer(failed, { type: 'retry', requestId: 2 });

            expect(state).toEqual({ messages: [question], status: 'loading', error: null, requestId: 2 });
        });

        it('fuera de un error no hace nada', () => {
            expect(chatReducer(loading, { type: 'retry', requestId: 2 })).toBe(loading);
            expect(chatReducer(initialChatState, { type: 'retry', requestId: 2 })).toBe(initialChatState);
        });
    });

    describe('receive', () => {
        it('añade la respuesta y vuelve a reposo', () => {
            const state = chatReducer(loading, { type: 'receive', requestId: 1, message: answer });

            expect(state).toEqual({ messages: [question, answer], status: 'idle', error: null, requestId: null });
        });

        it('ignora la respuesta de una petición vieja', () => {
            const state = chatReducer(loading, { type: 'receive', requestId: 0, message: answer });

            expect(state).toBe(loading);
        });

        it('ignora una respuesta si no hay petición en curso', () => {
            const state = chatReducer(initialChatState, { type: 'receive', requestId: 1, message: answer });

            expect(state).toBe(initialChatState);
        });
    });

    describe('fail', () => {
        it('guarda el error y conserva el historial', () => {
            const state = chatReducer(loading, { type: 'fail', requestId: 1, error: networkError });

            expect(state).toEqual(failed);
        });

        it('ignora el error de una petición vieja', () => {
            const state = chatReducer(loading, { type: 'fail', requestId: 0, error: networkError });

            expect(state).toBe(loading);
        });
    });

    describe('reset', () => {
        it('vuelve al estado inicial', () => {
            const conversation: ChatState = {
                messages: [question, answer],
                status: 'idle',
                error: null,
                requestId: null,
            };

            expect(chatReducer(conversation, { type: 'reset' })).toEqual(initialChatState);
        });

        it('descarta la respuesta que llega después de reiniciar', () => {
            let state = chatReducer(initialChatState, { type: 'send', requestId: 1, message: question });
            state = chatReducer(state, { type: 'reset' });
            state = chatReducer(state, { type: 'receive', requestId: 1, message: answer });

            expect(state).toEqual(initialChatState);
        });

        it('descarta la respuesta vieja aunque ya haya otra pregunta en curso', () => {
            const second = { ...question, id: 'user-2', content: '¿Cuánto cuesta?' };
            let state = chatReducer(initialChatState, { type: 'send', requestId: 1, message: question });
            state = chatReducer(state, { type: 'reset' });
            state = chatReducer(state, { type: 'send', requestId: 2, message: second });
            state = chatReducer(state, { type: 'receive', requestId: 1, message: answer });

            expect(state).toEqual({ messages: [second], status: 'loading', error: null, requestId: 2 });
        });
    });

    it('no modifica el estado anterior', () => {
        const before = structuredClone(loading);

        chatReducer(loading, { type: 'receive', requestId: 1, message: answer });

        expect(loading).toEqual(before);
    });
});