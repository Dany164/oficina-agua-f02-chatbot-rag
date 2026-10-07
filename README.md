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
  `ollama pull bge-m3` (embeddings) y `ollama pull qwen2.5:7b` (chat; alternativa más ligera: `qwen2.5:3b`).

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
| `OLLAMA_CHAT_MODEL` | `qwen2.5:7b` |
| `KNOWLEDGE_PATH` | `../knowledge` (relativa a `backend/`) |

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

## Estado

En desarrollo.

- Hecho: esqueleto del backend, contrato OpenAPI (Contract-First), manejo de errores,
  PostgreSQL + pgvector con Flyway, integración con Ollama e ingesta de documentos.
- Pendiente: flujo de consulta RAG, protección del chat (límite de mensajes y rate limiting),
  interfaz y BFF en Next.js.