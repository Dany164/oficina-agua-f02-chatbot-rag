# Feature 02 – Chatbot con RAG: requisitos, decisiones y supuestos

> Documento de trabajo, base de los futuros ADR (`docs/adr/`).
> Última actualización: 2026-10-06. Incorpora la información oficial de la **Clase 11**
> (Spring Boot + OpenAPI) y las reglas del mensaje de inicio de implementación (arquitectura por capas,
> historial en DynamoDB, convención de Git). Donde hubiera contradicción con versiones anteriores, **gana lo más reciente
> indicado por el profesor**. Ver la sección 6 para el detalle de lo que se corrigió.

## Estado actual del proyecto

| Elemento | Estado |
|---|---|
| Proyecto Jira | `MicroServicios-Oficina-de-Agua`, clave `MODA`, Scrum/Backlog, sprints de ~1 semana |
| Estructura en Jira | `MODA-2` (Épica: F02 - Implementación del Chatbot con RAG) → `MODA-1` (F02 - Chatbot con RAG en la página pública) |
| `MODA-1` | Por hacer, sin asignar, informador: Dany Mateo. Representa la feature F02; **no se duplica** |
| Historias, subtareas y sprints de F02 | **Ninguno creado todavía**, a propósito |
| Repositorio | `github.com/Dany164/oficina-agua-f02-chatbot-rag`, **privado** durante el desarrollo; público solo si el curso lo exige |
| Trabajo en Git | `main` contiene la estructura inicial, la documentación, el esqueleto Spring Boot de `backend/` y el Maven Wrapper (PR #1 y #2 fusionados). Siguiente rama: `feature/f02-openapi-contract` |
| Integración GitHub–Jira | No asumida; se verifica cuando corresponda |
| Grupo | Es el mismo que desarrolló el monolito en la Fase 1: conocemos el sistema y su base de datos |
| Regla principal de Fase 2 | **El monolito heredado NO se modifica.** Todo es servicio nuevo alrededor de él |
| Stack alineado con el curso | Spring Boot 3.5.x + JDK 17 (ver D-04) |
| Entorno local | JDK 17 Temurin (sistema) listo; Docker, Git, Node 22 e IntelliJ Ultimate listos. Maven vía wrapper (3.9.16). JDK 21 instalado en `C:\dev\jdks\jdk-21.0.12.1+1` **sin uso previsto** |

## Cómo gestionamos Jira

1. Primero definimos qué necesitamos hacer realmente.
2. Cuando una parte está suficientemente clara, se crea **una Historia** para ella.
3. Si una Historia necesita trabajo técnico específico, se crean **subtareas** solo cuando sean útiles.
4. Si surge una necesidad durante el desarrollo, se agrega en ese momento.
5. No se crean tareas ni requisitos que no sabemos si harán falta.
6. Historias = entregables o bloques de trabajo significativos. Subtareas = acciones técnicas concretas.
7. Un ticket puede continuar entre sprints si no se termina, pero se procura dividir en historias manejables.

Las Historias nuevas se crean bajo la épica `MODA-2`. Las ramas y los commits siguen la convención de D-10 (sin clave de Jira);
como **recomendación** (no requisito), el título del Pull Request incluye `MODA-1` para mantener la trazabilidad sin crear historias nuevas.

## Regla de trabajo

1. **Nada queda bloqueado esperando respuestas.** Ante ambigüedad se elige la alternativa más
   defendible, se documenta y se marca como decisión o supuesto.
2. **No se inventan requisitos funcionales.** Este documento contiene requisitos del curso,
   decisiones de arquitectura y supuestos, claramente separados.
3. **Cada decisión debe poder defenderse con argumentos técnicos**, no con "así nos dijeron".
4. **Cada supuesto indica cómo se invalidaría y qué costaría cambiarlo.**
5. Las preguntas de la sección 5 son referencias de confirmación, no bloqueadores.

---

## 1. Requisitos indicados por el profesor

Origen: indicaciones de clase (incluida la Clase 11), según lo reportado por el equipo.

| ID | Requisito | Alcance en F02                                                        |
|---|---|-----------------------------------------------------------------------|
| R-01 | F02 es un chatbot con RAG en la página pública | Directo                                                               |
| R-02 | Next.js como BFF | Directo                                                               |
| R-03 | Spring Boot como backend | Directo                                                               |
| R-04 | Usar Spring AI | Directo                                                               |
| R-05 | PostgreSQL + pgvector, base propia de F02 para el conocimiento del RAG | Directo                                                               |
| R-06 | LLM: el profesor permite **AWS Bedrock, OpenAI u Ollama local** | Directo; proveedor elegido: Ollama en desarrollo (D-05).              |
| R-07 | Usar inyección de dependencias | Directo                                                               |
| R-08 | Usar FeignClient para consumir APIs de otros servicios | F02 **hoy no consume ninguna API**; aplica solo si aparece una (D-03) |
| R-09 | Un repositorio por feature, en la cuenta personal del integrante | Directo                                                               |
| R-10 | Tareas gestionadas en Jira (espacio MODA) | Directo                                                               |
| R-11 | **Contract-First**: OpenAPI YAML → generación de código → Controller que implementa la interfaz generada; Service para la lógica y Repository para los datos | Directo (D-02)                                                        |
| R-12 | El monolito heredado **no se modifica**; Fase 2 son servicios nuevos alrededor | Directo                                                               |
| R-13 | Mantenerse alineados con la clase: **Spring Boot 3.5 y JDK 17** | Directo (D-04)                                                        |
| R-14 | Los microservicios de Fase 2 **nunca** usan `ddl-auto: update`; no modifican el esquema del monolito | Directo (D-01)                                                        |
| R-15 | F01 y F02 son independientes en datos y lógica; **F02 no necesita esperar a F01** para su desarrollo interno | Directo (D-03)                                                        |
| R-16 | La relación principal entre F01 y F02 es la **página pública Next.js/BFF**. El navegador nunca habla directo con la BD ni con el proveedor del LLM | Directo (D-03)                                                        |
| R-17 | El RAG público maneja **solo información institucional/curada**; nunca saldos, pagos, consumos, contratos ni datos personales | Directo (D-06)                                                        |
| R-18 | Controles del chatbot público: longitud máxima de mensajes, rate limiting, costos del LLM, respuesta sin contexto suficiente, alucinaciones | Directo (D-09, D-07)                                                  |
| R-19 | La API key del proveedor, si es externo, vive **solo en backend** | Directo                                                               |
| R-20 | Arquitectura RAG: ingesta (extracción, limpieza, chunking, metadata, embeddings, pgvector) y consulta (embedding de la pregunta, búsqueda semántica, Top-K, threshold de similitud, contexto, prompt, LLM); el prompt obliga a responder con el contexto y permite "no tengo información suficiente" | Directo (D-07, D-08)                                                  |
| R-21 | El historial de conversaciones **puede** registrarse de forma asíncrona en Amazon DynamoDB | **Opcional** y de etapa tardía (D-12)                                 |
| R-22 | Arquitectura **por capas** (Controller → Service → Repository), con Java 17, Spring Boot 3.5, Maven y OpenAPI | Directo (D-11)                                                        |
| R-23 | Errores con `@RestControllerAdvice`: respuestas consistentes y **sin exponer stack traces** | Directo (D-11)                                                        |
| R-24 | Git: `main` estable; ramas como `feature/f02-project-setup`; commits pequeños y de una sola tarea, con formato `tipo(f02): descripción` | Directo (D-10)                                                        |
| R-25 | F02 **no se acopla** a la base de datos de F01 ni directamente a F01 | Directo (D-01, D-03)                                                  |

**Datos de la Clase 11 que no corresponden a F02** (se anotan para no confundirlos):
- El contrato inicial de F01 es `GET /api/v1/public/accounts/{meterCode}` con respuestas 200, 400, 404 y 429,
  servido por `consulta-service`, con usuario de BD de solo lectura. **F02 no lo consume.**
- La indicación de reunirse con el "residente" para conocer el monolito no aplica a nuestro grupo.
- El entregable de F01 (YAML, validación, proyecto vacío que compila, generación, Controller con datos fijos)
  es la referencia práctica del flujo Contract-First que F02 repetirá.

---

## 2. Decisiones de arquitectura

### D-01 · Propiedad y ubicación de los datos del chatbot — *respaldada por R-05, R-12 y R-14*

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| A. Conectar el RAG a MariaDB (monolito) | Acceso inmediato a todo | Base compartida (antipatrón); acopla al esquema del monolito; datos privados al alcance de un bot público; credenciales de la BD del monolito en este servicio |
| B. PostgreSQL + pgvector propia, con conocimiento público curado | Cada servicio es dueño de sus datos; sin datos privados; es lo que pide R-05 | Hay que mantener la ingesta; el conocimiento puede desactualizarse |
| C. Consumir la API de otro servicio para datos vivos | Datos siempre actuales | Depende de que exista esa API; acoplamiento; riesgo con el CAPTCHA de F01 |

- **Decisión:** **B**. **C** solo si aparece una necesidad real, y entonces como integración por servicio/API, nunca
  como acceso directo a la BD del monolito.
- **Justificación defendible:** R-05 ya exige una base propia en PostgreSQL; R-12 prohíbe tocar el monolito;
  un servicio público no debe tener credenciales de una base con datos de clientes.
- **Esquema de la BD de F02:** la BD es nuestra, así que gestionamos su esquema con **migraciones versionadas**
  (propuesta: Flyway, a confirmar al crear el esquema) y, por R-14 y por buena práctica, **nunca `ddl-auto: update`**.
- **Reversibilidad:** alta.

### D-02 · Contratos de API y desarrollo Contract-First — **OFICIAL (Clase 11, R-11)**

Esto **ya no es una opción ni una decisión pendiente.** Fase 2 usa OpenAPI como fuente de verdad.

```
openapi.yaml  →  validar  →  openapi-generator-maven-plugin  →  mvn (genera interfaces y DTOs)
                                                                      ↓
                                      Controller implementa la interfaz generada
                                                                      ↓
                                  Service (lógica de negocio)  →  Repository (acceso a datos)
```

Reglas:

1. Los contratos se escriben **primero** en YAML.
2. Se **validan** antes de implementar.
3. `openapi-generator-maven-plugin` genera interfaces y DTOs en cada build.
4. Los Controllers **implementan las interfaces generadas**.
5. La lógica de negocio va en Service; el acceso a datos, en Repository. **Ninguna lógica en el Controller.**
6. El código generado queda en `target/`: **no se edita ni se versiona.**
7. Cambiar el contrato implica regenerar el código.
8. Si se agrega una operación al YAML y el Controller no la implementa, **el proyecto no compila** hasta completarla.

Configuración del generador indicada en clase:

| Parámetro | Valor |
|---|---|
| `generatorName` | `spring` |
| `interfaceOnly` | `true` |
| `skipDefaultInterface` | `true` |
| `useSpringBoot3` | `true` |
| `modelNameSuffix` | `Dto` |
| `useBeanValidation` | `true` |

Cómo se relaciona con lo que antes era ambiguo (R-12 anterior, "DTO / API Specification / POM"):
- "Desde el POM" = el plugin del generador configurado en `pom.xml`.
- "No depender de un DTO a mano" = los DTOs de la API **se generan** del contrato; no se escriben ni se mantienen manualmente.
- **Siguen existiendo** clases propias dentro del servicio (por ejemplo, el resultado de una búsqueda vectorial). Son objetos
  internos y no forman parte del contrato.
- No se comparten DTOs Java entre microservicios: cada servicio genera los suyos desde su YAML.
- La API bancaria, si existe, pertenece a otra feature y **no forma parte de F02.**

**Detalles que quedan por fijar al crear el proyecto** (no cambian la decisión):
- **Ubicación del YAML.** Recomendación provisional: dentro del módulo, en `backend/src/main/resources/openapi/`.
  Motivo: así el módulo Maven es autocontenido y el build de Docker (que solo ve `backend/`) encuentra el contrato.
  Si en clase se mostró otra convención, se sigue esa.
- **Herramienta de validación del YAML.** Se usa la que se mostró en clase; si no la conocemos, se elige una y se documenta.
- **Versiones del plugin y del generador.** Se verifican en la documentación vigente al configurar el POM.

### D-03 · Relación con F01 y uso de FeignClient — *respaldada por R-15, R-16 y R-08*

La Clase 11 aclara que F01 y F02 son **independientes** en datos y lógica, y que se relacionan en la página pública
(Next.js/BFF). Esto **reemplaza** la idea anterior de una interfaz de integración con un adaptador Feign hacia F01.

```
Navegador → Next.js → BFF ─┬→ consulta-service  (F01)
                           └→ chatbot-service   (F02)
```

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| A. `chatbot-service` llama a `consulta-service` con FeignClient | Cumple R-08 de forma literal | Acopla F02 con F01 sin necesidad; riesgo de saltar el CAPTCHA; contradice R-15 |
| B. Servicios independientes; la integración ocurre en la página pública (BFF) | Cada feature avanza sola; sin acoplamiento entre servicios; coherente con R-15 y R-16 | La UI del chat debe montarse en la página pública |

- **Decisión:** **B**. `chatbot-service` **no consume** ninguna API de otro servicio; por lo tanto **no hay FeignClient en F02 por ahora.**
- **Justificación defendible:** R-08 dice usar Feign *para consumir APIs*. F02 no consume ninguna; agregar Feign sin una API real
  sería código muerto. Si más adelante aparece una necesidad real (D-01, alternativa C), se usa Feign entonces.
- **Qué se retira:** la interfaz `ConsultaPublicaGateway` con stub y adaptador. Ya no tiene motivo de existir.
- **Dónde vivirá la interfaz del chat:** el chat se construye como un componente autocontenido de Next.js, de modo que pueda
  ejecutarse solo y montarse después en la página pública (ver S-03).
- **Reversibilidad:** alta.

### D-04 · Versiones de Java, Spring Boot y Spring AI — **alineadas con la clase (R-13)**

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| A. Spring Boot 3.5.x + Spring AI 1.1.x + JDK 17 | Igual que la clase; `useSpringBoot3=true` del generador coincide; JDK 17 ya instalado | Línea anterior de Spring AI |
| B. Spring Boot 4.1.x + Spring AI 2.0.x + JDK 21 | Líneas más recientes | Se aparta de lo enseñado; cambios incompatibles entre 1.1 y 2.0; el parámetro `useSpringBoot3` apunta a Boot 3 |

- **Decisión:** **A**. La alternativa B queda descartada **por alineación con el curso**, no por razones técnicas.
- **Verificado (2026-10-05):** Spring AI 1.1.x es la línea compatible con Spring Boot 3.5.x; la versión estable es 1.1.8, que
  usa Spring Boot 3.5.15; Spring Boot 3.5 sigue en las versiones estables (3.5.16).
- **Se verifica al crear el proyecto:** versiones de parche exactas, el mínimo de Java de Spring AI 1.1.x y la compatibilidad del
  conjunto con el plugin del generador.
- **JDK 21:** queda instalado sin usarse. No estorba (no está en el `PATH`) y puede borrarse cuando se quiera.
- - **Creación del proyecto (2026-10-06):** Spring Initializr ya no ofrece Spring Boot 3.5 (solo 4.x), así que el `pom.xml` se escribió a mano con `spring-boot-starter-parent` 3.5.16. Compila y pasa el test de contexto con Java 17.0.19.
- **Maven:** se usa Maven Wrapper con Maven 3.9.16 (línea 3.9, que Spring Boot 3.5 usa y prueba).
- **Reevaluar** solo si el curso permitiera o pidiera Spring Boot 4.

### D-05 · Proveedor del LLM y de embeddings — *R-06; todavía abierta*
Hechos técnicos relevantes:
- La **dimensión del vector** la fija el modelo de embeddings; cambiarlo obliga a reindexar todo.
- Spring AI abstrae el proveedor (OpenAI, Bedrock y Ollama tienen soporte documentado de chat y embeddings); cambiarlo es sobre
  todo configuración, salvo el reindexado.
- Los tres proveedores permitidos por el profesor son los de esta tabla. **Google y Anthropic ya no son candidatos.**

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| A. OpenAI (chat y embeddings) | Una sola clave y configuración; calidad alta; simple de integrar | Costo por uso; clave externa que proteger; depende de internet |
| B. AWS Bedrock | Permitido por el profesor; opciones de modelos en un solo servicio | Requiere cuenta y credenciales de AWS; más configuración; costo por uso |
| C. Ollama local | Sin costo ni clave; los datos no salen de la máquina; sirve para demo sin internet | Requiere hardware suficiente; modelos pequeños dan respuestas de menor calidad; más lento |

- **Decisión (2026-10-07):** **C, Ollama local, para desarrollo**, con el proveedor **desacoplado** (configurable por propiedades y
  por perfil). Si otro proveedor requiriera clave, vive solo en backend. Bedrock u OpenAI quedan como cambio posible
  (configuración más reindexado). Gemini **no se considera**: no está entre los proveedores permitidos por el profesor.
- **Hardware verificado:** 16 GB de RAM; GPU NVIDIA RTX 3050 con 4 GB de VRAM dedicada.
- **Modelos probados en esta máquina (provisionales):**

  | Uso | Modelo | Resultado |
    |---|---|---|
  | Embeddings | `bge-m3` | Dimensión **1024** (verificada con `ollama show`) |
  | Chat (por defecto) | `qwen2.5:7b` | 5.1 GB, 55% CPU / 45% GPU; respuesta rápida y correcta en español en una prueba simple |
  | Chat (alternativa) | `qwen2.5:3b` | 2.2 GB, 100% GPU |

- **Consecuencia:** el esquema vectorial usa `vector(1024)`. Cambiar de modelo de embeddings exige una nueva migración y reindexar.
- **A reevaluar:** velocidad y calidad con prompts RAG reales, el contexto por defecto de Ollama (4096 tokens, el prompt no debe
  superarlo) y el consumo de RAM junto con Docker e IntelliJ.
### D-06 · Alcance de información del chatbot — **confirmado por la clase (R-17)**

- **Solo información institucional/curada:** horarios, requisitos, procedimientos, tarifas públicas, preguntas frecuentes,
  información institucional y documentación pública.
- **Nunca:** saldos, historial de pagos, consumo individual, contratos, datos personales ni información privada.
- Si se necesitara información privada, se resolvería con autenticación/autorización y **otro flujo**, no con el RAG público.
- **Comportamiento propuesto (diseño nuestro):** ante una pregunta sobre datos de una cuenta, el chatbot no intenta resolverla y
  responde que esa consulta se hace por el canal oficial correspondiente. Cómo se redacta esa derivación se decide al diseñar el prompt.
- **Cómo se refuerza:** los fragmentos indexados llevan `visibility=PUBLIC` y la búsqueda filtra por ese valor (defensa en profundidad).

### D-07 · Cómo se implementa el flujo RAG — *consistente con R-20*

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| A. Advisor de RAG de Spring AI (todo delegado) | Muy poco código | Menos control sobre el caso "sin información" y sobre las fuentes devueltas |
| B. Flujo explícito: embedding de la pregunta → búsqueda Top-K → umbral de similitud → contexto → prompt → generación | Control total; permite responder "sin información" **sin llamar al LLM**; fuentes tomadas de los fragmentos reales; fácil de demostrar | Algo más de código |

- **Decisión:** **B**, usando de Spring AI el `VectorStore`, el modelo de embeddings y el `ChatClient`. Podemos comparar con el advisor después.
- **Justificación defendible:** el objetivo es demostrar que hay RAG y controlar alucinaciones. El flujo explícito permite medir cada
  paso. RAG reduce las respuestas inventadas pero no las elimina; se complementa con umbral, prompt restrictivo y pruebas.

### D-08 · Ingesta de documentos

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| A. Módulo dentro del mismo Spring Boot | Un solo despliegue; simple | Comparte proceso con el servicio de chat |
| B. Servicio separado | Aislamiento | Sobreingeniería para este tamaño |
| C. Script externo | Independiente | Código fuera del proyecto y sin pruebas |

- **Decisión:** **A**, como subpaquete `service/ingestion` separado del de consulta (`service/rag`, ver D-11). Implementación:
  - **Disparo:** un `ApplicationRunner` que solo existe con el perfil `ingest`. No hay endpoint HTTP: así no se expone nada por el BFF,
    no hay que autenticarlo y no entra al contrato OpenAPI. La aplicación termina al acabar la ingesta.
  - **Troceo:** un chunk por sección `##` del Markdown (`MarkdownSectionSplitter`). El texto guardado es `Título - Sección` seguido del
    contenido, para que el embedding capture el tema de la sección. *Recomendación nuestra*; descartado el corte por tamaño fijo porque
    parte ideas por la mitad. Pendiente de revisar si algún documento crece más allá de lo que cabe en `num_ctx` (4096).
  - **Metadatos por chunk:** `documentId`, `title`, `section`, `visibility=PUBLIC`, `contentHash`, `chunkIndex`. `visibility` es lo que
    permitirá filtrar en el chatbot público (R-17).
  - **Idempotencia:** tabla `knowledge_document` (migración V2) con el SHA-256 del contenido (saltos de línea normalizados). Hash igual: se omite.
    Hash distinto o documento nuevo: se borran los chunks del documento por `documentId` y se reinsertan. El hash se registra al final,
    de modo que una ejecución que falla a medias se reintenta completa.
  - **Verificado:** 5 documentos → 16 chunks en `vector_store`; segunda ejecución → 0 ingeridos, 5 sin cambios, conteo intacto.
    Spring AI (`PgVectorStore`) escribe y borra correctamente sobre la tabla creada por Flyway.
- **Nota Contract-First:** si la acción administrativa es un endpoint HTTP, **también se define en el YAML** (D-02); si es un comando o
  una tarea interna, no.
- **Reversibilidad:** alta.

### D-09 · Protección del chatbot público — *R-18*

- **Controles requeridos:** longitud máxima de mensaje, rate limiting, control de costos de llamadas al LLM, respuesta sin contexto
  suficiente y mitigación de alucinaciones.
- **Dónde se aplican (diseño nuestro):** rate limiting y validación de forma en el BFF, para no gastar recursos del backend; validación
  definitiva en `chatbot-service`, que declara los límites en el YAML y los aplica con Bean Validation (`useBeanValidation=true`).
- **CAPTCHA:** no se pidió para F02 (es de F01). Queda como mejora opcional.
- **Reversibilidad:** alta.

### D-10 · Repositorio, ramas y trazabilidad

- **Repositorio:** `oficina-agua-f02-chatbot-rag`, cuenta personal, **privado durante el desarrollo**; se hace público si el curso lo exige.
  Consecuencias: (1) quien deba revisarlo se agrega como colaborador; (2) en cuentas gratuitas la protección de ramas no aplica a repos
  privados, así que "nada va a `main` sin Pull Request" se sostiene por disciplina.
- **Estructura:** un solo repositorio para la feature, con `backend/`, `web/`, `knowledge/` y `docs/`. La carpeta `api/` en la raíz **se retira**
  (el YAML vive dentro de `backend/`, ver D-02). Alternativa descartada: dos repositorios, porque R-09 pide uno por feature.
- **Nombre del servicio:** `chatbot-service`.
  En el commit inicial solo se crean `web/` y `knowledge/` (con `.gitkeep`); `backend/` y `docs/` nacen con contenido real en sus commits.
- **Flujo (R-24):** GitHub Flow: `main` estable, una rama por bloque de trabajo y Pull Request para integrar. Git Flow se descarta por ceremonia excesiva.
- **Convención de ramas:** `feature/f02-<descripción-corta>` (por ejemplo, `feature/f02-project-setup`). Sin clave de Jira.
- **Convención de commits:** `tipo(f02): descripción en inglés, en imperativo`. Tipos en uso: `chore`, `docs`, `feat`, `fix`, `test`, `refactor`, `build`.
  Cada commit trata una sola tarea y se crea cuando la etapa se probó. Ejemplo: `chore(f02): initialize project structure`.
- **Jira:** `MODA-2` (Épica) → `MODA-1` (F02). Las Historias se crean bajo `MODA-2` solo cuando su alcance esté claro y, antes de crear una,
  se identifica y se justifica. `MODA-14` **no** se usa.
- **Trazabilidad (recomendación):** incluir `MODA-1` en el título del Pull Request. La integración automática GitHub–Jira no se asume.

### D-11 · Arquitectura por capas y manejo de errores — *R-22, R-23, R-07*

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| A. Paquetes por capa (`controller`, `service`, `repository`…), con subpaquetes por proceso dentro de `service` | Es lo que pide el curso; estructura conocida y fácil de explicar; los dos procesos RAG quedan separados | Con muchas funciones, las capas crecen |
| B. Paquetes por función (`chat/`, `retrieval/`, `ingestion/`) | Cohesión por funcionalidad | No es la estructura pedida; se aparta del resto de features |

- **Decisión:** **A**. Esto reemplaza mi propuesta anterior de paquetes por función.
- **Estructura prevista** (paquete raíz **fijado**: `com.oficinaagua.chatbot`; artefacto `chatbot-service`):

  ```
  com.oficinaagua.chatbot
  ├── controller/     implementan las interfaces generadas por OpenAPI; sin lógica de negocio
  ├── service/
  │   ├── rag/        consulta: embedding → Top-K → umbral → contexto → prompt → LLM
  │   └── ingestion/  ingesta: extracción → limpieza → chunking → metadata → embeddings → pgvector
  ├── repository/     accesos propios a datos (por ejemplo, el registro de documentos)
  ├── config/         beans de Spring AI, propiedades y límites
  └── exception/      @RestControllerAdvice
  ```
- **Notas:**
  - El `VectorStore` de Spring AI cumple el papel de repositorio vectorial y se inyecta en los servicios.
  - Inyección de dependencias **por constructor** (R-07). Solo se crea una interfaz cuando hay más de una implementación real o una frontera externa.
  - El formato de error (campos y códigos) se define como esquema en el YAML (D-02); el `@RestControllerAdvice` lo produce de forma
    uniforme y **nunca incluye stack traces** (R-23).
  - Las clases generadas (`*Dto` e interfaces) quedan en `target/`, y sus paquetes se fijan al configurar el plugin.

### D-12 · Historial de conversaciones en DynamoDB — *R-21 (opcional)*

El requisito dice que el historial **puede** registrarse de forma asíncrona en DynamoDB. Se trata como **opcional y de etapa tardía.**

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| A. No implementarlo | Menos trabajo; el RAG no depende de él | No cumple la arquitectura general acordada |
| B. Registro síncrono en DynamoDB | Simple | Suma latencia a cada respuesta; un fallo de DynamoDB rompería el chat |
| C. Registro **asíncrono** (`@Async`) en DynamoDB, con fallos aislados | Coherente con la arquitectura; no afecta la respuesta al usuario | Requiere SDK de AWS, credenciales y configuración de la tabla |

- **Decisión:** **C**, pero **después** de que el flujo RAG funcione de punta a punta. No se implementa en el esquema inicial.
- **Principio de diseño:** si el registro falla, el chat **sigue respondiendo** (el error se registra en logs, sin datos del usuario).
- **Supuesto:** es un registro (auditoría o analítica), **no** memoria que se use para construir el prompt (S-09).
- **Recomendaciones (no son requisito):** no guardar IP ni datos innecesarios, porque los mensajes pueden contener datos personales que escriba el
  usuario; limitar el tamaño de lo guardado; evaluar un tiempo de expiración (TTL) de los registros.
- **A verificar en su momento:** SDK y versión, modelo de la tabla, y si se desarrolla contra DynamoDB Local o una cuenta de AWS.
- Las credenciales de AWS viven solo en backend, igual que la API key del proveedor LLM.

---

## 3. Supuestos técnicos razonables

Un supuesto no es un hecho. Cada uno indica qué lo invalidaría y qué cambiaría.

| ID | Supuesto | Qué lo invalidaría | Impacto si falla |
|---|---|---|---|
| S-01 | La API bancaria mencionada pertenece a otra feature (probablemente F03) | Que F02 deba consumirla | Bajo a medio: nueva integración |
| S-02 | No hay documentos oficiales todavía; se usan documentos de prueba | Llegada de documentos reales | Bajo: se reemplazan y se reindexa |
| S-03 | F02 desarrolla su propia app Next.js (UI + BFF del chat) que corre sola; al integrarse, su componente/rutas se montan en la página pública de Next.js | Que se exija un único proyecto Next.js desde el inicio | Medio: reubicar componente y rutas del BFF; el backend no cambia |
| S-04 | El despliegue de la demostración es con Docker local | Que se pida GCP u otra nube | Medio: despliegue adicional, sin cambiar el diseño |
| S-05 | Basta una demostración con fuentes y un dato verificable para probar que es RAG | Una exigencia de métricas | Bajo: se agrega un conjunto de preguntas de evaluación (ya planeado) |
| S-06 | Podremos obtener una clave de OpenAI o AWS, o nuestro equipo soporta Ollama | Ninguna de las tres opciones es viable | Medio: replantear el proveedor |
| S-07 | La ubicación del YAML y la herramienta de validación no están fijadas por la clase | Que la clase muestre una convención distinta | Bajo: mover el archivo o cambiar la herramienta |
| S-08 | Spring AI 1.1.x funciona con Spring Boot 3.5.x y JDK 17 sin problemas | Incompatibilidad al crear el proyecto | Medio: ajustar versiones de parche |
| S-09 | El historial en DynamoDB es un registro de conversaciones, no memoria que alimente al LLM | Que se espere que el chatbot recuerde turnos anteriores | Medio: agregar manejo de contexto conversacional al flujo RAG |
| S-10 | El historial en DynamoDB es opcional (dice "puede registrarse") | Que sea obligatorio para la entrega | Bajo a medio: se adelanta esa etapa; el diseño ya lo prevé |

---

## 4. Entorno verificado

| Elemento | Valor |
|---|---|
| Java del sistema y del proyecto | Temurin 17.0.19 |
| JDK 21 | Temurin 21.0.12.1 en `C:\dev\jdks\jdk-21.0.12.1+1`, fuera del `PATH`, sin uso previsto |
| Docker Desktop | Funcionando |
| IDE | IntelliJ IDEA Ultimate 2026 |

---

## 5. Preguntas de confirmación (no bloqueantes)

1. ¿Qué convención de carpeta y de herramienta de validación se mostró en clase para el YAML de OpenAPI?
2. ¿Quién es dueño del proyecto Next.js de la página pública? ¿Montaremos el chat dentro de él, o F02 entrega su propia app?
3. ¿Hay una máquina con recursos suficientes para Ollama, o se usará un proveedor externo?
4. ¿Cómo se evaluará que la solución implementa RAG? (referencia para el profesor, si surge la oportunidad)

---

## 6. Historial de correcciones

| Elemento | Antes | Ahora |
|---|---|---|
| Organización de paquetes (2026-10-06) | Paquetes por función (`chat/`, `retrieval/`, `ingestion/`) | **Por capas**, como pide el curso (D-11) |
| Historial de conversaciones (2026-10-06) | No contemplado | Requisito opcional R-21: registro asíncrono en DynamoDB, etapa tardía (D-12) |
| Ramas y commits (2026-10-06) | `feature/MODA-XX-…` y `MODA-XX tipo: …` | `feature/f02-…` y `tipo(f02): …`; `MODA-1` solo en el título del PR como recomendación (D-10) |
| Historia "Preparación del repositorio" (2026-10-06) | Propuesta de crearla en Jira | Retirada: es demasiado pequeña; `MODA-1` ya cubre F02 |
| Estructura inicial (2026-10-06) | `backend/`, `web/`, `knowledge/`, `docs/` en el primer commit | Solo `web/` y `knowledge/`; `backend/` y `docs/` con contenido real (D-10) |
| D-02 Contratos | Controllers a mano; generación "a evaluar" | **Contract-First obligatorio**: OpenAPI → generación → Controller implementa la interfaz generada |
| D-04 Versiones | Spring Boot 4.1 + Spring AI 2.0 + JDK 21 | **Spring Boot 3.5 + Spring AI 1.1.x + JDK 17**, alineado con la clase |
| D-03 F01 | Interfaz `ConsultaPublicaGateway` con stub y adaptador Feign | F01 y F02 son independientes; **sin Feign en F02** por ahora; la relación es la página pública (BFF) |
| D-05 Proveedor | Candidatos OpenAI y Google GenAI; Ollama "no cuenta como externo" | Candidatos **Bedrock, OpenAI, Ollama** (permitidos por el profesor); Ollama sí es válido; decisión aún abierta |
| D-05 Proveedor (2026-10-07) | Decisión abierta | Resuelta: Ollama en desarrollo; embeddings `bge-m3` (1024); chat `qwen2.5:7b` provisional con `qwen2.5:3b` como alternativa; Gemini descartado por no estar entre los proveedores permitidos |
| D-10 Estructura | Carpeta `api/` en la raíz | Sin `api/`: el YAML va dentro de `backend/` (provisional) |
| D-06 Alcance | Decisión nuestra | Requisito confirmado por la clase |
| D-09 Abuso | Riesgo de diseño propuesto | Requisito confirmado; se reparte entre BFF y backend |
| Preguntas a F01 y al profesor | Siete preguntas a F01 y tres al profesor | Reemplazadas: el contrato de F01 ya se conoce y no se necesita |
| Supuestos S-01, S-03, S-04 anteriores | DTO/OpenAPI, ubicación de F01, alcance público | Resueltos por la clase y reemplazados por los de la sección 3 |
| Paquete raíz y Maven (2026-10-06) | Nombre del paquete provisional; versión de Maven sin definir | Paquete `com.oficinaagua.chatbot`, artefacto `chatbot-service`; Maven Wrapper 3.9.16; `pom.xml` manual porque Initializr no ofrece Boot 3.5 |
| D-08 Ingesta (2026-10-07) | Acción administrativa sin definir | Perfil `ingest` con `ApplicationRunner`, troceo por `##`, idempotencia con la tabla `knowledge_document` |