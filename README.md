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
docker compose up -d postgres             # solo PostgreSQL + pgvector en el puerto 5433
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

### Importar tarifas y formas de pago desde el monolito

`knowledge/03-tarifas.md` se genera con los datos reales del monolito (tarifas vigentes, regla del exceso y formas de pago).
No se edita a mano. Necesita el monolito encendido, tu IP autorizada, las variables `MONOLITH_DB_*` en el `.env`
(ver `.env.example`) y el certificado en `secrets/oficina-agua-ca.pem`. Desde `backend/`:

```bash
set -a && source ../.env && set +a
SPRING_PROFILES_ACTIVE=import-monolith,ingest ./mvnw spring-boot:run
```

La importación es de solo lectura (usuario `chatbot_ro`, TLS verificado) y termina sola; la ingesta que sigue indexa el documento nuevo.
El chat no necesita el monolito encendido: usa el documento ya generado. Vuelve a ejecutarlo cuando cambien las tarifas y revisa
el resultado con `git diff knowledge/03-tarifas.md` antes de commitearlo. Ver D-19 en `docs/decisiones-arquitectura.md`.

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

### Ejecutar el backend con Docker

También puedes correr PostgreSQL y el backend juntos, sin instalar Java ni Maven. Desde la raíz del repositorio, con Docker
Desktop y Ollama abiertos y `POSTGRES_PASSWORD` definida en `.env`:

```bash
docker compose up -d --build              # PostgreSQL + backend; el backend queda en http://localhost:8081
docker compose logs -f backend            # seguir el arranque (Ctrl+C para salir)
```

La primera construcción tarda varios minutos porque descarga las imágenes y las dependencias; las siguientes reutilizan
la caché. El backend se publica en el puerto 8081 (cámbialo con `BACKEND_PORT` en `.env`). Dentro de Docker se conecta a
PostgreSQL por el nombre del servicio y a Ollama, que sigue corriendo en tu equipo, por `http://host.docker.internal:11434`
(cámbialo con `OLLAMA_BASE_URL`).

Para cargar la base de conocimiento desde el contenedor (la carpeta `knowledge/` se monta en solo lectura):

```bash
docker compose run --rm -e SPRING_PROFILES_ACTIVE=ingest backend
```

Para que la web use este backend, define `CHATBOT_API_URL=http://localhost:8081` en `web/.env.local`. Para detener todo:
`docker compose down` (los datos se conservan en el volumen; `docker compose down -v` los borra).
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

### Probar la interfaz de chat

Con el backend y el BFF en marcha, abre `http://localhost:3000`. Si el backend corre en otro puerto, guarda la dirección una vez
en `web/.env.local` (el archivo está ignorado por Git) y reinicia `npm run dev`:

```bash
printf 'CHATBOT_API_URL=http://localhost:8081\n' > web/.env.local
```

La pantalla muestra las preguntas sugeridas, un indicador mientras se busca la respuesta, las fuentes usadas (como etiquetas
desplegables, no como enlaces) y, ante un error, un aviso con el botón **Reintentar**; con un `429` el botón se habilita al
terminar la cuenta regresiva. En escritorio la mascota sigue el puntero. Las decisiones de diseño están en
`docs/decisiones-arquitectura.md` (D-16).

Si llamas al backend con `curl` desde Git Bash y la pregunta lleva tildes, usa escapes JSON, por ejemplo
`\u00bfCu\u00e1l es el horario?`, o prueba a través del BFF.

### Incrustar el chat como widget

El chat también puede mostrarse como un botón flotante dentro de otra página, por ejemplo la página pública con captcha. La
página anfitriona solo añade una línea con la dirección donde corre `web/`:

```html
<script src="http://localhost:3000/widget.js" defer></script>
```

El botón abre `/embed` (solo el chat) en un iframe, y se cierra con el mismo botón o con Escape. Para que otro sitio pueda
incrustarlo hay que permitir su origen en `web/.env.local` y reiniciar `npm run dev`:

```bash
WIDGET_ALLOWED_ORIGINS=http://localhost:5000
```

Sin esa variable solo se puede incrustar desde el propio sitio. Para probarlo sin la página real, abre
`http://localhost:3000/demo-captcha.html` (mismo origen) o sirve una página propia en otro puerto con la línea anterior. Las
decisiones están en `docs/decisiones-arquitectura.md` (D-18).

## Estado

Completo para la demostración local. El historial en DynamoDB es opcional y no se implementó.

- Backend: contrato OpenAPI (Contract-First), manejo de errores, PostgreSQL + pgvector con Flyway, Ollama, ingesta idempotente, consulta RAG (umbral de similitud, prompt restrictivo, errores 400, 503 y 500), rate limiting por IP (429 con `Retry-After`) y Docker.
- Datos reales: tarifas, tipos de servicio y formas de pago importados del monolito en solo lectura (D-19), y reglas de pago confirmadas por el equipo del monolito.
- Web: BFF en Next.js (`POST /api/chat`), pantalla de chat generada con v0 y widget embebible para la página pública (`/embed` y `widget.js`).
- Pendiente: historial en DynamoDB (opcional).
- Limitaciones conocidas: la importación del monolito es manual, los documentos 01, 02 y 05 de `knowledge/` siguen siendo de prueba y el widget solo se probó en local.
