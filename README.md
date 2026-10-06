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

JDK 17, Node.js 22 y Docker Desktop.

## Estado

En desarrollo.