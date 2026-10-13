# Web y BFF del chatbot

Aplicación Next.js (App Router, TypeScript, React 19) con dos partes:

- **Pantalla de chat** (`app/page.tsx` y `components/chat/`): componentes visuales generados con v0 y adaptados al proyecto
  (Tailwind CSS y `lucide-react`). Decisión D-16.
- **BFF** (`POST /api/chat`): valida el mensaje, aplica un límite de peticiones propio y reenvía la consulta al backend
  (`chatbot-service`). Decisión D-15. Las claves del modelo viven solo en el backend.

Las decisiones de diseño están en `docs/decisiones-arquitectura.md`.

```bash
npm install
npm run dev     # http://localhost:3000
npm test        # tests con Vitest
npm run lint
npm run build
```

## Estructura

- `components/chat/`: componentes visuales (solo presentación).
- `hooks/use-chat.ts`: conecta el estado de la conversación con la pantalla; `hooks/use-mascot-tilt.ts`: inclinación de la mascota.
- `lib/chatApi.ts`: cliente del navegador hacia `/api/chat`; `lib/chatState.ts`: reductor de la conversación;
  `lib/tilt.ts`: cálculo de la inclinación. Los tres tienen tests.
- `lib/chatHandler.ts`, `lib/chatClient.ts`, `lib/validation.ts`, `lib/rateLimiter.ts`: BFF.

## Variables de entorno

Ver `.env.example` y el README de la raíz. Para uso local, crea `web/.env.local` (ignorado por Git), por ejemplo:

```bash
CHATBOT_API_URL=http://localhost:8081
```

Next solo lee ese archivo al arrancar: reinicia `npm run dev` tras cambiarlo.