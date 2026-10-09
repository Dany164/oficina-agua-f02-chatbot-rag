import { describe, expect, it } from 'vitest';
import { validateChatRequest } from '@/lib/validation';

const MAX = 500;

describe('validateChatRequest', () => {
    it('acepta un mensaje normal', () => {
        expect(validateChatRequest({ message: '¿Cuál es el horario?' }, MAX)).toEqual({
            ok: true,
            message: '¿Cuál es el horario?',
        });
    });

    it('acepta un mensaje de exactamente el máximo', () => {
        const message = 'a'.repeat(MAX);

        expect(validateChatRequest({ message }, MAX)).toEqual({ ok: true, message });
    });

    it('rechaza un mensaje que supera el máximo', () => {
        const result = validateChatRequest({ message: 'a'.repeat(MAX + 1) }, MAX);

        expect(result).toEqual({ ok: false, error: 'El mensaje no puede superar 500 caracteres.' });
    });

    it('acepta mensajes con varias líneas y no los modifica', () => {
        const message = 'hola\ncómo estás\n';

        expect(validateChatRequest({ message }, MAX)).toEqual({ ok: true, message });
    });

    it.each(['', '   ', '\n\t '])('rechaza un mensaje en blanco (%j)', (message) => {
        expect(validateChatRequest({ message }, MAX)).toEqual({
            ok: false,
            error: 'El mensaje no puede estar vacío.',
        });
    });

    it.each([{}, { message: 123 }, { message: null }])(
        'rechaza un cuerpo sin mensaje de texto (%j)',
        (body) => {
            expect(validateChatRequest(body, MAX)).toEqual({
                ok: false,
                error: 'El campo "message" es obligatorio y debe ser texto.',
            });
        },
    );

    it.each([null, [], 'texto', 42])('rechaza un cuerpo que no es un objeto (%j)', (body) => {
        expect(validateChatRequest(body, MAX)).toEqual({
            ok: false,
            error: 'La solicitud no es válida.',
        });
    });
});