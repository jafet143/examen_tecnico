# Sistema de Gestión de Tarjetas de Crédito

Aplicación web para administrar el ciclo de vida de las tarjetas de crédito de un banco. Cubre desde el registro del cliente y la solicitud de la tarjeta, evaluada con reglas de crédito (ingreso mínimo, estabilidad laboral, mora y una cuenta activa por tipo), hasta la operación diaria de la **cuenta de crédito**: consumos y pagos, ciclo de facturación con pago mínimo del 10 %, mora, interés automático del 5 % mensual, bloqueo, reposición y renovación de tarjetas, cargos y sus ajustes, reversos y cancelación de cuentas. El acceso se controla con 18 permisos agrupados en cuatro perfiles (Administrador, Operador de atención, Cajero y Analista de crédito).

Desarrollada como examen técnico para la plaza de Analista Programador de **Crédito Hipotecario Nacional (CHN)**.

## Funcionalidades principales

- **Resumen operativo:** indicadores de clientes, solicitudes pendientes, cuentas activas, deuda de la cartera y cuentas en mora.
- **Clientes:** alta, edición, búsqueda y eliminación, con código de cliente de 8 dígitos asignado por el sistema.
- **Solicitudes:** registro con validación de requisitos y aprobación (hasta el 150 % del ingreso) o rechazo con motivo.
- **Cuentas y tarjetas:** al aprobar se abre la cuenta con su primera tarjeta; detalle con límite, saldo, disponible e historiales.
- **Consumos y pagos:** registro sobre la cuenta, con opciones de pago (monto en mora, pago mínimo, pago de contado u otro monto).
- **Ciclo, mora e intereses:** corte el día 5, pago hasta el día 25, mora contra el pago mínimo e interés automático del 5 %.
- **Tarjetas:** bloqueo y desbloqueo, reposición con requisitos y comisión por extravío, y renovación sin costo de la tarjeta vencida.
- **Cargos y ajustes:** recargo por mora, devolución de intereses, exoneración de mora y reversión de comisión.
- **Reversos y cancelación:** reverso de movimientos con motivo y cancelación de cuentas al día con saldo Q 0.00, sin borrar historial.
- **Seguridad:** inicio de sesión con Keycloak, permisos por operación y números de cuenta y tarjeta enmascarados salvo permiso.

## Tecnologías

| Capa | Tecnología | Versión |
|---|---|---|
| Frontend | Vue + Vue Router + Vite, keycloak-js, Axios (servido por nginx) | Vue ^3.4, Vue Router ^4.4, Vite ^5.4, keycloak-js 25.0.6, Axios ^1.7, nginx 1.27 |
| Backend | Java + Spring Boot (Web, Data JPA, Validation, OAuth2 Resource Server, Actuator) | Java 17, Spring Boot 3.3.5 |
| Documentación de la API | springdoc-openapi (Swagger UI) | 2.6.0 |
| Base de datos | Microsoft SQL Server (scripts Transact-SQL) | 2022 |
| Identidad | Keycloak (OpenID Connect, Authorization Code + PKCE) | 25.0.6 |
| Pruebas | JUnit 5, Mockito, Spring Security Test | gestionadas por Spring Boot 3.3.5 |
| Contenedores | Docker + Docker Compose | Compose v2 |

## Inicio rápido con Docker

Requisitos:

- Docker Desktop 4.x. En Windows, con el motor **WSL 2** activado (es la opción por defecto); en Mac con Apple Silicon, con la emulación de Rosetta activada.
- Al menos 4 GB de RAM asignados a Docker.
- Los puertos `3000`, `8080`, `8180` y `14330` libres. SQL Server se publica en el `14330`, y no en el `1433` habitual, para no chocar con un SQL Server instalado en la máquina.

Los comandos funcionan igual en PowerShell, CMD, Git Bash y en la terminal de Mac o Linux, salvo el paso 2 en CMD.

1. Clonar el repositorio y entrar a la carpeta:

```bash
git clone https://github.com/jafet143/examen_tecnico.git
```

```bash
cd examen_tecnico
```

2. Crear el archivo de variables de entorno con los valores de demostración. En PowerShell, Git Bash, Mac o Linux:

```bash
cp .env.example .env
```

En CMD (Símbolo del sistema de Windows):

```bat
copy .env.example .env
```

3. Construir y levantar todo el sistema. La primera vez tarda de 3 a 6 minutos, porque descarga las imágenes y compila el backend y el frontend:

```bash
docker compose up -d --build
```

4. Verificar que todo esté en ejecución. `base-datos`, `keycloak` y `backend` deben aparecer como *healthy*, `frontend` como *running* e `inicializador-base-datos` como *exited (0)*, porque solo corre una vez para crear la base:

```bash
docker compose ps
```

Luego abra http://localhost:3000 e ingrese con alguno de los usuarios de la tabla siguiente.

**Si algo falla:**

- *"port is already allocated"*: otro programa usa uno de los puertos. En Windows se identifica con `netstat -ano | findstr :8080` (cambiando el puerto) y en Mac o Linux con `lsof -i :8080`.
- Si antes levantó una versión anterior del proyecto, ejecute `docker compose down -v` antes del paso 3 para empezar con una base limpia.
- La ejecución sin Docker, la configuración y más casos de solución de problemas están en el [manual técnico](docs/Manual_Tecnico.pdf).

## URLs y usuarios de demostración

| Servicio | URL |
|---|---|
| Aplicación web | http://localhost:3000 |
| API REST | http://localhost:8080/gestion-tarjetas |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Consola de Keycloak | http://localhost:8180 (usuario `admin`, contraseña `admin`) |
| SQL Server | `localhost,14330` (usuario `sa`, contraseña en `.env`) |

| Usuario | Contraseña | Perfil | Qué puede hacer |
|---|---|---|---|
| `admin` | `admin123` | Administrador | Todo, incluido ver números completos |
| `supervisor` | `supervisor123` | Administrador | Igual que `admin` |
| `operador` | `operador123` | Operador de atención | Clientes, registrar solicitudes y consumos, bloquear, reponer y renovar tarjetas |
| `cajero` | `cajero123` | Cajero | Consultar clientes y cuentas y registrar pagos |
| `analista` | `analista123` | Analista de crédito | Consultar y aprobar o rechazar solicitudes |

La base de datos se crea con datos de demostración (7 clientes, 6 cuentas y casos de mora, tarjeta vencida, bloqueos y cuenta cancelada). El detalle y un recorrido sugerido están en el [manual técnico](docs/Manual_Tecnico.pdf) (sección 22).

## Requisitos del enunciado

| Requisito | Cómo se cumple |
|---|---|
| Gestión de clientes: agregar, listar, editar y eliminar (nombre, apellido, identificación, fecha de nacimiento, dirección, correo y teléfono) | Pantalla **Clientes** y `/gestion-tarjetas/clientes`. Cada cliente recibe un código de 8 dígitos. No se elimina un cliente con cuentas activas (CL-01). |
| Solicitud de tarjeta con cliente, tipo, fecha e ingreso mensual; estado inicial *Pendiente* | Pantalla **Nueva solicitud**; valida ingreso mínimo, estabilidad laboral y mora antes de registrar. |
| Aprobar con fecha, monto y comentario; rechazar con fecha y motivo | Pantalla **Revisión de solicitud** (perfil Analista de crédito o Administrador). |
| Al aprobar se crea automáticamente la tarjeta | En la misma transacción se abre la cuenta de crédito y su primera tarjeta. |
| Número de tarjeta único generado por el sistema | 16 dígitos con dígito verificador Luhn; el número de cuenta es el de la primera tarjeta. |
| Límite igual al monto aprobado y saldo inicial en cero | Se guardan en la cuenta de crédito. |
| Consultar las tarjetas emitidas a un cliente (número, tipo, límite y saldo) | Detalle del cliente y pantalla **Tarjetas emitidas**. |
| Registrar consumos y pagos (monto, fecha y descripción); los consumos suman y los pagos restan al saldo | Detalle de la cuenta, con bloqueo pesimista para evitar saldos inconsistentes. |
| Un consumo no puede superar el límite de crédito | Se rechaza con el mensaje del disponible actual. |
| Consultar consumos y pagos | Historial de movimientos de la cuenta, con los reversos visibles. |
| Java y T-SQL | Spring Boot 3.3 (Java 17) y SQL Server 2022 con scripts en [`database/`](database/). |
| Entregables: código, documentación técnica, script de BD, diagrama E/R y README | Este repositorio, los manuales en PDF, los scripts T-SQL y el [diagrama entidad-relación](docs/diagrama-entidad-relacion.md). |
| Valor agregado: Docker Compose | `docker compose up -d --build` levanta base de datos, Keycloak, backend y frontend. |

## Reglas de negocio

Toda violación de una regla responde **HTTP 422** con un mensaje en español. La especificación completa está en [docs/REGLAS_CREDITO.md](docs/REGLAS_CREDITO.md).

### Solicitud y aprobación

| Regla | Descripción |
|---|---|
| RC-01 | Ingreso mensual mínimo por tipo: Clásica Q 3,000 · Oro Q 8,000 · Platino Q 15,000 · Black Q 30,000. |
| RC-02 / RC-04 | El cliente no puede tener cuentas en mora; se valida al solicitar y otra vez al aprobar. |
| RC-03 | El monto aprobado no puede superar el 150 % del ingreso mensual declarado. |
| RC-06 | Se requieren al menos 12 meses de estabilidad laboral. |
| RC-07 | Solo una cuenta activa por tipo de tarjeta; si perdió la tarjeta corresponde una reposición. |

### Ciclo de facturación, pago mínimo, intereses y mora

| Regla | Descripción |
|---|---|
| Corte y pago | Corte el día 5 de cada mes; fecha límite de pago el día 25. |
| Pago mínimo | 10 % del saldo al corte. El pago de contado cubre el saldo al corte completo. |
| Mora | Si al día 25 no se cubrió el pago mínimo, la cuenta entra en mora y no acepta consumos (RC-05). |
| Interés | El día 26 se cobra automáticamente un 5 % mensual sobre lo no pagado del corte; el proceso es idempotente (un interés por ciclo). |
| Ajustes | Recargo por mora, devolución de intereses, exoneración de mora y reversión de comisión, con motivo y usuario. |

### Tarjetas

| Regla | Descripción |
|---|---|
| RB-01 a RB-05 | Solo se bloquea una tarjeta activa; la tarjeta bloqueada no acepta consumos pero sí pagos. Por robo, extravío o sospecha de fraude **no se desbloquea**: solo se repone. |
| RR-01 a RR-06 | La reposición genera un número nuevo en la **misma cuenta** (mismo límite, saldo y movimientos); la anterior queda *Reemplazada*. Extravío cobra Q 45; robo exige denuncia y deterioro exige presentar la tarjeta. La mora no impide reponer. Si la tarjeta ya está bloqueada por robo, extravío o fraude, la reposición toma ese mismo motivo. |
| RNV-01 a RNV-04 | Una tarjeta vencida no acepta consumos y se renueva sin costo, con vencimiento a 5 años. |

### Reversos, cancelación y clientes

| Regla | Descripción |
|---|---|
| RV-01 a RV-05 | Un movimiento se reversa una sola vez, con motivo; no se borra, deja de contar en los cálculos. No se permite si el saldo quedaría negativo o superaría el límite. |
| CA-01 a CA-05 | Solo se cancela una cuenta activa, al día y con saldo Q 0.00. Se conserva todo el historial y el cliente queda activo sin producto. |
| CL-01 | No se puede eliminar un cliente con cuentas activas; primero deben cancelarse. |

## Arquitectura en breve

- **Frontend:** aplicación de una sola página (Vue 3) que inicia sesión contra Keycloak y consume la API con un token JWT.
- **Backend:** API REST en Spring Boot organizada en capas (controller, service, repository, entity) que valida el token y exige un permiso por operación.
- **Base de datos:** SQL Server 2022, creada por scripts T-SQL idempotentes que ejecuta un contenedor inicializador.
- **Identidad:** Keycloak con el realm `tarjetas`, 18 permisos agrupados en 4 perfiles y 5 usuarios de demostración.

Diagrama y detalle en el [manual técnico](docs/Manual_Tecnico.pdf) (sección 2).

## Pruebas

El backend incluye 195 pruebas automatizadas (JUnit 5, Mockito y Spring Security Test) que no requieren base de datos ni Keycloak:

```bash
cd backend && ./mvnw test
```

## Estructura del repositorio

```
sistema-tarjetas-credito/
├── README.md
├── docker-compose.yml       # Stack completo: base de datos, inicializador, Keycloak, backend y frontend
├── .env.example             # Variables de entorno de demostración
├── backend/                 # API REST Spring Boot (Java 17)
│   └── src/                 # Código (main) y pruebas (test)
├── frontend/                # Aplicación web Vue 3 + Vite
│   └── src/                 # Vistas, componentes, servicios y rutas
├── database/                # Scripts T-SQL: base de datos, esquema y datos semilla
├── keycloak/                # Realm con permisos, perfiles y usuarios de demostración
└── docs/                    # Manuales en PDF, reglas de negocio, requisitos y diagrama ER
```

## Documentación

| Documento | Contenido |
|---|---|
| [Manual técnico (PDF)](docs/Manual_Tecnico.pdf) | Arquitectura, instalación con y sin Docker, configuración, Keycloak, permisos, API REST, base de datos, pruebas, decisiones de diseño y solución de problemas |
| [Manual de usuario (PDF)](docs/Manual_Usuario.pdf) | Uso de la aplicación paso a paso para el personal del banco, con capturas |
| [Reglas de crédito](docs/REGLAS_CREDITO.md) | Especificación completa de las reglas de negocio |
| [Diagrama entidad-relación](docs/diagrama-entidad-relacion.md) | Modelo de datos, cardinalidades y cascadas |
| [Scripts de base de datos](database/) | `01_crear_base_datos.sql`, `02_crear_esquema.sql` y `03_datos_semilla.sql` |
| [Arquitectura](docs/ARQUITECTURA.md) | Cómo está armado el sistema y por qué |
