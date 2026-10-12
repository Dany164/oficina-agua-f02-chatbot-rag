# oficina-agua-f02-chatbot-rag

F02 - Chatbot con RAG en la página pública (Oficina de Agua, Fase 2).

El chatbot responde preguntas con información institucional pública recuperada
mediante RAG (Retrieval-Augmented Generation), no solo con el conocimiento general del LLM.

## Arquitectura (resumen)

Navegador → Next.js (BFF) → Spring Boot + Spring AI → PostgreSQL + pgvector / LLM externo

## Estructura prevista del repositorio

| Carpeta | Contenido |
|---|---|
| `backend/` | Servicio Spring Boot (contrato OpenAPI, RAG, ingesta) |
| `web/` | Next.js: interfaz del chat y BFF |
| `knowledge/` | Documentos de conocimiento institucional |
| `docs/` | Decisiones de arquitectura |

## Requisitos

- JDK 17, Node.js 22 y Docker Desktop. Docker debe estar abierto también para correr
  los tests del backend (usan Testcontainers con PostgreSQL + pgvector).
- [Ollama](https://ollama.com) instalado en el equipo, con los modelos descargados:
  `ollama pull bge-m3` (embeddings) y `ollama pull qwen2.5:3b` (chat). En equipos con más de 4 GB de VRAM
  se puede usar `qwen2.5:7b` definiendo `OLLAMA_CHAT_MODEL`.

## Configuración

Copiar `.env.example` a `.env` y definir `POSTGRES_PASSWORD`. El archivo `.env` no se sube a Git.

El backend lee estas variables de entorno (solo `POSTGRES_PASSWORD` es obligatoria):

| Variable | Valor por defecto |
|---|---|
| `POSTGRES_PASSWORD` | *(sin valor; obligatoria)* |
| `POSTGRES_HOST` / `POSTGRES_PORT` | `localhost` / `5433` |
| `POSTGRES_DB` / `POSTGRES_USER` | `chatbot` / `chatbot` |
| `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `OLLAMA_EMBEDDING_MODEL` | `bge-m3` |
| `OLLAMA_CHAT_MODEL` | `qwen2.5:3b` |
| `KNOWLEDGE_PATH` | `../knowledge` (relativa a `backend/`) |
| `RAG_TOP_K` | `4` |
| `RAG_SIMILARITY_THRESHOLD` | `0.55` (por debajo, el fragmento se descarta) |
| `RAG_MAX_CONTEXT_CHARS` | `6000` |
| `RATE_LIMIT_MAX_REQUESTS` | `10` (peticiones por cliente y ventana) |
| `RATE_LIMIT_WINDOW_SECONDS` | `60` |
| `RATE_LIMIT_TRUST_FORWARDED_FOR` | `false` (solo `true` si el backend es accesible únicamente a través del BFF) |

El BFF de `web/` lee estas variables solo en el servidor (ejemplo en `web/.env.example`; para cambiarlas, créalo como
`web/.env.local`):

| Variable | Valor por defecto |
|---|---|
| `CHATBOT_API_URL` | `http://localhost:8080` (URL del backend) |
| `CHATBOT_TIMEOUT_MS` | `30000` |
| `BFF_RATE_LIMIT_MAX_REQUESTS` | `10` (peticiones por cliente y ventana) |
| `BFF_RATE_LIMIT_WINDOW_SECONDS` | `60` |
| `BFF_TRUST_PROXY_HEADERS` | `false` (solo `true` detrás de un proxy de confianza) |

## Cómo ejecutar (desarrollo)

Desde la raíz del repositorio, con Docker Desktop y Ollama abiertos:

```bash
docker compose up -d                      # PostgreSQL + pgvector en el puerto 5433
export POSTGRES_PASSWORD='<la de tu .env>'
cd backend
./mvnw clean verify                       # compila, genera el código del contrato y corre los tests
```

### Cargar la base de conocimiento (ingesta)

Los documentos de `knowledge/` se convierten en embeddings y se guardan en pgvector:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=ingest
```

La aplicación procesa los documentos y termina sola. La ingesta es idempotente: un documento
cuyo contenido no cambió se omite (no se vuelve a llamar a Ollama), y uno modificado reemplaza sus chunks.

### Probar el chat

Con la base cargada, arranca el backend (el puerto 8080 debe estar libre):

```bash
./mvnw spring-boot:run
```

Y desde otra terminal:

```bash
curl -s -X POST http://localhost:8080/api/v1/chat/messages \
  -H "Content-Type: application/json" \
  -d '{"message":"Cual es el horario de atencion?"}'
```

La respuesta trae `answer` y `sources`. Si la pregunta no se relaciona con la base de conocimiento, el chatbot responde
que no tiene información suficiente y `sources` viene vacío. La primera consulta tarda más porque Ollama carga los modelos.

Códigos de error: `400` (mensaje vacío, en blanco o de más de 500 caracteres) y `503` (Ollama o PostgreSQL no disponibles).

### Probar el BFF (Next.js)

Con el backend en marcha, en otra terminal:

```bash
cd web
npm install
npm run dev                               # http://localhost:3000
```

Si el backend corre en otro puerto (por ejemplo `SERVER_PORT=8081`), indícalo así:
`CHATBOT_API_URL=http://localhost:8081 npm run dev`.

```bash
curl -s -X POST http://localhost:3000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Cual es el horario de atencion?"}'
```

El BFF responde lo mismo que el backend (`answer` y `sources`). Errores: `400` (mensaje inválido), `429` (demasiadas
peticiones, con `Retry-After`) y `503` (backend no disponible). Los tests se corren con `npm test`.

Nota: si llamas al backend directamente desde Git Bash con tildes, puede responder `400` por la codificación de la consola;
prueba sin tildes o a través del BFF.

## Estado

En desarrollo.

- Hecho: esqueleto del backend, contrato OpenAPI (Contract-First), manejo de errores, PostgreSQL + pgvector con Flyway,
  integración con Ollama, ingesta de documentos y flujo de consulta RAG (umbral de similitud, prompt restrictivo,
  límite de longitud del mensaje, errores 400, 503 y 500), rate limiting por IP (429 con `Retry-After`) y BFF en
  Next.js (`POST /api/chat` con validación, límite propio y errores genéricos).
- Pendiente: pantalla de chat en Next.js, Dockerización del backend e historial en DynamoDB (opcional).
