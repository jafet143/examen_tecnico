# Checklist de entrega

Se marca cada punto antes de enviar el enlace del repositorio. Última revisión: 2026-10-10. Incluye la Parte 2 de [REGLAS_CREDITO.md](REGLAS_CREDITO.md) (cuenta de crédito y tarjeta, número de cuenta = primera tarjeta, estabilidad laboral, una cuenta por tipo, reposición de tarjetas, reverso de movimientos y API por cuenta) la **Parte 3** (código de cliente, RB-04 vigente, permisos y 4 perfiles, 5 usuarios demo, datos sensibles por permiso, tarjeta visual sin número de cuenta con chip SVG, botón Regresar y página Sin acceso), la **Parte 4** (cargos y sobregiro, requisitos y costo de la reposición, renovación de tarjetas vencidas, cancelación de cuentas, ajustes sobre cargos y 4 permisos nuevos) y la **Parte 5** (pago mínimo del 10 %, mora contra el mínimo, interés automático del 5 % mensual y modal de pago con opciones). Todas prevalecen sobre [ARQUITECTURA.md](ARQUITECTURA.md), cuyo encabezado lo indica hasta la Parte 5.

## Repositorio
- [ ] Repositorio público en GitHub o GitLab con la URL verificada en una ventana de incógnito. **(Pendiente: lo realiza el usuario y comparte el enlace.)**
- [ ] La rama `main` contiene la versión final, con un historial de commits limpio y mensajes descriptivos. **(Pendiente: el proyecto aún no está en un repositorio Git.)**
- [x] El `.gitignore` incluye `.env`, `backend/target/`, `frontend/node_modules/`, `frontend/dist/`, `.idea/`, `*.iml`, `.vscode/`, `.DS_Store` y `*.log`.
- [x] El `.gitattributes` fija `*.sh` y `mvnw` con fin de línea LF.
- [x] No hay secretos reales en el repositorio, solo `.env.example` y las credenciales de demo.
- [x] Está el Maven Wrapper (`backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`), con permiso de ejecución en `mvnw`.
- [x] No quedan artefactos de compilación en el árbol (`backend/target/` se borró tras ejecutar las pruebas).

## README.md (resumen, lo primero que ve el evaluador)
- [x] Nombre y descripción breve de la solución (qué resuelve, en un párrafo).
- [x] Funcionalidades principales en viñetas de una línea por módulo.
- [x] Tecnologías con las versiones reales de `pom.xml`, `package.json` y `docker-compose.yml`.
- [x] Inicio rápido con Docker (requisitos, `cp .env.example .env`, `docker compose up -d --build`, `docker compose ps`) y aviso de `docker compose down -v` para volúmenes anteriores.
- [x] Tabla de URLs y de los 5 usuarios de demostración con su perfil.
- [x] Estructura del repositorio de primer y segundo nivel.
- [x] Sección "Documentación" con enlaces al manual técnico, al manual de usuario, a las reglas de crédito, al diagrama ER, a los scripts de `database/`, a los requisitos y a la arquitectura.

## Manual técnico (`docs/MANUAL_TECNICO.md`)
- [x] Visión general (cuenta frente a tarjeta, número de cuenta = primera tarjeta, código de cliente, acceso por permisos), tecnologías y versiones, y diagrama de arquitectura.
- [x] Estructura detallada del repositorio, requisitos previos, ejecución con Docker Compose (orden de arranque, tiempos, comandos útiles, volúmenes anteriores, Apple Silicon) y ejecución local sin Docker (infraestructura, `sqlcmd`, `./mvnw spring-boot:run`, `npm install && npm run dev`).
- [x] Configuración y variables de entorno (`.env`, backend y `VITE_*`).
- [x] Keycloak: realm, cliente `frontend-tarjetas`, roles, credenciales y cómo crear usuarios y asignar perfiles, con la advertencia de que los cambios en la consola se pierden al recrear el contenedor si no se agregan a `keycloak/realm-tarjetas.json`.
- [x] Seguridad: flujo PKCE, Resource Server, issuer frente a JWKS, los 18 permisos, la matriz perfil → permisos (ADMINISTRADOR 18, OPERADOR 9, CAJERO 3, ANALISTA_CREDITO 4), `denyAll` y datos sensibles decididos en el servidor.
- [x] API REST completa con el **permiso** y los códigos de cada endpoint, rutas eliminadas, respuestas principales, formato de errores y ejemplos curl.
- [x] Backend en capas, base de datos (scripts, diagrama ER embebido, integridad, cascadas, concurrencia, código de cliente, cargos y sobregiro).
- [x] Reglas de negocio resumidas (requisitos, RC, RR, RNV, CA, RB, RV, ciclo con ejemplo numérico, cargos y ajustes) con enlace a `REGLAS_CREDITO.md`, e implementación de crédito, ciclo, mora, proceso automático de intereses, bloqueo, reposición, renovación, cancelación, reversos, ajustes, catálogo y consultas en lote.
- [x] Frontend (vistas, componentes, composables, permisos en la interfaz).
- [x] Datos de demostración y recorrido sugerido en 17 pasos.
- [x] Pruebas: cómo ejecutarlas y qué cubren (**195** en 12 clases, contadas en `target/surefire-reports` el 2026-10-10).
- [x] Decisiones y supuestos, notas de seguridad y solución de problemas (Apple Silicon/Rosetta, `docker compose down -v`, Keycloak que no reimporta, puertos ocupados, IntelliJ y Lombok).
- [x] Reemplaza a `docs/DOCUMENTACION_TECNICA.md` (eliminado; su contenido está fusionado aquí).

## Manual de usuario (`docs/MANUAL_USUARIO.md`)
- [x] Lenguaje de negocio, sin términos técnicos.
- [x] Ingreso y cierre de sesión, perfiles y qué puede hacer cada uno, recorrido de la pantalla (barra lateral, barra superior, botón Regresar, menú "Más acciones").
- [x] Paso a paso de cada tarea: resumen, clientes, solicitudes (requisitos, aprobación hasta el 150 %, rechazo con motivo), cuentas y tarjetas, consumos y pagos, pago mínimo, intereses y mora con ejemplo numérico, bloqueo, reposición (requisitos y comisión de Q 45.00), renovación, reversos, recargos, devolución de intereses, exoneración de mora y cancelación.
- [x] Preguntas frecuentes y mensajes comunes con qué hacer.
- [x] 13 capturas de la interfaz en `docs/imagenes/manual-usuario/`.

## Base de datos
- [x] `database/01_crear_base_datos.sql`: BD `GestionTarjetasCredito` (`Modern_Spanish_CI_AS`) y login/usuario `usuario_tarjetas` (`db_datareader` + `db_datawriter`).
- [x] `database/02_crear_esquema.sql`: secuencia `dbo.seq_codigo_cliente` (desde 10000001) con `GRANT UPDATE` a `usuario_tarjetas`; tablas `cliente` (con `codigo_cliente CHAR(8)` único, solo dígitos), `solicitud_tarjeta` (con `nombre_empleador` y `fecha_inicio_laboral`), `cuenta_credito` (con `estado_cuenta` y datos de cancelación; CHECK `saldo_actual >= 0` sin tope superior), `cargo` (tres tipos, `ciclo_corte` solo en intereses, columnas de reverso), `tarjeta_credito` (plástico con estados ACTIVA/BLOQUEADA/REEMPLAZADA/CANCELADA, bloqueo, reposición con motivo `RENOVACION`, `denuncia_presentada`, `tarjeta_deteriorada_presentada` e `id_cargo_reposicion`), `consumo` (con `id_tarjeta`) y `pago`, con columnas de reverso; PK, FK, UQ, CK, DF e índices con nombre.
- [x] Índices únicos filtrados `ux_tarjeta_credito_vigente_por_cuenta` (una tarjeta vigente por cuenta), `ux_tarjeta_credito_anterior`, `ux_cuenta_credito_cliente_tipo_activa` (RC-07 solo con cuentas activas) y `ux_cargo_interes_por_ciclo` (un interés por ciclo).
- [x] Estrategia de FK sin *multiple cascade paths*: `cuenta_credito.id_cliente`, `consumo.id_tarjeta`, `tarjeta_credito.id_tarjeta_anterior` y `tarjeta_credito.id_cargo_reposicion` en NO ACTION; el resto en cascada (incluido `cargo.id_cuenta`).
- [x] `database/03_datos_semilla.sql`: 7 clientes (códigos 10000001 a 10000007; la secuencia continúa en 10000008), 9 solicitudes (6 aprobadas, 2 pendientes, 1 rechazada), 6 cuentas (5 activas y 1 cancelada), 7 tarjetas (1 reemplazada, 1 cancelada, 1 vencida; Luis bloqueada por Solicitud del cliente y Sofía por sospecha de fraude), 16 consumos (1 reversado), 9 pagos (10 entre el día 6 y el 25) y 2 recargos por mora, con fechas relativas a la fecha actual. **No inserta intereses:** los genera el backend al arrancar (3). Solo se cargan si `dbo.cliente` está vacía.
- [x] `database/inicializar-base-datos.sh` con LF y `set -euo pipefail`.
- [x] Los scripts son idempotentes sobre una BD nueva y se ejecutan sin errores en SQL Server 2022.
- [x] La eliminación de un cliente con varias cuentas, reposiciones y movimientos reversados se verificó contra la BD: no quedan cuentas, tarjetas, consumos ni pagos huérfanos.

## Diagrama entidad/relación
- [x] `docs/diagrama-entidad-relacion.md`: `erDiagram` de Mermaid con las 7 tablas (incluida `cargo`), sus columnas, PK/FK/UK, cardinalidades (incluida la autorrelación de reposición) y la explicación de las cascadas.
- [x] Coincide con `02_crear_esquema.sql` y está embebido en el manual técnico (sección 12).

## Otra documentación técnica
- [x] `docs/MANUAL_TECNICO.md` (ver la sección anterior), que consolida la antigua documentación técnica y el contenido técnico del README.
- [x] `docs/REGLAS_CREDITO.md` (Partes 1 a 5) y `docs/ARQUITECTURA.md` (con aviso de que su modelo, su API, su seguridad y su estado de pago fueron reemplazados por las Partes 2 a 5).
- [x] `docs/REQUISITOS.md` con RC-06, RC-07, RR-01 a RR-05, RV-01 a RV-05, RF de cuentas, reposición y reverso, y de la Parte 3: RF-CLI-05/06 (código de cliente), RB-04 vigente, RF-CIC-03 (saldo al corte nunca negativo), RF-SEG-01 a 05 (permisos y perfiles), RF-SEN-01/02 (datos sensibles) y RF-UI-01 a 03; sección 8b de las Partes 4 y 5 (RF-CRG-01 a 03, RF-REP-02/03, RF-REN-01 y RNV-01 a 04, RF-CAN-01 a 04 y CA-01 a 05, RF-AJU-01 a 03, RF-SEG-06, RF-PAG-01/02, RF-INT-01 a 03, RF-UI-04/05, RF-SEM-01) y filas afectadas actualizadas (RC-02, RC-05, RC-07, RN-10, RF-CIC-01/02, RF-CAT-01, RNF-11…). 131 de 132 en **Cumplido / Cumplido (verificado)**, salvo RE-01.
- [x] Swagger UI accesible (público) y funcionando con el token `bearerAuth`.

## Funcionalidad (prueba final en un clon limpio)

> Verificado por API (curl) con tokens reales de Keycloak sobre el stack en Docker, en el navegador y con `./mvnw test`. Se recomienda repetir el recorrido sugerido del manual técnico (sección 22.2) en un clon limpio después de publicar el repositorio.
> Los puntos de las Partes 3 a 5 están cubiertos por `./mvnw test` (autorización por permiso para los 4 perfiles y los 4 permisos nuevos, datos sensibles, código de cliente, RB-04, requisitos de reposición, renovación, cancelación, cargos y ajustes, pago mínimo, mora e intereses) y se verificaron con curl y tokens reales y en el navegador. Conviene repetir, con un `docker compose down -v` previo, el recorrido de los pasos 1 a 17 del manual técnico (sección 22.2) con los 5 usuarios.

- [x] Login con los 5 usuarios (`admin`, `supervisor`, `operador`, `cajero`, `analista`); la barra superior muestra el perfil; el token trae los 18 permisos para `admin`.
- [x] Cada perfil ve solo sus acciones: el operador no ve "Eliminar", "Aprobar/Rechazar", "Registrar pago", "Desbloquear", "Reversar", "Registrar cargo", "Exonerar mora", "Devolver" ni "Cancelar cuenta" (sí "Renovar tarjeta"); el cajero solo "Registrar pago" y no tiene el menú Solicitudes; el analista decide solicitudes pero no gestiona clientes ni opera cuentas. La API responde 403 a lo que el perfil no tiene.
- [x] Una ruta sin permiso (p. ej. `/solicitudes` con `cajero`, `/clientes/nuevo` con `analista`) muestra la página **Sin acceso**.
- [x] Con `admin`/`supervisor` aparece el ojo junto a los números y muestra el completo; con los demás perfiles los campos `…Completo` llegan en `null` y no hay ojo.
- [x] Código de cliente visible en clientes, cuentas, solicitudes y revisión; buscable; un cliente nuevo recibe el siguiente valor de la secuencia; no editable.
- [x] Botón Regresar en las páginas internas; no aparece en las de primer nivel.
- [x] Resumen operativo con indicadores de cuentas, solicitudes pendientes y actividad reciente.
- [x] Crear, listar (con buscador), editar y eliminar clientes. Identificación duplicada → 409. Menor de edad → 422.
- [x] Una solicitud nueva queda en PENDIENTE y guarda empleador y fecha de inicio laboral.
- [x] Menos de 12 meses de estabilidad laboral → 422 (RC-06); la solicitud de Andrea (semilla) solo se puede rechazar.
- [x] Ingreso bajo el mínimo del tipo → 422 (en la UI el tipo queda deshabilitado).
- [x] Cliente con cuentas en mora no puede solicitar (aviso en la UI y 422) ni recibir una cuenta al aprobar.
- [x] Cliente con cuenta del mismo tipo → 422 (RC-07); en la UI el tipo queda deshabilitado.
- [x] Monto aprobado mayor al 150 % del ingreso → 422; la revisión propone el máximo.
- [x] Al aprobar se abren la cuenta (límite = monto aprobado, saldo 0) y su primera tarjeta ACTIVA con el mismo número (16 dígitos, Luhn, prefijo por tipo).
- [x] La API y la UI muestran cuenta y tarjeta enmascaradas; los números completos solo con `DATOS_SENSIBLES_VER`.
- [x] Tarjeta visual con el color de su tipo (Clásica azul, Oro dorado, Platino plateado, Black negro), chip dorado en SVG y **sin** número de cuenta.
- [x] Al rechazar se exige el comentario; una solicitud ya decidida no se puede decidir de nuevo (422).
- [x] Consumos y pagos por cuenta: el consumo suma y se registra con la tarjeta vigente, el pago resta; consumo sobre el límite y pago mayor al saldo → 422.
- [x] Estado de pago y ciclo de facturación de la cuenta visibles con pago mínimo, pago de contado, lo pagado y la tasa; cuenta en mora (pagó menos del mínimo): el consumo responde 422 y un pago que completa el mínimo vencido la saca de la mora.
- [x] Modal de pago con opciones Monto en mora (solo en mora), Pago mínimo, Pago de contado y Otro monto; aviso amarillo si no cubre el mínimo y aviso del interés del 5 %.
- [x] Interés automático: al arrancar el backend se generan los 3 intereses de la semilla (Ana Clásica Q 90.00; Carlos Q 575.00 y Q 786.29) con usuario `sistema`; reiniciar no los duplica; devolver un interés no lo regenera.
- [x] Cargos: *Registrar cargo* solo ofrece recargo por mora; `INTERES` manual → 422; un cargo puede sobregirar el límite.
- [x] Ajustes (`CARGOS_AJUSTAR`): devolución de intereses, exoneración de un recargo o de todos (*Exonerar mora*) y reversión de comisión; la exoneración no saca de la mora si falta el mínimo.
- [x] Reposición con requisitos: robo sin denuncia y deterioro sin tarjeta → 422 (botón deshabilitado en la UI); extravío cobra Q 45.00 automáticamente; fraude sin costo.
- [x] Renovación (`TARJETAS_RENOVAR`): tarjeta vencida con insignia y aviso, consumo → 422 (RNV-04), renovación sin costo; no vencida o bloqueada → 422.
- [x] Cancelación (`CUENTAS_CANCELAR`): Luis (al día, saldo 0, tarjeta bloqueada) se cancela; Carlos no (requisitos en rojo, 422 por la API); la cuenta cancelada no admite operaciones (422) y conserva su historial; el cliente queda "Sin producto activo" y puede solicitar de nuevo el mismo tipo (Jorge).
- [x] Menú "Más acciones" en el detalle de cuenta, operable con teclado y filtrado por permiso y estado.
- [x] Bloqueo con `TARJETAS_BLOQUEAR` (ADMINISTRADOR y OPERADOR); desbloqueo con `TARJETAS_DESBLOQUEAR` (solo ADMINISTRADOR) y **solo** por Solicitud del cliente (Luis); robo, extravío y sospecha de fraude responden 422 y la UI ofrece solo la reposición (Sofía); tarjeta bloqueada no acepta consumos pero sí pagos.
- [x] Reposición (`TARJETAS_REPONER`: ADMINISTRADOR y OPERADOR): misma cuenta y saldo, tarjeta nueva ACTIVA con otro número, anterior REEMPLAZADA (y bloqueada si robo, extravío o fraude); una tarjeta reemplazada no se repone, bloquea ni desbloquea (422); la mora no la impide.
- [x] Reverso (`MOVIMIENTOS_REVERSAR`, solo ADMINISTRADOR): el movimiento queda marcado con motivo, fecha y usuario y deja de contar; ya reversado, saldo negativo o límite superado → 422.
- [x] Detalle de cuenta con historial de tarjetas (vigente, reemplazadas y cancelada) y de movimientos (consumos, pagos y cargos; tarjeta usada en cada consumo; reversados tachados como Reversado, Devuelto o Exonerado).
- [x] Las rutas eliminadas (`GET /gestion-tarjetas/tarjetas`, `/gestion-tarjetas/clientes/{id}/tarjetas`, `/gestion-tarjetas/tarjetas/{id}/consumos|pagos`) responden 403 con token (`denyAll`); `/tarjetas/:id` en el frontend redirige a la cuenta.
- [x] El saldo al corte nunca es negativo.
- [x] Eliminar un cliente elimina sus solicitudes, cuentas, tarjetas, movimientos y cargos.
- [x] `./mvnw test` pasa: **194 pruebas, 0 fallos** en 12 clases (ejecutado el 2026-10-10; `backend/target/` borrado después).

## Docker
- [x] `docker compose up -d --build` desde cero:
  - `base-datos`, `keycloak`, `backend` y `frontend` quedan en estado healthy o running.
  - `inicializador-base-datos` termina con código 0.
- [x] El realm `tarjetas` se importa automáticamente (`start-dev --import-realm`) con los 18 permisos, los 4 perfiles compuestos y los 5 usuarios demo.
- [x] `docker compose down -v` seguido de un nuevo `up` vuelve a funcionar, y un `up` sin `-v` no duplica los datos semilla.
- [x] Volumen de una versión anterior: no se migra (cambio de esquema incompatible: cuentas, código de cliente, cargos, cancelación y requisitos de reposición); el README y el manual técnico indican ejecutar `docker compose down -v`, que además recrea Keycloak con el realm nuevo.

## Credenciales de demo (documentadas en el README y en el manual técnico)
| Servicio | Usuario | Contraseña | Rol |
|----------|---------|------------|-----|
| App (Keycloak, realm `tarjetas`, cliente `frontend-tarjetas`) | `admin` | `admin123` | ADMINISTRADOR |
| App | `supervisor` | `supervisor123` | ADMINISTRADOR |
| App | `operador` | `operador123` | OPERADOR |
| App | `cajero` | `cajero123` | CAJERO |
| App | `analista` | `analista123` | ANALISTA_CREDITO |
| Consola de Keycloak (http://localhost:8180, realm `master`) | `admin` | `admin` (`KEYCLOAK_ADMIN_PASSWORD`) | (administración) |
| SQL Server (localhost:1433) | `sa` | `Tarjetas_Chn2026` (`MSSQL_SA_PASSWORD` en `.env`) | (administración) |
| SQL Server, usuario de la aplicación | `usuario_tarjetas` | `Aplicacion_Tarjetas2026` (`CLAVE_USUARIO_APLICACION`) | lectura y escritura de datos |

> Son valores solo para demostración. Si cambian, se actualizan en el README, en el manual técnico, en `.env.example` y en `keycloak/realm-tarjetas.json`.
