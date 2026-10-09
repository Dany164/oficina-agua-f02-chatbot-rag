# Web y BFF del chatbot

Aplicación Next.js (App Router, TypeScript). Por ahora contiene el BFF: `POST /api/chat`, que valida el mensaje, aplica
un límite de peticiones propio y reenvía la consulta al backend (`chatbot-service`). Las decisiones de diseño están en
`docs/decisiones-arquitectura.md` (D-15).

```bash
npm install
npm run dev     # http://localhost:3000
npm test        # tests con Vitest
npm run lint
npm run build
```

Variables de entorno: ver `.env.example` y el README de la raíz.
