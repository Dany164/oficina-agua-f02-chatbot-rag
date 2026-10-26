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
| `MODA-1` | En curso, asignada a Dany Mateo (informador: Dany Mateo). Representa la feature F02; **no se duplica** |
| Historias de F02 | `MODA-4` a `MODA-16` y `MODA-33` (widget) en revisión; `MODA-17` (DynamoDB, opcional) por hacer. La importación del monolito se rastrea con los PR #19 y #20 |
| Repositorio | `github.com/Dany164/oficina-agua-f02-chatbot-rag`, **privado** durante el desarrollo; público solo si el curso lo exige |
| Trabajo en Git | `main` contiene todo F02 salvo lo opcional: backend, ingesta, RAG, rate limiting, BFF, interfaz, Docker, widget, importación del monolito y reglas de pago (PR #1 a #21 fusionados). Sin ramas abiertas |
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
- **Actualización (2026-10-23):** el ingeniero pidió usar la base del monolito. Se atiende con una importación acotada de solo lectura de tres tablas públicas, no con acceso directo desde el chat (ver D-19).
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
- **Aclaración (2026-10-23):** el importador de D-19 lee tres tablas del monolito por JDBC, no consume ninguna API; por eso sigue sin haber FeignClient en F02.
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

### D-05 · Proveedor del LLM y de embeddings — *R-06; decidida (2026-10-07)*
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
  | Chat (por defecto) | `qwen2.5:3b` | 2.2 GB, 100% GPU en una RTX 3050 de 4 GB (ver D-13) |
  | Chat (equipos con más VRAM) | `qwen2.5:7b` | 5.1 GB; en 4 GB de VRAM Ollama falló al cargarlo (`CUDA error`) |

- **Consecuencia:** el esquema vectorial usa `vector(1024)`. Cambiar de modelo de embeddings exige una nueva migración y reindexar.
- **Evaluado (2026-10-08):** velocidad y calidad con prompts RAG reales, en D-13. El contexto del prompt se limita a 6000 caracteres
  (unos 1500 a 2000 tokens, estimación) para quedar dentro de los 4096 tokens de contexto de Ollama.
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
- **Implementación, calibración y resultados:** ver D-13.

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
  - **Documento generado:** `knowledge/03-tarifas.md` ya no se escribe a mano: lo genera el importador de D-19 y esta ingesta lo indexa como cualquier otro documento.
  - **Redacción para el modelo de 3B (2026-10-23):** cada tema que la gente pregunta lleva su propia sección con un título parecido a la pregunta (la reconexión pasó de 0.50 a 0.68-0.70 de similitud al separarla), y las respuestas negativas se escriben como pregunta y respuesta ("¿Hay recargo por pagar tarde? No. ...") porque el modelo las confundía con "sin información". El umbral se mantiene en 0.55.
- **Nota Contract-First:** si la acción administrativa es un endpoint HTTP, **también se define en el YAML** (D-02); si es un comando o
  una tarea interna, no.
- **Reversibilidad:** alta.

### D-09 · Protección del chatbot público — *R-18*

- **Controles requeridos:** longitud máxima de mensaje, rate limiting, control de costos de llamadas al LLM, respuesta sin contexto
  suficiente y mitigación de alucinaciones.
- **Dónde se aplican (diseño nuestro):** rate limiting y validación de forma en el BFF, para no gastar recursos del backend; validación
  definitiva en `chatbot-service`, que declara los límites en el YAML y los aplica con Bean Validation (`useBeanValidation=true`).
- **Rate limiting en el backend:** implementado; ver D-14.
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

### D-13 · Implementación de la consulta RAG — *implementa D-07 (R-18, R-20)*

Flujo implementado en `service/rag`:

1. `KnowledgeRetriever`: búsqueda en pgvector con Top-K, umbral de similitud y filtro `visibility=PUBLIC`.
2. Si no queda ningún fragmento, se responde con una frase fija **sin llamar al LLM**.
3. `RagPromptBuilder`: reglas restrictivas + contexto (con presupuesto de caracteres) + pregunta.
4. `AnswerGenerator` (interfaz) con `SpringAiAnswerGenerator` (`ChatClient` sobre Ollama).
5. `RagService` interpreta la salida. Las fuentes son los fragmentos recuperados; si no hay información, la lista va vacía.

Parámetros, todos configurables por variable de entorno: `RAG_TOP_K` = 4, `RAG_SIMILARITY_THRESHOLD` = 0.55,
`RAG_MAX_CONTEXT_CHARS` = 6000.

**a) Cómo reconoce el servicio que "no hay información"** *(decisión nuestra)*

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| A. Pedirle al LLM una frase exacta y buscarla en la respuesta | Respuesta final redactada por el modelo | Un modelo pequeño la parafrasea |
| B. Pedirle una palabra clave (`SIN_INFORMACION`) y que el servicio escriba la frase final | Fácil de cumplir; salida uniforme; fuentes siempre coherentes | El modelo no redacta la negativa |

- **Decisión:** **B.** Evidencia: con `qwen2.5:3b`, la alternativa A produjo "No **tienen** información suficiente…" junto a 4 fuentes
  (una negativa con fuentes, incoherente). Con B, la pregunta sobre una tarifa que no existe respondió `SIN_INFORMACION` y salió sin fuentes.

**b) Umbral de similitud** *(decisión nuestra, calibrada con mediciones)*

Mejor puntaje de similitud por pregunta, con `bge-m3` y los 5 documentos actuales:

| Pregunta | Mejor score | ¿Debe responder? |
|---|---|---|
| Capital de Francia | < 0.30 | No |
| Instrucción de ignorar reglas | 0.37 | No |
| Pagos con bitcoin | 0.52 | No |
| Tubo roto en la calle | 0.62 | Sí |
| Horario de atención | 0.66 | Sí |
| Cómo pagar la factura | 0.68 | Sí |
| Tarifa residencial | 0.73 | Sí |

- **Decisión:** `0.55`, dentro de la separación entre 0.52 y 0.62.
- **Supuesto:** calibrado con 5 documentos y 16 fragmentos. **Con documentos reales hay que recalibrar** (por eso es configuración).

**c) Modelo de chat local por defecto** *(decisión nuestra)*

`qwen2.5:3b`. El `7b` no cabe en una GPU de 4 GB junto a `bge-m3` y Ollama falló con `CUDA error` al cargarlo. Se puede subir con
`OLLAMA_CHAT_MODEL=qwen2.5:7b` en equipos con más VRAM.

**d) Errores** *(R-23: respuestas uniformes, sin stack traces)*

| Situación | Respuesta |
|---|---|
| Mensaje vacío, en blanco o de más de 500 caracteres | 400, validado por el contrato OpenAPI |
| LLM o base vectorial no disponibles | 503 (`AiServiceUnavailableException`), mensaje genérico |
| Cualquier otro error inesperado | 500 genérico; el detalle solo queda en el log |

Solo los fallos de infraestructura se traducen a 503; un bug real sigue siendo 500 y no se disfraza.

**e) Limitaciones conocidas** *(para no presentarlas como resueltas)*

- Las fuentes mostradas son los fragmentos **recuperados**, no los citados por el modelo; puede aparecer alguno poco relacionado.
- El modelo de 3B a veces responde de forma parcial o con redacción imperfecta (por ejemplo, omitió un tramo de tarifa
  en una respuesta). Se mitigó con una regla del prompt, no se eliminó.
- No hay memoria de conversación (ver S-09).
- La regla del prompt sobre datos personales o de cuenta **no se ha probado con el modelo real**.

**f) Verificación** (2026-10-08): pruebas manuales con Ollama real (`qwen2.5:3b` y `bge-m3`, 100% GPU) y PostgreSQL;
respuestas con fuentes, respuesta sin LLM por umbral, negativa por palabra clave, 400 por mensaje en blanco.

### D-14 · Rate limiting del chatbot — *implementa D-09 (R-18)*

La Clase 11 exige rate limiting y límite de longitud del mensaje (R-18). El resto del diseño es decisión nuestra.

**a) Cómo se implementa el límite** *(decisión nuestra)*

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| Clase propia: ventana fija por cliente en `ConcurrentHashMap` | Unas 40 líneas legibles, sin dependencias, fácil de probar con un `Clock` inyectado | Permite ráfagas en el borde de la ventana; el estado no se comparte entre instancias |
| Bucket4j (token bucket) | Estándar conocido, algoritmo más fino | Dependencia nueva que versionar; con una sola instancia no aporta más que la opción anterior |
| Redis o API gateway | Escala a varias instancias | Sobreingeniería para el alcance de F02 |

**Decisión:** clase propia (`ratelimit/RateLimiter`).

**b) Parámetros** *(recomendación)*: 10 peticiones por cliente cada 60 segundos, configurables con `RATE_LIMIT_MAX_REQUESTS` y
`RATE_LIMIT_WINDOW_SECONDS`. Con el modelo local tardando entre 1 y 10 s por respuesta, 10 por minuto cubre el uso humano normal.

**c) Dónde se engancha:** un `HandlerInterceptor` (`RateLimitInterceptor`) registrado solo sobre `/api/v1/chat/**`
(`RateLimitConfig`). Lanza `RateLimitExceededException`, y el `GlobalExceptionHandler` la convierte en **429** con el mismo
`ErrorResponseDto`, mensaje genérico y la cabecera `Retry-After` (segundos). El límite se aplica **antes** de validar el cuerpo, así
que los mensajes inválidos también cuentan.

**d) Identificación del cliente:** por defecto, la IP de la conexión (`getRemoteAddr()`). Detrás del BFF de Next.js eso es la IP del BFF y todos los usuarios compartirían contador, así que existe la propiedad `chatbot.rate-limit.trust-forwarded-for` (variable `RATE_LIMIT_TRUST_FORWARDED_FOR`, por defecto `false`). Activada, la clave es el **último** valor de `X-Forwarded-For` (el que añadió el BFF, único proxy de confianza); si la cabecera falta o está vacía, se usa la IP de la conexión.

**e) Limitaciones conocidas**

- El estado vive en memoria: solo vale con una instancia del backend; con varias habría que mover el contador a Redis.
- Ventana fija: un cliente puede hacer hasta 2 × el máximo alrededor del cambio de ventana.
- Con la propiedad desactivada (por defecto), todos los usuarios que lleguen a través del BFF comparten la IP del BFF y un mismo contador. Con ella activada, el backend solo debe ser accesible a través del BFF: quien lo alcance directamente puede falsificar `X-Forwarded-For`.
- Las ventanas expiradas se purgan solo cuando el mapa supera 10 000 claves.

**f) Verificación** (2026-10-08): 65 tests en verde (5 del limitador con reloj manual, 3 de MVC con límite 2). Prueba real con el
servidor: 12 peticiones seguidas devuelven 10 × `400` y 2 × `429`; la respuesta 429 incluye `Retry-After` y no expone detalles.
El test web detectó un error durante el desarrollo: usar la URI en lugar de la IP como clave convertía el límite en uno global.

**g) IP reenviada** (2026-10-12): `RateLimitInterceptor` usa el último valor de `X-Forwarded-For` cuando
`chatbot.rate-limit.trust-forwarded-for=true`; por defecto lo ignora. Alternativas descartadas: `server.forward-headers-strategy=native`
(Tomcat `RemoteIpValve`), porque MockMvc no arranca Tomcat y no se podía probar con la suite actual, y porque por defecto confía en
cualquier red privada; y dejar la IP del BFF, que agrupa a todos los usuarios. Verificación: 70 tests en verde (4 nuevos en
`RateLimitForwardedIpWebTest` y 1 en `RateLimitWebTest` que protege el valor por defecto). Prueba real con el servidor y límite 2: con
la propiedad, un cliente reenviado recibe `400`, `400`, `429` y otro cliente distinto `400`; sin ella, los cuatro comparten contador
(`400`, `400`, `429`, `429`). El contrato OpenAPI declara ahora la cabecera `Retry-After` en la respuesta 429.

### D-15 · BFF en Next.js — *implementa D-09 (R-18); la clase exige BFF en Next.js y claves del LLM solo en el backend*

**a) Dónde vive el BFF** *(decisión nuestra)*

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| Route Handler dentro de la app Next (`web/app/api/chat/route.ts`) | Un solo proyecto y un solo despliegue; el navegador solo conoce `/api/chat` | El estado en memoria depende del entorno de ejecución (ver limitaciones) |
| `rewrites` de `next.config` hacia el backend | Cero código | No permite validar ni limitar peticiones propias, que es justo para lo que está el BFF |
| Servidor Node/Express aparte | Control total | Otro proyecto y otro despliegue; sobreingeniería para F02 |

**Decisión:** Route Handler. La lógica está en `web/lib/` como funciones pequeñas y probadas; la ruta solo las conecta.

**b) Responsabilidades del BFF:** validar el mensaje (obligatorio, texto, no vacío, máximo 500 caracteres), aplicar su propio límite
de peticiones, reenviar al backend y traducir sus errores. Nunca tiene claves del LLM. El backend sigue validando y limitando
(defensa en profundidad): el BFF no sustituye sus controles.

**c) Parámetros** *(recomendación)*, todos de servidor (sin prefijo `NEXT_PUBLIC_`):

| Variable | Defecto |
|---|---|
| `CHATBOT_API_URL` | `http://localhost:8080` |
| `CHATBOT_TIMEOUT_MS` | `30000` |
| `BFF_RATE_LIMIT_MAX_REQUESTS` | `10` |
| `BFF_RATE_LIMIT_WINDOW_SECONDS` | `60` |
| `BFF_TRUST_PROXY_HEADERS` | `false` |

**d) Identificación del cliente:** con `BFF_TRUST_PROXY_HEADERS=false` (por defecto) todos comparten la clave `unknown`: es el
comportamiento seguro, porque `X-Forwarded-For` es falsificable. Con `true` se usa el **último** valor de `X-Forwarded-For`
(el que añadió el proxy de confianza) y, si no existe, `X-Real-IP`. El BFF reenvía esa IP al backend en `X-Forwarded-For`; el
backend la usa si `RATE_LIMIT_TRUST_FORWARDED_FOR=true` (D-14); por defecto la ignora.

**e) Errores hacia el navegador:** siempre `{timestamp, status, error, message}` sin detalles internos.

| Caso | Respuesta |
|---|---|
| Cuerpo ilegible, mensaje vacío, no texto o de más de 500 caracteres | `400` (sin llamar al backend) |
| Límite propio o `429` del backend | `429` con `Retry-After` |
| Backend caído, timeout, respuesta inválida o cualquier otro error del backend | `503` con mensaje genérico |
| Error inesperado en el BFF | `500` genérico |

El detalle técnico solo se escribe en el log del servidor. Un `400` del backend se trata como `503`: el BFF ya validó, así que
indicaría un desajuste de contrato, no un error del usuario. La respuesta exitosa se reconstruye solo con los campos del contrato
(`answer`, `sources`).

**f) Limitaciones conocidas**

- El limitador vive en memoria: vale con una instancia; en serverless o con varias instancias cada una cuenta aparte, y en
  desarrollo se reinicia con la recarga en caliente.
- Ventana fija, igual que en el backend: ráfagas de hasta 2 × el máximo en el cambio de ventana.
- Se asume un solo proxy de confianza delante del BFF.
- El BFF no limita el tamaño del cuerpo antes de leerlo; eso se delega al proxy o a la plataforma de despliegue.
- Para que el límite del backend distinga usuarios hay que activar `RATE_LIMIT_TRUST_FORWARDED_FOR=true` (D-14), y entonces el backend no debe quedar accesible sin pasar por el BFF.

**g) Verificación** (2026-10-08): 58 tests, lint y `next build` en verde. Prueba real con backend, PostgreSQL y Ollama:
pregunta del horario → `200` con respuesta y fuentes (10,8 s); mensaje en blanco → `400`; 12 peticiones seguidas →
10 aceptadas y 2 × `429`; backend detenido → `503` genérico, con el detalle (`ECONNREFUSED`) solo en el log. Observación:
llamando al backend directamente desde Git Bash con tildes se obtuvo un `400`, y la misma pregunta sin tildes funcionó;
probablemente es la codificación de la consola (el BFF sí tolera esas peticiones), no un fallo del código.
### D-16 · Interfaz del chat con v0, Tailwind CSS y lucide-react — *la clase exige generar la UI con v0 o Lovable*

**Contexto.** La clase pide generar la interfaz con una herramienta de IA para UI (v0 o Lovable) y el ingeniero pidió React. El proyecto usa Next.js 16 con React 19.

**Decisión.**

- La UI visual (panel, mensajes, mascota, fondo) se generó con **v0**. Es solo presentación: no contiene lógica de red ni de estado.
- Se aceptó **Tailwind CSS** porque v0 genera con esas clases. Dependencias nuevas: `tailwindcss` y `@tailwindcss/postcss` (desarrollo) y `lucide-react`. No se instalaron shadcn, `clsx`, `tailwind-merge`, `tw-animate-css`, `ai`, `@ai-sdk/react` ni `@vercel/analytics`, porque los componentes no los usan.
- La lógica es nuestra y se desarrolló con TDD: `chatApi` (cliente del navegador hacia `/api/chat`), `chatState` (reductor: una petición a la vez, reintento sin duplicar la pregunta, respuestas tardías descartadas) y `tilt` (cálculo de la inclinación de la mascota).
- Los hooks `useChat` y `useMascotTilt` son capas finas sin tests automáticos: toda la lógica con ramas vive en módulos puros probados, y los hooks se verificaron a mano.
- Tipografía del sistema, sin Google Fonts, para que la compilación no dependa de la red. Solo tema claro.
- La mascota es una imagen (`water-mascot.webp`) con efectos en CSS; respeta `prefers-reduced-motion` y no se mueve en pantallas táctiles.

**Alternativas descartadas.** Lovable (sin ventaja para este caso); copiar toda la plantilla de v0 con shadcn (arrastra dependencias sin uso); lógica dentro de los componentes generados (no se puede probar).

**Limitaciones conocidas.**

- `npm audit` informa 5 vulnerabilidades altas, todas en la cadena de desarrollo de `eslint-config-next`. Con `--omit=dev` hay 0. No se usa `--force`.
- Node 22.12 emite `EBADENGINE` para `eslint-visitor-keys`, que pide 22.13 o superior. Se actualizará Node más adelante.
- En Windows, `curl` desde Git Bash envía las tildes en otra codificación y el backend responde 400. Con escapes JSON (`\u00e1`) o desde el navegador funciona.
- Las fuentes se muestran como etiquetas, no como enlaces: los documentos son internos y no tienen dirección pública.
### D-17 · Dockerización del backend — *tarea de empaquetado*

**Contexto.** Hasta ahora solo PostgreSQL corría en Docker; el backend se ejecutaba con `./mvnw`. Para poder levantar el sistema sin instalar Java ni Maven hace falta empaquetar el backend.

**Decisión.**

- `backend/Dockerfile` de dos etapas: Maven con Java 17 compila el jar y una imagen ligera de Java 17 lo ejecuta con un usuario sin privilegios. Los tests no se ejecutan al construir la imagen; se corren con `./mvnw clean verify`.
- El servicio `backend` se añade a `docker-compose.yml` junto a `postgres`: espera su healthcheck, se conecta con el nombre del servicio y el puerto interno (`POSTGRES_HOST=postgres`, `POSTGRES_PORT=5432`) y monta `knowledge/` en solo lectura para la ingesta.
- Ollama sigue corriendo en el equipo y el contenedor lo alcanza por `host.docker.internal`, configurable con `OLLAMA_BASE_URL`. Con Docker Desktop en Windows funcionó sin cambiar la configuración de Ollama.
- El backend se publica en el puerto 8081 (`BACKEND_PORT`) porque el 8080 lo usan otros servicios locales.
- `web/` no se dockeriza todavía: se sigue ejecutando con `npm`.

**Alternativas descartadas.** Ollama en un contenedor (complica el uso de la GPU en Windows y no aporta nada aquí); copiar un jar compilado fuera de Docker (menos reproducible).

**Limitaciones conocidas.**

- La primera construcción es lenta por la descarga de imágenes y dependencias; las siguientes usan la caché.
- La imagen ocupa unos 500 MB en disco.
- No se ha verificado fuera de Docker Desktop en Windows; en otros entornos, Ollama podría tener que escuchar en `0.0.0.0`.

### D-18 · Widget embebible en la página pública — *el ingeniero pidió un widget en la página con captcha de F01, como una dependencia*

**Contexto.** El ingeniero indicó (mensaje del 2026-10-19) que el chat debe ser un widget dentro de la página pública con captcha de F01, no una página aparte, y que debe comportarse como una dependencia: quien programa esa página no debería hacer mayor esfuerzo para incorporarlo. No conocemos la tecnología de esa página; la solución técnica de abajo es *recomendación* nuestra.

**Decisión.**

- `/embed`: ruta de Next.js que muestra solo el panel de chat, sin héroe ni fondo decorativo, reutilizando `useChat` y `ChatPanel` con la opción `compact` (sin la mascota grande, para que las preguntas sugeridas se vean sin desplazarse).
- `web/public/widget.js`: script sin dependencias. La página anfitriona añade una sola línea, `<script src="https://DOMINIO_DEL_CHAT/widget.js" defer></script>`. El script muestra un botón flotante dentro de un Shadow DOM, para que los estilos de la página no lo afecten, y abre `/embed` en un iframe con `sandbox`. El iframe se crea al abrir por primera vez, así que cargar la página anfitriona no genera tráfico al chatbot.
- Se cierra con el botón o con Escape. Como el foco queda dentro del iframe, el chat embebido avisa al widget con `postMessage` y el widget solo acepta ese mensaje si viene de su propio iframe.
- `/embed` envía `Content-Security-Policy: frame-ancestors`: solo el propio sitio puede incrustarlo, más los orígenes de `WIDGET_ALLOWED_ORIGINS` (lista separada por comas; las entradas que no sean URL `http(s)` se ignoran).
- El iframe llama al BFF de su propio origen, así que no hay CORS y las claves del LLM siguen solo en el backend.
- `web/public/demo-captcha.html` simula la página pública para probarlo.

**Alternativas descartadas.** Web Component propio (obliga a reescribir la interfaz fuera de Next y a llamar al backend sin pasar por el BFF); paquete npm de React (solo sirve si la página anfitriona es React). Si la página de F01 resulta ser React o Next, se podría ofrecer además un paquete que envuelva el mismo script.

**Limitaciones conocidas.**

- Requiere que `web/` esté alojada en una dirección accesible desde la página anfitriona. Hoy solo se ejecuta en local (`npm run dev`), por lo que la demostración funciona en el equipo donde corre.
- Se probó con una página de demostración en otro origen (`localhost:5000`) y en Edge; no se ha probado en la página real de F01 ni en otros navegadores.
- El tamaño del panel es fijo en escritorio (400 × 600) y casi de pantalla completa en móvil; el color del botón está fijo en el script.

### D-19 · Importación de datos públicos del monolito — *el ingeniero pidió usar la base del monolito; acota D-01*

El ingeniero indicó de palabra que el chatbot debe usar la base de datos del monolito (se evalúa en la entrega final).
D-01 rechazó conectar el RAG al monolito por ser una base compartida. Se busca una opción que cumpla lo pedido sin ese acoplamiento.

| Alternativa | Ventajas | Desventajas |
|---|---|---|
| A. Consultar el monolito en cada pregunta | Datos siempre actuales | El chat cae si el monolito está apagado (se apaga para ahorrar costos); credenciales del monolito en el servicio público; es el antipatrón de D-01 |
| B. Mantener documentos escritos a mano | Simple | Cifras inventadas o desactualizadas; no usa la base del monolito |
| C. Importador de solo lectura que genera un documento | Datos reales; el chat no depende del monolito en ejecución; credenciales solo al importar | Los datos quedan atrasados hasta volver a importar |

- **Decisión:** **C**. Es una excepción acotada a D-01: la base propia (pgvector) sigue siendo la única que usa el chat.
  - **Qué se lee:** solo `Tb_Tipos_Servicio`, `Tb_Tarifas` y `Tb_Metodos_Pago`, con el usuario `chatbot_ro` (solo `SELECT` sobre esas tres tablas,
    creado por el equipo del monolito, con `REQUIRE SSL` y acceso limitado a nuestra IP). Nunca datos de clientes.
  - **Conexión:** JDBC de solo lectura con TLS verificado contra la CA del proyecto (`sslMode=verify-full`). La clave y el certificado
    viven en `.env` y `secrets/`, ambos ignorados por Git.
  - **Disparo:** perfil `import-monolith` con un `ApplicationRunner` (`@Order(1)`), análogo a D-08. Sin ese perfil el componente ni se crea.
    Se combina con la ingesta: `SPRING_PROFILES_ACTIVE=import-monolith,ingest`.
  - **Generación:** `TariffDocumentGenerator` es una función pura (sin base de datos ni reloj), probada con 12 casos con las filas reales.
    Escribe `knowledge/03-tarifas.md` con las tarifas vigentes, la regla del exceso con ejemplos y las formas de pago.
  - **Regla del exceso (tomada de `Lecturas.php` del monolito):** unidades de 1,000 litros redondeadas hacia arriba;
    total = tarifa del servicio + unidades x tarifa de «Exceso» vigente.
  - **Dato que no está en la base:** «Tarjeta» incluye débito y crédito, confirmado por el equipo del monolito (2026-10-22).
  - **Verificado (2026-10-22):** 3 tipos, 6 tarifas y 3 métodos leídos del monolito real; 5 chunks indexados; el chat respondió Q 150.00
    para 1/2 paja y Q 105.00 para 20,001 litros en 1/4 paja.
- **Limitaciones:** el documento es una foto del momento de la importación; si cambian las tarifas hay que volver a importar y reindexar.
  La importación sigue siendo manual.
- **Confirmación (2026-10-26, verbal):** el ingeniero indicó que no hace falta consulta en vivo y que la importación con la ingesta actual basta para la demostración. No consta por escrito.
- **Reversibilidad:** alta (basta no usar el perfil y mantener el documento).

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
| D-05 Modelo de chat (2026-10-08) | `qwen2.5:7b` por defecto | `qwen2.5:3b` por defecto: el 7b no cabe en 4 GB de VRAM (D-13) |
| D-07 Flujo RAG (2026-10-08) | Diseño propuesto | Implementado y calibrado; umbral 0.55; negativa por palabra clave (D-13) |
| D-09 Rate limiting (2026-10-08) | Límite previsto en BFF y backend | Backend implementado: ventana fija en memoria por IP, 10 peticiones/min, 429 con `Retry-After` (D-14); el BFF aplicará el suyo después |
| D-09 BFF (2026-10-08) | Límite previsto en el BFF | BFF implementado: Route Handler `POST /api/chat` con validación, límite propio de 10 peticiones/min por IP y errores genéricos (D-15); el backend aún no lee la IP real |
| D-14 IP del cliente (2026-10-12) | Detrás del BFF el backend agrupaba a todos los usuarios en una IP | Con `RATE_LIMIT_TRUST_FORWARDED_FOR=true` usa el último valor de `X-Forwarded-For`; por defecto sigue usando la IP de la conexión |
| D-16 Interfaz (2026-10-12) | Interfaz prevista con v0, sin dependencias definidas | v0 + Tailwind CSS + lucide-react; sin shadcn ni paquetes de IA; tipografía del sistema; solo tema claro (D-16) |
| D-16 Lógica de la UI (2026-10-12) | Lógica dentro de los componentes generados | Cliente `chatApi`, reductor `chatState` y cálculo `tilt` con TDD; hooks finos verificados a mano (D-16) |
| D-17 Docker (2026-10-13) | Solo PostgreSQL en Docker Compose; el backend se ejecutaba con `./mvnw` | Backend con Dockerfile de dos etapas y servicio en Compose (puerto 8081); Ollama sigue en el equipo (D-17) |
| D-18 Widget (2026-10-22) | El chat solo existía como página completa en `/` | Widget embebible: ruta `/embed` y script `widget.js` que abre el chat en un iframe, restringido con `frame-ancestors` (D-18) |
| D-19 Datos de tarifas (2026-10-23) | `03-tarifas.md` escrito a mano con cifras inventadas | Generado por un importador de solo lectura desde el monolito; el chat no depende del monolito en ejecución (D-19) |
| D-01 Monolito (2026-10-23) | Nunca acceso directo a la BD del monolito | Se mantiene para el chat; excepción acotada: importación de solo lectura de tres tablas públicas (D-19) |
| D-08 Redacción de documentos (2026-10-23) | Secciones agrupadas por tema; negaciones en frase corta | Una sección por tema preguntado y negaciones como pregunta y respuesta; reglas de pago confirmadas por el equipo del monolito |
