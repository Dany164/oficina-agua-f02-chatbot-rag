/*
 * Widget del Asistente virtual de la Oficina de Agua.
 *
 * Uso en cualquier página (una sola línea):
 *   <script src="https://DOMINIO_DEL_CHATBOT/widget.js" defer></script>
 *
 * Muestra un botón flotante que abre el chat (/embed) dentro de un iframe.
 * El iframe se crea al abrir por primera vez, así que no genera tráfico al cargar la página.
 * Opcional: data-chat-url en la etiqueta script para apuntar a otra URL del chat.
 */
(function () {
    'use strict';

    if (window.__oficinaAguaChatWidget) return;
    window.__oficinaAguaChatWidget = true;

    var script = document.currentScript;
    if (!script) {
        console.warn('[oficina-agua-chat] No se pudo determinar la URL del widget.');
        return;
    }

    var COLOR = '#00607a';
    var chatUrl = script.getAttribute('data-chat-url') || new URL('/embed', script.src).href;

    var ICON_CHAT =
        '<svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>';
    var ICON_CLOSE =
        '<svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12"/></svg>';

    var CSS = [
        ':host { all: initial; }',
        '.launcher { position: fixed; right: 16px; bottom: 16px; z-index: 2147483000; width: 56px; height: 56px;',
        '  border: 0; border-radius: 50%; background: ' + COLOR + '; color: #fff; cursor: pointer;',
        '  display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25); }',
        '.launcher:focus-visible { outline: 3px solid #7dd3fc; outline-offset: 3px; }',
        '.panel { position: fixed; right: 16px; bottom: 88px; z-index: 2147483000; width: min(400px, calc(100vw - 32px));',
        '  height: min(600px, calc(100vh - 104px)); height: min(600px, calc(100dvh - 104px));',
        '  overflow: hidden; border-radius: 16px; background: #fff; box-shadow: 0 16px 48px rgba(0, 0, 0, 0.3); }',
        '.panel iframe { display: block; width: 100%; height: 100%; border: 0; }',
        '@media (max-width: 480px) {',
        '  .launcher { right: 12px; bottom: 12px; }',
        '  .panel { right: 8px; bottom: 80px; width: calc(100vw - 16px);',
        '    height: calc(100vh - 96px); height: calc(100dvh - 96px); }',
        '}',
    ].join('\n');

    function mount() {
        var host = document.createElement('div');
        var root = host.attachShadow({ mode: 'open' });
        root.innerHTML =
            '<style>' + CSS + '</style>' +
            '<div class="panel" id="panel" role="dialog" aria-label="Asistente virtual de la Oficina de Agua" hidden></div>' +
            '<button class="launcher" type="button" aria-label="Abrir asistente virtual" aria-expanded="false" aria-controls="panel"></button>';

        var panel = root.getElementById('panel');
        var launcher = root.querySelector('.launcher');
        var iframe = null;
        var isOpen = false;

        launcher.innerHTML = ICON_CHAT;

        function setOpen(open) {
            isOpen = open;
            if (open && !iframe) {
                iframe = document.createElement('iframe');
                iframe.src = chatUrl;
                iframe.title = 'Asistente virtual de la Oficina de Agua';
                iframe.setAttribute('sandbox', 'allow-scripts allow-same-origin allow-forms');
                panel.appendChild(iframe);
            }
            panel.hidden = !open;
            launcher.innerHTML = open ? ICON_CLOSE : ICON_CHAT;
            launcher.setAttribute('aria-expanded', String(open));
            launcher.setAttribute('aria-label', open ? 'Cerrar asistente virtual' : 'Abrir asistente virtual');
            if (open) {
                iframe.focus();
            } else {
                launcher.focus();
            }
        }

        launcher.addEventListener('click', function () {
            setOpen(!isOpen);
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && isOpen) setOpen(false);
        });

        window.addEventListener('message', function (event) {
            if (
                isOpen &&
                iframe &&
                event.source === iframe.contentWindow &&
                event.data &&
                event.data.type === 'oficina-agua-chat:close'
            ) {
                setOpen(false);
            }
        });

        document.body.appendChild(host);
    }

    if (document.body) {
        mount();
    } else {
        document.addEventListener('DOMContentLoaded', mount);
    }
})();