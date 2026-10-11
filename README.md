# Sistema de Gestión de Tarjetas de Crédito

Aplicación web para administrar el ciclo de vida de las tarjetas de crédito de un banco. Cubre desde el registro del cliente y la solicitud de la tarjeta, evaluada con reglas de crédito (ingreso mínimo, estabilidad laboral, mora y una cuenta activa por tipo), hasta la operación diaria de la **cuenta de crédito**: consumos y pagos, ciclo de facturación con pago mínimo del 10 %, mora, interés automático del 5 % mensual, bloqueo, reposición y renovación de tarjetas, cargos y sus ajustes, reversos y cancelación de cuentas. El acceso se controla con 18 permisos agrupados en cuatro perfiles (Administrador, Operador de atención, Cajero y Analista de crédito).

Desarrollada como examen técnico para la plaza de Analista Programador de **Crédito Hipotecario Nacional de Guatemala (CHN)**.

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

Requisitos: Docker Desktop 4.x (o Docker Engine con Compose v2), al menos 4 GB de RAM para Docker y los puertos `3000`, `8080`, `8180` y `1433` libres. En Mac con Apple Silicon active la emulación con Rosetta en Docker Desktop.

1. Clonar el repositorio:

```bash
git clone <url-del-repositorio> && cd sistema-tarjetas-credito
```

2. Crear el archivo de variables de entorno (valores de demostración):

```bash
cp .env.example .env
```

3. Construir y levantar todo el stack (el primer arranque tarda de 3 a 6 minutos):

```bash
docker compose up -d --build
```

4. Verificar que `backend` y `frontend` estén en ejecución:

```bash
docker compose ps
```

Si antes levantó una versión anterior del proyecto, ejecute `docker compose down -v` antes del paso 3. La ejecución local sin Docker, la configuración y la solución de problemas están en el [manual técnico](docs/MANUAL_TECNICO.md).

## URLs y usuarios de demostración

| Servicio | URL |
|---|---|
| Aplicación web | http://localhost:3000 |
| API REST | http://localhost:8080/gestion-tarjetas |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Consola de Keycloak | http://localhost:8180 (usuario `admin`, contraseña `admin`) |

| Usuario | Contraseña | Perfil | Qué puede hacer |
|---|---|---|---|
| `admin` | `admin123` | Administrador | Todo, incluido ver números completos |
| `supervisor` | `supervisor123` | Administrador | Igual que `admin` |
| `operador` | `operador123` | Operador de atención | Clientes, registrar solicitudes y consumos, bloquear, reponer y renovar tarjetas |
| `cajero` | `cajero123` | Cajero | Consultar clientes y cuentas y registrar pagos |
| `analista` | `analista123` | Analista de crédito | Consultar y aprobar o rechazar solicitudes |

La base de datos se crea con datos de demostración (7 clientes, 6 cuentas y casos de mora, tarjeta vencida, bloqueos y cuenta cancelada). El detalle y un recorrido sugerido están en el [manual técnico](docs/MANUAL_TECNICO.md#22-datos-de-demostración-y-recorrido-sugerido).

## Arquitectura en breve

- **Frontend:** aplicación de una sola página (Vue 3) que inicia sesión contra Keycloak y consume la API con un token JWT.
- **Backend:** API REST en Spring Boot organizada en capas (controller, service, repository, entity) que valida el token y exige un permiso por operación.
- **Base de datos:** SQL Server 2022, creada por scripts T-SQL idempotentes que ejecuta un contenedor inicializador.
- **Identidad:** Keycloak con el realm `tarjetas`, 18 permisos agrupados en 4 perfiles y 5 usuarios de demostración.

Diagrama y detalle en el [manual técnico](docs/MANUAL_TECNICO.md#2-arquitectura).

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
└── docs/                    # Manuales, reglas de negocio, diagrama ER y trazabilidad
    └── imagenes/            # Capturas del manual de usuario
```

## Documentación

| Documento | Contenido |
|---|---|
| [Manual técnico](docs/MANUAL_TECNICO.md) | Arquitectura, instalación con y sin Docker, configuración, Keycloak, permisos, API REST, base de datos, pruebas, decisiones de diseño y solución de problemas |
| [Manual de usuario](docs/MANUAL_USUARIO.md) | Uso de la aplicación paso a paso para el personal del banco, con capturas |
| [Reglas de crédito](docs/REGLAS_CREDITO.md) | Especificación completa de las reglas de negocio |
| [Diagrama entidad-relación](docs/diagrama-entidad-relacion.md) | Modelo de datos, cardinalidades y cascadas |
| [Scripts de base de datos](database/) | `01_crear_base_datos.sql`, `02_crear_esquema.sql` y `03_datos_semilla.sql` |
| [Requisitos](docs/REQUISITOS.md) | Matriz de trazabilidad de requisitos con evidencia |
| [Arquitectura](docs/ARQUITECTURA.md) | Contrato técnico original del proyecto |
