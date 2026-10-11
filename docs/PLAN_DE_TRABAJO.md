# Plan de trabajo: examen técnico en 1 día

**Equipo:** PM · Arquitecto · BD (SQL Server/T-SQL) · Backend (Spring Boot 3, Java 17) · Frontend (Vue 3 + Vite + keycloak-js)
**Referencias:** [ARQUITECTURA.md](ARQUITECTURA.md) (contrato, fuente de verdad) · [REQUISITOS.md](REQUISITOS.md) · [CHECKLIST_ENTREGA.md](CHECKLIST_ENTREGA.md)

## Responsables por archivo (según la sección 0 del contrato)

| Agente | Archivos |
|--------|----------|
| BD | `database/**`, `keycloak/realm-tarjetas.json`, `docker-compose.yml`, `.env.example`, `.gitignore`, `.gitattributes`, `docs/diagrama-entidad-relacion.md` |
| Backend | `backend/**` (incluye su Dockerfile y el Maven Wrapper) |
| Frontend | `frontend/**` (incluye su Dockerfile y `nginx.conf`) |
| Orquestador | `README.md`, `docs/ARQUITECTURA.md`, `docs/MANUAL_TECNICO.md`, `docs/MANUAL_USUARIO.md` |
| PM | `docs/REQUISITOS.md`, `docs/PLAN_DE_TRABAJO.md`, `docs/CHECKLIST_ENTREGA.md` |

Ningún agente edita archivos de otro.

## Puertos y URLs (contrato)

| Servicio | Host | Interno en Docker |
|----------|------|-------------------|
| SQL Server (`base-datos`) | localhost:1433 | base-datos:1433 |
| Keycloak (`keycloak`) | http://localhost:8180 | keycloak:8080 |
| Backend (`backend`) | http://localhost:8080 (API `/gestion-tarjetas`, Swagger `/swagger-ui.html`) | backend:8080 |
| Frontend en Docker (`frontend`, nginx) | http://localhost:3000 | frontend:80 |
| Frontend en desarrollo (Vite) | http://localhost:5173 | – |

## Fases

| # | Fase | Responsable | Duración aprox. | Salida |
|---|------|-------------|-----------------|--------|
| 0 | Arranque | PM | 0:30 | Requisitos, plan y checklist |
| 1 | Arquitectura | Arquitecto | 1:00 | `docs/ARQUITECTURA.md`: modelo, DDL, API, DTO, seguridad, compose y convenciones **(completada)** |
| 2 | Construcción en paralelo | BD · BE · FE | 4:00 | Scripts T-SQL, realm y compose; API REST completa; UI completa |
| 3 | Integración | Todos | 1:30 | `docker compose up -d --build` funciona y el flujo completo funciona de punta a punta |
| 4 | Validación | PM + Arquitecto | 1:00 | Recorrido de la matriz de requisitos, con estados actualizados a Verificado |
| 5 | Documentación y entrega | PM + Orquestador | 1:00 | README, manual técnico, manual de usuario, diagrama ER y push a GitHub |

### Fase 1: Arquitectura (completada; bloquea a las demás fases)
- Contrato de la API congelado: `/gestion-tarjetas/clientes`, `/gestion-tarjetas/clientes/{idCliente}/solicitudes|tarjetas`, `/gestion-tarjetas/solicitudes` (con `?estado=` y `/{id}/aprobar|rechazar`), `/gestion-tarjetas/tarjetas/{idTarjeta}` y `/gestion-tarjetas/tarjetas/{idTarjeta}/consumos|pagos`.
- Modelo: `cliente` 1─N `solicitud_tarjeta`, `solicitud_tarjeta` 1─0..1 `tarjeta_credito`, `cliente` 1─N `tarjeta_credito`, `tarjeta_credito` 1─N `consumo` y `tarjeta_credito` 1─N `pago`.
- Realm `tarjetas`, cliente público `frontend-tarjetas` (PKCE S256), roles `ADMINISTRADOR` y `OPERADOR`, y usuarios demo `admin` y `operador`.
- Ningún agente agrega endpoints, campos ni rutas que no estén en el contrato sin coordinarlo antes.

### Fase 2: Construcción en paralelo
- **BD:**
  - `01_crear_base_datos.sql`: BD `GestionTarjetasCredito` con collation `Modern_Spanish_CI_AS`, más el login y usuario `usuario_tarjetas`.
  - `02_crear_esquema.sql`: tablas con `VARCHAR`, `DECIMAL(18,2)` y `DATETIME2(3)`, y constraints con nombre.
  - `03_datos_semilla.sql`: solo se ejecuta si `dbo.cliente` está vacía.
  - `inicializar-base-datos.sh`, realm JSON, `docker-compose.yml`, `.env.example` y diagrama ER.
- **Backend:**
  - Entidades con Lombok, repositorios, servicios `@Transactional` y controladores con DTO `record`.
  - Mappers estáticos, `ManejadorGlobalExcepciones`, `ConfiguracionSeguridad` con un `JwtDecoder` propio, `ConvertidorRolesKeycloak`, CORS y springdoc.
  - Maven Wrapper generado con la imagen `maven:3.9.9-eclipse-temurin-17`.
  - Pruebas unitarias (RNF-11).
- **Frontend:**
  - Vue Router con las vistas del contrato (ListaClientesVista, FormularioClienteVista, DetalleClienteVista, BandejaSolicitudesVista, DetalleTarjetaVista y NoEncontradoVista).
  - `clienteHttp.js` con interceptores de token y 401, y `servicioAutenticacion.js`.
  - Componentes compartidos y CSS propio sin librería de UI.

### Fase 3: Integración
- Se prueba el flujo:
  1. Login como `admin`, crear un cliente y registrar una solicitud.
  2. Aprobarla y verificar que se crea la tarjeta (límite = monto, saldo 0).
  3. Registrar un consumo y luego un pago.
  4. Registrar un consumo que excede el límite (debe dar 422) y un pago mayor al saldo (debe dar 422).
  5. Eliminar el cliente y verificar la cascada.
- Se repite como `operador`: debe recibir 403 al aprobar o eliminar, y los botones deben estar ocultos.
- Se verifican CORS (orígenes 5173 y 3000) y el issuer (`http://localhost:8180/realms/tarjetas`).

### Fase 4: Validación
- Se recorre cada fila de REQUISITOS.md y se marca como Verificado con evidencia (Swagger, curl o captura).
- Se clona el repositorio desde cero, se ejecuta `cp .env.example .env` y `docker compose up -d --build`, y se usa la app solo con las instrucciones del README y del manual técnico.

## Riesgos y mitigación

| # | Riesgo | Impacto | Mitigación (según el contrato) |
|---|--------|---------|--------------------------------|
| R1 | **Cascadas múltiples en SQL Server.** Si `tarjeta_credito` tuviera CASCADE hacia `cliente` y también hacia `solicitud_tarjeta`, SQL Server fallaría con "multiple cascade paths". | Alto: el esquema no se crea. | Una sola ruta: `cliente → solicitud_tarjeta` CASCADE, `solicitud_tarjeta → tarjeta_credito` CASCADE, `cliente → tarjeta_credito` **NO ACTION** y `tarjeta_credito → consumo/pago` CASCADE. `ClienteService.eliminar` borra primero las tarjetas del cliente y luego el cliente, en una sola transacción. Se prueba al inicio de la Fase 2. |
| R2 | **Issuer de Keycloak dentro y fuera de Docker.** El navegador obtiene el token por localhost:8180, pero el backend en Docker llega a Keycloak por `keycloak:8080`. | Alto: todas las llamadas responden 401. | Keycloak usa `KC_HOSTNAME=http://localhost:8180` y `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`. El backend **no usa discovery**: tiene un `JwtDecoder` propio con `KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/tarjetas/protocol/openid-connect/certs` y `KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/tarjetas`. En local ambas variables apuntan a localhost:8180. |
| R3 | **`mvn` no está instalado.** | Medio: no se puede compilar localmente. | El Maven Wrapper se genera con `docker run ... maven:3.9.9-eclipse-temurin-17 mvn -N wrapper:wrapper -Dmaven=3.9.9`. Se versiona `mvnw` con LF (`.gitattributes`) y permiso de ejecución. El Dockerfile es multi-stage y no depende del host. |
| R4 | **SQL Server en Apple Silicon (arm64).** `mcr.microsoft.com/mssql/server:2022-latest` solo existe para amd64. | Medio: no arranca o arranca lento. | `platform: linux/amd64` en `base-datos` e `inicializador-base-datos`, y "Use Rosetta for x86_64/amd64 emulation" activado en Docker Desktop. Healthcheck con `sqlcmd` (`start_period` 30 s y 20 reintentos). Se anota en el README y en el manual técnico (solución de problemas). |
| R5 | **Tiempo (un solo día).** | Alto | El contrato está congelado. Se prioriza el MVP de RF y RN antes que los extras. Hay un corte fijo: a las 2 h del final no se agregan funcionalidades. |
| R6 | Orden de arranque (el backend inicia antes de que la BD o Keycloak estén listos). | Medio | `depends_on` con `service_healthy` (base-datos, keycloak y backend) y `service_completed_successfully` (inicializador-base-datos). |
| R7 | Columnas de texto incompatibles con `ddl-auto=validate`. | Medio: el backend no arranca. | Solo `VARCHAR`, nunca `NVARCHAR` ni `CHAR`. Tipos exactos según la sección 2 del contrato. |
| R8 | Keycloak 25 pide "Actualizar perfil" en el primer login. | Bajo | Los usuarios demo tienen nombre, apellido, correo verificado y `requiredActions: []`. |
| R9 | Concurrencia en saldos y decisiones. | Bajo (en el examen) | Bloqueo pesimista (`buscarPorIdConBloqueo`) más el CHECK `ck_tarjeta_credito_saldo_actual`. |

## Definición de "terminado"

Una funcionalidad está terminada cuando:
1. Cumple el criterio de aceptación de su fila en REQUISITOS.md y su estado está en **Verificado**.
2. Respeta literalmente el contrato (ARQUITECTURA.md): nombres, rutas, DTO, códigos HTTP y mensajes.
3. Funciona de punta a punta en Docker Compose (UI en 3000 → API en 8080 → BD), autenticada con Keycloak (8180) y con el rol correcto.
4. Las validaciones y los errores devuelven `ErrorRespuesta` con mensajes en español, sin stack traces en la UI.
5. El endpoint aparece en Swagger.
6. El código está en `main`, sin código comentado ni TODOs, y las pruebas unitarias pasan.

El proyecto está terminado cuando además se cumple todo [CHECKLIST_ENTREGA.md](CHECKLIST_ENTREGA.md) y una persona ajena al equipo puede clonar y ejecutar el sistema siguiendo solo el README y el manual técnico.
