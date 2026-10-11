# Reglas de crédito, ciclo de facturación y mora

Este documento amplía el contrato de [ARQUITECTURA.md](ARQUITECTURA.md). Donde los dos documentos no coincidan, manda este.

## 1. Tipos de tarjeta

| Código (BD / enum / JSON) | Descripción | Ingreso mensual mínimo | Prefijo del número | Color de la tarjeta en la UI |
|---|---|---|---|---|
| `CLASICA` | Clásica | Q 3,000.00 | 4100 | Azul institucional (el actual) |
| `ORO` | Oro | Q 8,000.00 | 4200 | Dorado |
| `PLATINO` | Platino | Q 15,000.00 | 4300 | Plateado |
| `BLACK` | Black | Q 30,000.00 | 4400 | Negro |

- El enum `TipoTarjeta` concentra el código, la descripción, el ingreso mínimo y el prefijo. No se repiten estos valores en otras clases.
- El CHECK de la columna `tipo_tarjeta` de `solicitud_tarjeta` y `tarjeta_credito` acepta los cuatro códigos.

## 2. Reglas al registrar una solicitud (`POST /gestion-tarjetas/solicitudes`)

Se validan en este orden. Si alguna falla, la API responde **422** con el mensaje indicado.

| # | Regla | Mensaje |
|---|---|---|
| RC-01 | El ingreso mensual declarado debe ser mayor o igual al mínimo del tipo. | `El ingreso mensual de Q {ingreso} no alcanza el mínimo de Q {minimo} requerido para la tarjeta {tipo}` |
| RC-02 | El cliente no puede tener ninguna tarjeta **en mora**. | `El cliente tiene cuentas en mora. Debe ponerse al día en sus pagos antes de solicitar una nueva tarjeta` |
| (existente) | No puede haber otra solicitud PENDIENTE del mismo tipo para el cliente. | (sin cambios) |

## 3. Regla al aprobar una solicitud (`PUT /gestion-tarjetas/solicitudes/{id}/aprobar`)

| # | Regla | Mensaje |
|---|---|---|
| RC-03 | El monto aprobado no puede superar el **150 %** del ingreso mensual declarado (`ingresoMensual × 1.5`). | `El monto aprobado de Q {monto} supera el máximo de Q {maximo} (150 % del ingreso mensual declarado)` |
| RC-04 | Al aprobar se vuelve a validar RC-02, por si el cliente cayó en mora mientras la solicitud estaba pendiente. | Mismo mensaje de RC-02 |

- El monto sugerido en la pantalla de revisión es el máximo permitido (`ingresoMensual × 1.5`).
- El factor 1.5 vive en una sola constante del backend: `PORCENTAJE_MAXIMO_LIMITE_SOBRE_INGRESO`. El catálogo de la sección 6 lo expone para que el frontend no lo duplique.

## 4. Ciclo de facturación

- **Fecha de corte:** día **5** de cada mes, al final del día (23:59:59).
- **Fecha límite de pago:** día **25** del mismo mes que el corte, al final del día.
- **Saldo al corte** de un ciclo: la suma de consumos (y cargos, sección 21) menos la suma de pagos con fecha menor o igual al corte, sin contar movimientos reversados. **Nunca es negativo:** si el resultado da negativo, se toma como 0, porque el modelo no maneja saldo a favor.
- **Pago requerido** del ciclo: el saldo al corte completo. Para estar al día se debe pagar todo el saldo al corte antes del día 25.
- Los días 5 y 25 viven en constantes de una única clase, `CicloFacturacion`, que también calcula las fechas de corte y de pago.
- No hay procesos programados. El estado se calcula al consultar, con un `java.time.Clock` inyectado como bean para poder probarlo.

## 5. Estado de pago de una tarjeta (calculado, no se guarda en BD)

Con `hoy` igual a la fecha y hora actual en la zona America/Guatemala:

1. **Último ciclo vencido:** es el corte `C` más reciente cuya fecha límite de pago `P` ya pasó (`hoy` es posterior al día 25 a las 23:59:59 del mes de `C`).
   - Se calcula `pendienteVencido = saldoAlCorte(C) − pagos con fecha posterior a C hasta hoy`.
   - Si `pendienteVencido > 0`, el estado es **`EN_MORA`**, con `montoEnMora = pendienteVencido` y `diasMora` igual a los días transcurridos desde `P`.
2. Si no hay mora, se toma el **ciclo actual**: el corte `C2` más reciente con `C2 ≤ hoy`.
   - Se calcula `pagoPendiente = max(0, saldoAlCorte(C2) − pagos posteriores a C2)`.
   - Si `pagoPendiente > 0` y `hoy` aún no pasa la fecha límite de C2, el estado es **`PAGO_PENDIENTE`**.
   - En cualquier otro caso el estado es **`AL_DIA`**.
3. Los cortes anteriores a la fecha de emisión de la tarjeta no cuentan; su saldo al corte es 0.

Consecuencias de la mora:
- **RC-05:** una tarjeta en mora **no puede registrar consumos** y responde 422 con `La cuenta está en mora por Q {monto}. Registre un pago para ponerse al día antes de realizar consumos`.
- Los pagos siempre se permiten. Al cubrir el monto en mora, la tarjeta vuelve a estar al día.
- Con RC-02, el cliente no puede solicitar ni recibir una tarjeta nueva mientras tenga alguna en mora.

## 6. Cambios en la API

### 6.1 `TarjetaRespuesta`: campos nuevos

| Campo | Tipo | Descripción |
|---|---|---|
| `estadoPago` | `"AL_DIA"` \| `"PAGO_PENDIENTE"` \| `"EN_MORA"` | Estado calculado según la sección 5. |
| `descripcionEstadoPago` | string | "Al día", "Pago pendiente", "En mora". |
| `fechaUltimoCorte` | `LocalDate` (ISO) | Corte del ciclo actual (C2). |
| `fechaLimitePago` | `LocalDate` | Día 25 del mes de `fechaUltimoCorte`. |
| `fechaProximoCorte` | `LocalDate` | Siguiente día 5. |
| `saldoAlCorte` | BigDecimal | Saldo al corte del ciclo actual. |
| `pagoPendiente` | BigDecimal | Lo que falta pagar del ciclo actual (0 si está al día). |
| `montoEnMora` | BigDecimal | 0 si no está en mora. |
| `diasMora` | int | 0 si no está en mora. |

### 6.2 Nuevo endpoint: `GET /gestion-tarjetas/catalogos/tipos-tarjeta`

- Acceso: ADMINISTRADOR y OPERADOR.
- Respuesta:

```json
{
  "porcentajeMaximoLimiteSobreIngreso": 150,
  "diaCorte": 5,
  "diaLimitePago": 25,
  "tiposTarjeta": [
    { "codigo": "CLASICA", "descripcion": "Clásica", "ingresoMinimo": 3000.00 },
    { "codigo": "ORO", "descripcion": "Oro", "ingresoMinimo": 8000.00 },
    { "codigo": "PLATINO", "descripcion": "Platino", "ingresoMinimo": 15000.00 },
    { "codigo": "BLACK", "descripcion": "Black", "ingresoMinimo": 30000.00 }
  ]
}
```

### 6.3 Nuevo campo en `ClienteRespuesta`

| Campo | Tipo | Descripción |
|---|---|---|
| `tieneTarjetasEnMora` | boolean | Para avisar en la UI antes de crear una solicitud. |

En el listado de clientes se puede calcular por cliente, porque los datos son pocos. Si se calcula, hay que evitar consultas N+1 evidentes.

## 7. Datos semilla

- Las fechas de movimientos y emisiones se calculan en T-SQL **relativas a `GETDATE()`**. Así la demostración se ve igual sin importar el día en que se ejecute:
  - Una tarjeta **al día**: tenía saldo al último corte vencido y lo pagó completo antes de su día 25.
  - Una tarjeta **en mora**: tenía saldo al último corte vencido y no lo pagó. Su titular **no puede solicitar** otra tarjeta.
  - Una tarjeta **sin saldo**.
- Los ingresos y límites de la semilla cumplen RC-01 y RC-03.
- Debe haber al menos una solicitud **pendiente** que se pueda aprobar sin violar ninguna regla, y otra que el administrador deba rechazar o que exceda el 150 %.

## 8. Bloqueo de tarjetas

### 8.1 Estado de la tarjeta (se guarda en BD)

| Código | Descripción |
|---|---|
| `ACTIVA` | Opera con normalidad (estado inicial al emitirse). |
| `BLOQUEADA` | No acepta consumos. Sí acepta pagos. |

Motivos de bloqueo (enum `MotivoBloqueo`): `ROBO` (Robo), `EXTRAVIO` (Extravío), `SOSPECHA_FRAUDE` (Sospecha de fraude), `SOLICITUD_CLIENTE` (Solicitud del cliente).

Columnas nuevas en `dbo.tarjeta_credito`:

| Columna | Tipo | Regla |
|---|---|---|
| `estado_tarjeta` | `VARCHAR(10) NOT NULL DEFAULT 'ACTIVA'` | CHECK con `ACTIVA` o `BLOQUEADA` |
| `motivo_bloqueo` | `VARCHAR(20) NULL` | CHECK con los 4 motivos |
| `comentario_bloqueo` | `VARCHAR(250) NULL` | |
| `fecha_bloqueo` | `DATETIME2(0) NULL` | |
| `usuario_bloqueo` | `VARCHAR(100) NULL` | |

- Un CHECK de coherencia exige que, si el estado es `BLOQUEADA`, el motivo y la fecha no sean nulos, y que, si es `ACTIVA`, el motivo sea nulo.
- *(Superado por la sección 9.5: el esquema actual requiere `docker compose down -v` si se tenía un volumen anterior.)*

### 8.2 Reglas

| # | Regla | Mensaje (422) |
|---|---|---|
| RB-01 | Solo se bloquea una tarjeta `ACTIVA`. | `La tarjeta ya se encuentra bloqueada` |
| RB-02 | Solo se desbloquea una tarjeta `BLOQUEADA`. | `La tarjeta no está bloqueada` |
| RB-03 | Una tarjeta bloqueada no acepta consumos. Se valida antes que la mora (RC-05). | `La tarjeta está bloqueada por {motivo}. No se pueden registrar consumos` |
| RB-04 | Las tarjetas bloqueadas por `ROBO` o `EXTRAVIO` **no se pueden desbloquear** (en la práctica se repone el plástico). | `Una tarjeta bloqueada por {motivo} no puede desbloquearse` |
| RB-05 | Los pagos se permiten aunque la tarjeta esté bloqueada. | |

- **Permisos:** bloquear lo pueden hacer ADMINISTRADOR y OPERADOR (atención al cliente por robo o extravío). Desbloquear solo ADMINISTRADOR.
- El bloqueo **no** afecta el estado de pago (sección 5). Una tarjeta puede estar bloqueada y en mora a la vez.
- Al desbloquear se limpian motivo, comentario, fecha y usuario del bloqueo.

### 8.3 API

| Método | Ruta | Cuerpo | Respuesta | Roles |
|---|---|---|---|---|
| PUT | `/gestion-tarjetas/tarjetas/{idTarjeta}/bloquear` | `BloqueoTarjetaPeticion { motivoBloqueo (obligatorio, enum), comentario (opcional, máx. 250) }` | 200 `TarjetaRespuesta` | ADMINISTRADOR, OPERADOR |
| PUT | `/gestion-tarjetas/tarjetas/{idTarjeta}/desbloquear` | sin cuerpo | 200 `TarjetaRespuesta` | ADMINISTRADOR |

Campos nuevos en `TarjetaRespuesta`:
- `estadoTarjeta`: `"ACTIVA"` o `"BLOQUEADA"`.
- `descripcionEstadoTarjeta`: "Activa" o "Bloqueada".
- `motivoBloqueo`: código o `null`.
- `descripcionMotivoBloqueo`: descripción o `null`.
- `comentarioBloqueo`, `fechaBloqueo` (LocalDateTime) y `usuarioBloqueo`.
- `puedeDesbloquearse`: boolean. Es `false` si la tarjeta está activa o si está bloqueada por robo o extravío.

El catálogo `GET /gestion-tarjetas/catalogos/tipos-tarjeta` agrega `"motivosBloqueo": [{ "codigo": "ROBO", "descripcion": "Robo", "permiteDesbloqueo": false }, ...]`.

### 8.4 Datos semilla

Debe haber al menos una tarjeta `BLOQUEADA` por `SOSPECHA_FRAUDE`, que se pueda desbloquear. Lo ideal es que sea distinta de la tarjeta en mora.

---

# Parte 2 · Cuentas, reposiciones, estabilidad laboral y reversos

> Esta parte **reemplaza** lo que contradiga a las secciones anteriores. El cambio central es que **el crédito vive en la cuenta y la tarjeta es solo el plástico** anclado a esa cuenta.

## 9. Modelo: cuenta de crédito y tarjeta (plástico)

```
cliente 1───* solicitud_tarjeta 1───0..1 cuenta_credito 1───* tarjeta_credito
                                             │                    │ (id_tarjeta_anterior → tarjeta_credito)
                                             ├──* consumo ────────┘ (cada consumo guarda con qué tarjeta se hizo)
                                             └──* pago
```

### 9.1 `dbo.cuenta_credito` (nueva tabla: aquí viven el límite, el saldo, el ciclo y la mora)

| Columna | Tipo | Regla |
|---|---|---|
| `id_cuenta` | `BIGINT IDENTITY` | PK |
| `numero_cuenta` | `CHAR(16) NOT NULL` | UNIQUE. **Es igual al número de la primera tarjeta** emitida para la cuenta: 16 dígitos con prefijo por tipo y Luhn. No hay un generador aparte. Al aprobar se genera un número único de tarjeta y se usa como número de cuenta y como número de la primera tarjeta. Las reposiciones generan números de tarjeta nuevos, pero la cuenta conserva el original. |
| `id_cliente` | `BIGINT NOT NULL` | FK a cliente, `NO ACTION` (se borra desde el servicio). |
| `id_solicitud` | `BIGINT NOT NULL` | FK a solicitud_tarjeta, `UNIQUE`, `ON DELETE CASCADE` (la solicitud aprobada origina la cuenta). |
| `tipo_tarjeta` | `VARCHAR(10)` | CHECK con los 4 tipos. |
| `limite_credito` | `DECIMAL(12,2)` | > 0 |
| `saldo_actual` | `DECIMAL(12,2)` | Entre 0 y el límite (CHECK). |
| `fecha_apertura` | `DATETIME2(0)` | |

### 9.2 `dbo.tarjeta_credito` (el plástico; se rehace la tabla)

| Columna | Tipo | Regla |
|---|---|---|
| `id_tarjeta` | `BIGINT IDENTITY` | PK |
| `numero_tarjeta` | `CHAR(16)` | UNIQUE, prefijo según el tipo de la cuenta, Luhn (como ya se hace). |
| `id_cuenta` | `BIGINT NOT NULL` | FK a cuenta_credito, `ON DELETE CASCADE`. |
| `estado_tarjeta` | `VARCHAR(12)` | `ACTIVA`, `BLOQUEADA` o `REEMPLAZADA`. |
| `fecha_emision` | `DATETIME2(0)` | |
| `fecha_vencimiento` | `DATE` | Fin del mes de emisión + 5 años. Solo informativo. |
| Columnas de bloqueo | | Se mantienen las de la sección 8: motivo, comentario, fecha y usuario. |
| `id_tarjeta_anterior` | `BIGINT NULL` | FK a sí misma, `NO ACTION`. Apunta a la tarjeta que esta reemplaza. |
| `motivo_reposicion` | `VARCHAR(20) NULL` | Motivo por el que **esta** tarjeta fue reemplazada (se llena al reponer). |
| `fecha_reposicion` / `usuario_reposicion` | | Cuándo y quién la reemplazó. |

- **Tarjeta vigente:** cada cuenta tiene **exactamente una** tarjeta no `REEMPLAZADA`, que puede estar `ACTIVA` o `BLOQUEADA`. Lo garantiza un índice único filtrado: `UNIQUE (id_cuenta) WHERE estado_tarjeta <> 'REEMPLAZADA'`.
- **Una cuenta por tipo:** un cliente no puede tener dos cuentas del mismo tipo (RC-07).

### 9.3 Consumos y pagos

- `consumo` y `pago` referencian `id_cuenta` (FK con `ON DELETE CASCADE`).
- `consumo` además guarda `id_tarjeta` (FK `NO ACTION`): la tarjeta vigente con la que se hizo el consumo.
- Columnas de reverso en ambas tablas: `reversado BIT NOT NULL DEFAULT 0`, `motivo_reverso VARCHAR(250) NULL`, `fecha_reverso DATETIME2(0) NULL`, `usuario_reverso VARCHAR(100) NULL`. Un CHECK de coherencia exige que, si `reversado = 1`, el motivo y la fecha no sean nulos.
- **Los movimientos reversados no cuentan** para el saldo, el saldo al corte, la mora ni los totales.

### 9.4 Eliminación de un cliente

- El servicio borra primero las cuentas del cliente; la cascada se lleva tarjetas, consumos y pagos. Después borra el cliente; la cascada se lleva sus solicitudes.
- Las FK `NO ACTION` (`consumo.id_tarjeta` y `tarjeta.id_tarjeta_anterior`) no deben impedir el borrado. Si SQL Server lo impide, el servicio borra explícitamente en el orden correcto. Se prueba de verdad contra la BD.

### 9.5 Migración

Es un cambio de esquema incompatible. Los scripts siguen siendo idempotentes para una BD **nueva**. El README y el [manual técnico](Manual_Tecnico.pdf) (sección 5) indican que, si se tenía un volumen anterior, hay que ejecutar `docker compose down -v`. El evaluador clona el repositorio desde cero, así que no le afecta.

## 10. Requisitos para obtener una tarjeta

### 10.1 Campos nuevos en la solicitud (se guardan en `solicitud_tarjeta`)

| Campo JSON / columna | Tipo | Validación |
|---|---|---|
| `nombreEmpleador` / `nombre_empleador` | `VARCHAR(150) NOT NULL` | Obligatorio, de 2 a 150 caracteres. |
| `fechaInicioLaboral` / `fecha_inicio_laboral` | `DATE NOT NULL` | Obligatoria, en el pasado o presente (400 si es futura). |

### 10.2 Reglas de la solicitud (orden de validación, 422)

| # | Regla | Mensaje |
|---|---|---|
| RC-06 | **Estabilidad laboral:** al menos **12 meses** entre `fechaInicioLaboral` y la fecha de la solicitud. | `Se requiere al menos 1 año de estabilidad laboral. Antigüedad actual: {n} meses` |
| RC-01 | Ingreso mínimo según el tipo (sin cambios). | |
| RC-02 | El cliente no debe tener ninguna **cuenta** en mora. | `El cliente tiene cuentas en mora. Debe ponerse al día en sus pagos antes de solicitar una nueva tarjeta` |
| RC-07 | El cliente no puede tener ya una **cuenta** del mismo tipo. Si perdió la tarjeta, lo que corresponde es una reposición. | `El cliente ya tiene una cuenta {tipo} (número {numeroCuenta}). Si perdió o dañó la tarjeta, solicite una reposición` |
| (existente) | No puede haber otra solicitud pendiente del mismo tipo. | |

- **Al aprobar** se vuelven a validar RC-06, RC-02, RC-03 y RC-07. RC-06 se calcula a la fecha de la solicitud, por lo que la solicitud de la semilla con menos de 12 meses no se puede aprobar; solo rechazar.
- La aprobación crea una **cuenta nueva y su primera tarjeta**. Una categoría nueva significa siempre una cuenta nueva y una tarjeta nueva.
- El catálogo agrega `"mesesMinimosEstabilidadLaboral": 12`.
- `SolicitudRespuesta` agrega:
  - `nombreEmpleador`, `fechaInicioLaboral` y `mesesAntiguedadLaboral`, este último calculado a la fecha de la solicitud.
  - `idCuenta` y `numeroCuenta`, cuando la solicitud está aprobada, en lugar de solo `idTarjeta`. `idTarjeta` y `numeroTarjetaEnmascarado` se conservan con la **primera** tarjeta.

## 11. Reposición de tarjetas

`POST /gestion-tarjetas/tarjetas/{idTarjeta}/reposiciones`. Cuerpo `ReposicionTarjetaPeticion { motivoReposicion, comentario? }`. Responde **201** con la `TarjetaRespuesta` de la **nueva** tarjeta. Lo pueden hacer ADMINISTRADOR y OPERADOR.

Motivos de reposición (enum `MotivoReposicion`): `ROBO` (Robo), `EXTRAVIO` (Extravío), `DETERIORO` (Deterioro), `SOSPECHA_FRAUDE` (Sospecha de fraude).

| # | Regla | Mensaje (422) |
|---|---|---|
| RR-01 | Solo se repone la tarjeta **vigente**, que no puede estar `REEMPLAZADA`. | `La tarjeta ya fue reemplazada por la tarjeta terminada en {ultimos4}` |
| RR-02 | Si el motivo es ROBO, EXTRAVIO o SOSPECHA_FRAUDE y la tarjeta está `ACTIVA`, en la **misma transacción** se registra primero su bloqueo con ese motivo (fecha y usuario) y luego se reemplaza. | |
| RR-03 | La tarjeta anterior pasa a `REEMPLAZADA` con `motivo_reposicion`, fecha y usuario. Conserva su información de bloqueo como historial. | |
| RR-04 | La tarjeta nueva queda `ACTIVA`, en la **misma cuenta**: mismo número de cuenta, límite, saldo, ciclo, mora y movimientos. Tiene número nuevo único, nuevas fechas de emisión y vencimiento, e `id_tarjeta_anterior` apunta a la reemplazada. | |
| RR-05 | La mora **no impide** una reposición. | |

- Una tarjeta `REEMPLAZADA` no se puede bloquear, desbloquear, reponer ni usar para consumos. Responde 422 con el mensaje de RR-01.
- Desbloquear sigue prohibido para ROBO y EXTRAVIO (RB-04). El camino correcto en esos casos es la reposición.

## 12. Reverso de transacciones

| Método | Ruta | Cuerpo | Respuesta | Roles |
|---|---|---|---|---|
| PUT | `/gestion-tarjetas/consumos/{idConsumo}/reversar` | `ReversoPeticion { motivo }` (obligatorio, 5 a 250 caracteres) | 200 `MovimientoRespuesta` | **Solo ADMINISTRADOR** |
| PUT | `/gestion-tarjetas/pagos/{idPago}/reversar` | igual | 200 | **Solo ADMINISTRADOR** |

| # | Regla | Mensaje (422) |
|---|---|---|
| RV-01 | No se puede reversar un movimiento ya reversado. | `El movimiento ya fue reversado el {fecha}` |
| RV-02 | **Reversar un consumo** resta su monto del saldo. No se permite si el saldo quedaría negativo, porque significa que hubo pagos posteriores que lo cubrieron. | `No se puede reversar el consumo: el saldo quedaría en Q {saldo}. Reverse primero el pago que lo cubrió` |
| RV-03 | **Reversar un pago** suma su monto al saldo. No se permite si el saldo superaría el límite. | `No se puede reversar el pago: el saldo de Q {saldo} superaría el límite de crédito de Q {limite}` |
| RV-04 | El reverso se ejecuta con bloqueo pesimista de la **cuenta**, igual que los consumos y pagos. | |
| RV-05 | El movimiento **no se borra**: queda marcado como reversado con motivo, fecha y usuario, y deja de contar en todos los cálculos. | |

- Se permite reversar aunque la tarjeta esté bloqueada o reemplazada, o la cuenta esté en mora, porque es una corrección administrativa.
- `MovimientoRespuesta` agrega:
  - `reversado`, `motivoReverso`, `fechaReverso` y `usuarioReverso`.
  - En consumos, también `idTarjeta` y `numeroTarjetaEnmascarado` de la tarjeta usada.

## 13. API reorganizada (cuenta como recurso principal)

| Método | Ruta | Descripción | Roles |
|---|---|---|---|
| GET | `/gestion-tarjetas/cuentas` | Todas las cuentas, con su tarjeta vigente y estado de pago (reemplaza a `GET /gestion-tarjetas/tarjetas`) | A, O |
| GET | `/gestion-tarjetas/cuentas/{idCuenta}` | Detalle de la cuenta | A, O |
| GET | `/gestion-tarjetas/cuentas/{idCuenta}/tarjetas` | Historial de plásticos de la cuenta, del más reciente al más antiguo | A, O |
| GET | `/gestion-tarjetas/cuentas/{idCuenta}/movimientos` | Consumos y pagos **unificados**, con `tipoMovimiento` CONSUMO o PAGO, ordenados por fecha descendente, incluidos los reversados | A, O |
| POST | `/gestion-tarjetas/cuentas/{idCuenta}/consumos` | Registra un consumo con la tarjeta vigente. Valida RB-03 (vigente bloqueada), RC-05 (mora) y el límite | A, O |
| POST | `/gestion-tarjetas/cuentas/{idCuenta}/pagos` | Registra un pago | A, O |
| GET | `/gestion-tarjetas/clientes/{idCliente}/cuentas` | Cuentas del cliente (reemplaza a `/clientes/{id}/tarjetas`) | A, O |
| GET | `/gestion-tarjetas/tarjetas/{idTarjeta}` | Una tarjeta con su `idCuenta` | A, O |
| PUT | `/gestion-tarjetas/tarjetas/{idTarjeta}/bloquear` · `/desbloquear` | Como en la sección 8, solo para la tarjeta vigente | igual |
| POST | `/gestion-tarjetas/tarjetas/{idTarjeta}/reposiciones` | Sección 11 | A, O |
| PUT | `/gestion-tarjetas/consumos/{id}/reversar` · `/gestion-tarjetas/pagos/{id}/reversar` | Sección 12 | **A** |

Se eliminan:
- `GET /gestion-tarjetas/tarjetas`.
- `/gestion-tarjetas/clientes/{id}/tarjetas`.
- `/gestion-tarjetas/tarjetas/{id}/consumos` y `/gestion-tarjetas/tarjetas/{id}/pagos`.

**`CuentaRespuesta`** tiene:
- `id`, `numeroCuentaEnmascarado` (ver sección 15), `idCliente`, `nombreCliente`, `idSolicitud`.
- `tipoTarjeta`, `descripcionTipoTarjeta`, `limiteCredito`, `saldoActual`, `creditoDisponible`, `fechaApertura`.
- Todos los campos de estado de pago de la sección 6.1, que ahora son de la **cuenta**.
- `tarjetaVigente` (una `TarjetaRespuesta`) y `cantidadReposiciones`.

**`TarjetaRespuesta`** tiene:
- `id`, `idCuenta`, `numeroCuentaEnmascarado`, `numeroTarjetaEnmascarado`.
- `tipoTarjeta`, `descripcionTipoTarjeta`, `nombreCliente`, `fechaEmision`, `fechaVencimiento`.
- `estadoTarjeta`, `descripcionEstadoTarjeta` y todos los campos de bloqueo de la sección 8.
- `idTarjetaAnterior`, `motivoReposicion`, `descripcionMotivoReposicion`, `comentarioReposicion`, `fechaReposicion`, `usuarioReposicion` y `puedeDesbloquearse`.

`ClienteRespuesta.tieneTarjetasEnMora` pasa a llamarse **`tieneCuentasEnMora`**.

El catálogo agrega `motivosReposicion` (con `codigo` y `descripcion`) y `mesesMinimosEstabilidadLaboral`.

## 14. Datos semilla (reemplaza la sección 7 y la 8.4)

Las fechas siguen siendo relativas a hoy en Guatemala. Escenarios obligatorios:
- **Ana Lucía:** cuenta ORO al día con **una reposición** por robo. La tarjeta original está REEMPLAZADA y bloqueada por robo, y la nueva está ACTIVA. Algunos consumos se hicieron con la tarjeta vieja y otros con la nueva.
- **Ana Lucía:** también tiene una **segunda cuenta** CLASICA, para mostrar "nueva categoría, nueva cuenta".
- **Carlos:** cuenta PLATINO **en mora**.
- **Sofía:** cuenta BLACK con la tarjeta vigente **bloqueada por sospecha de fraude**, que se puede desbloquear.
- **Luis:** cuenta CLASICA sin saldo.
- **Reverso:** al menos un consumo **reversado** (motivo: "Cargo duplicado por el comercio") en alguna cuenta. El saldo es coherente: la suma de consumos menos pagos, sin contar los reversados.
- **Solicitudes pendientes:**
  - Una aprobable que cumple todo, con más de 12 meses de antigüedad laboral.
  - Una con **menos de 12 meses** de antigüedad. Como RC-06 impide crearla por la API, se inserta directamente en la semilla para que el administrador vea el aviso y la rechace.
- Todas las solicitudes de la semilla tienen empleador y fecha de inicio laboral.

## 15. Aclaración: número de cuenta = número de la primera tarjeta

- `numeroCuenta` tiene 16 dígitos y es el mismo número de la primera tarjeta de esa cuenta.
- La unicidad del número nuevo se verifica contra `tarjeta_credito.numero_tarjeta`. Como toda cuenta tiene su primera tarjeta con ese mismo número, eso basta.
- En la UI, el número de cuenta se muestra **enmascarado igual que la tarjeta** (`•••• •••• •••• 1234`). En `CuentaRespuesta` se agrega `numeroCuentaEnmascarado`, y `numeroCuenta` se conserva completo solo si el backend lo necesita para los mensajes. Los mensajes de error usan el enmascarado (por ejemplo, RC-07: `... (número **** **** **** 1234) ...`).
- Por coherencia con la decisión de no exponer números completos, `CuentaRespuesta` **no** expone `numeroCuenta` completo, solo `numeroCuentaEnmascarado`. Lo mismo aplica a `TarjetaRespuesta.numeroCuenta`, que pasa a ser `numeroCuentaEnmascarado`, y a `SolicitudRespuesta.numeroCuenta`, que también pasa a `numeroCuentaEnmascarado`.
- Después de una reposición, la tarjeta vigente tiene un número distinto al de la cuenta. La UI muestra los dos: "Cuenta •••• 1234 · Tarjeta vigente •••• 5678".

---

# Parte 3 · Código de cliente, permisos por rol, datos sensibles y ajustes de UI

> Esta parte **reemplaza** lo que contradiga a las anteriores.

## 16. Código de cliente

- Columna nueva `dbo.cliente.codigo_cliente CHAR(8) NOT NULL UNIQUE`, solo dígitos.
- Se genera con una secuencia de SQL Server: `CREATE SEQUENCE dbo.seq_codigo_cliente AS BIGINT START WITH 10000001 INCREMENT BY 1`.
- El servicio obtiene el siguiente valor con una consulta nativa en el repositorio (`SELECT NEXT VALUE FOR dbo.seq_codigo_cliente`) antes de insertar.
- Una vez asignado **no se edita**.
- Los clientes de la semilla tienen códigos consecutivos desde `10000001`, y la secuencia continúa después del último.
- **El producto se relaciona con el código de cliente:**
  - `ClienteRespuesta`, `CuentaRespuesta`, `TarjetaRespuesta` y `SolicitudRespuesta` agregan `codigoCliente`.
  - La UI muestra el código en la lista de clientes, el detalle del cliente, las cuentas, las solicitudes y la revisión.
  - Las búsquedas de clientes, cuentas y solicitudes aceptan el código de cliente.

## 17. Bloqueo: solo "Solicitud del cliente" se puede desbloquear

**RB-04 (reemplaza la versión anterior):** las tarjetas bloqueadas por `ROBO`, `EXTRAVIO` o `SOSPECHA_FRAUDE` **no se pueden desbloquear**.
- Responden 422 con `Una tarjeta bloqueada por {motivo} no puede desbloquearse. Solicite una reposición`.
- La única salida es la **reposición** (sección 11).
- Solo `SOLICITUD_CLIENTE` permite desbloquear.
- En el catálogo, `permiteDesbloqueo` queda `true` solo para `SOLICITUD_CLIENTE`.
- **Semilla:** Sofía mantiene su tarjeta bloqueada por sospecha de fraude. Ya no se puede desbloquear, así que sirve para mostrar el aviso y la reposición. Se agrega otra tarjeta, por ejemplo la de Luis, bloqueada por `SOLICITUD_CLIENTE`, que sí se puede desbloquear.

## 18. Permisos por rol (control de acceso granular)

### 18.1 Permisos (roles de realm en Keycloak, el backend los verifica uno por uno)

| Permiso | Qué permite |
|---|---|
| `CLIENTES_CONSULTAR` | Ver clientes y su detalle |
| `CLIENTES_GESTIONAR` | Crear y editar clientes |
| `CLIENTES_ELIMINAR` | Eliminar clientes |
| `SOLICITUDES_CONSULTAR` | Ver solicitudes |
| `SOLICITUDES_REGISTRAR` | Crear solicitudes |
| `SOLICITUDES_DECIDIR` | Aprobar y rechazar |
| `CUENTAS_CONSULTAR` | Ver cuentas, tarjetas, movimientos y el catálogo |
| `CONSUMOS_REGISTRAR` | Registrar consumos |
| `PAGOS_REGISTRAR` | Registrar pagos |
| `MOVIMIENTOS_REVERSAR` | Reversar consumos y pagos |
| `TARJETAS_BLOQUEAR` | Bloquear tarjetas |
| `TARJETAS_DESBLOQUEAR` | Desbloquear tarjetas (solo motivo SOLICITUD_CLIENTE) |
| `TARJETAS_REPONER` | Reponer tarjetas |
| `DATOS_SENSIBLES_VER` | Ver el número completo de cuenta y de tarjeta |

### 18.2 Perfiles (roles compuestos de Keycloak que agrupan permisos)

| Perfil | Permisos incluidos |
|---|---|
| `ADMINISTRADOR` | **Todos** |
| `OPERADOR` (atención al cliente) | CLIENTES_CONSULTAR, CLIENTES_GESTIONAR, SOLICITUDES_CONSULTAR, SOLICITUDES_REGISTRAR, CUENTAS_CONSULTAR, CONSUMOS_REGISTRAR, TARJETAS_BLOQUEAR, TARJETAS_REPONER |
| `CAJERO` | CLIENTES_CONSULTAR, CUENTAS_CONSULTAR, PAGOS_REGISTRAR |
| `ANALISTA_CREDITO` | CLIENTES_CONSULTAR, SOLICITUDES_CONSULTAR, SOLICITUDES_DECIDIR, CUENTAS_CONSULTAR |

### 18.3 Usuarios de demostración

Solo **dos** usuarios tienen todos los permisos.

| Usuario | Contraseña | Perfil | Nombre |
|---|---|---|---|
| `admin` | `admin123` | ADMINISTRADOR | Administrador Sistema |
| `supervisor` | `supervisor123` | ADMINISTRADOR | Supervisora de Crédito |
| `operador` | `operador123` | OPERADOR | Operador Atención |
| `cajero` | `cajero123` | CAJERO | Cajero Agencia |
| `analista` | `analista123` | ANALISTA_CREDITO | Analista de Crédito |

### 18.4 Implementación

- **Keycloak:** los 14 permisos y los 4 perfiles se definen como roles de realm. Los perfiles son **roles compuestos** (`composite: true`, con `composites.realm`). El access token incluye los roles efectivos, es decir, los compuestos ya expandidos en `realm_access.roles`. Hay que verificarlo con un token real.
- **Backend:**
  - `ConfiguracionSeguridad` autoriza **por permiso**, no por perfil. Por ejemplo, `POST /gestion-tarjetas/cuentas/*/pagos` requiere `hasRole("PAGOS_REGISTRAR")`.
  - Todos los endpoints se mapean a un permiso según 18.1:
    - El catálogo requiere `CUENTAS_CONSULTAR` o `SOLICITUDES_CONSULTAR`.
    - El resumen del frontend usa listados que requieren los permisos de consulta.
  - Los nombres de los permisos van como constantes en una clase `Permisos`, sin strings sueltos.
- **Frontend:**
  - `tienePermiso(permiso)` lee `realm_access.roles`. Las constantes van en `utilidades/permisos.js`.
  - Cada botón, acción, ruta y opción de la barra lateral se muestra según su permiso. Una ruta sin permiso redirige a una página "Sin acceso".
  - Si el usuario no tiene permiso para ver una sección del Resumen, esa sección se oculta.
  - La etiqueta de rol de la barra superior muestra el perfil: Administrador, Operador de atención, Cajero o Analista de crédito.

## 19. Datos sensibles: número completo según permiso

- La API sigue enviando siempre `numeroCuentaEnmascarado` y `numeroTarjetaEnmascarado`.
- **Si el usuario tiene `DATOS_SENSIBLES_VER`**, las respuestas también traen `numeroCuentaCompleto` y `numeroTarjetaCompleto` (en `CuentaRespuesta`, `TarjetaRespuesta` y su `tarjetaVigente`, y en `SolicitudRespuesta` si está aprobada). Sin el permiso, esos campos van en `null`.
- La decisión se toma en el backend: el mapper recibe un indicador `incluirDatosSensibles` que el controlador o servicio obtiene del usuario autenticado. **Nunca** se envía el dato completo para que el frontend lo oculte.
- En la UI, junto a cada número enmascarado aparece un botón de ojo "Mostrar número", solo si el número completo viene en la respuesta. Al presionarlo muestra el número formateado en grupos de 4; vuelve a ocultarse al presionarlo otra vez o al salir de la página.

## 20. Ajustes de interfaz

- **Tarjeta visual:**
  - **No muestra el número de cuenta.** Muestra el número de tarjeta, el titular, el tipo y el vencimiento.
  - El chip es un **chip dorado dibujado en SVG** (`ChipTarjeta.vue`): un rectángulo redondeado con degradado dorado y las líneas de contacto típicas de un chip EMV. Se ve bien sobre los 4 colores de tarjeta y con el velo de "Bloqueada".
- **Botón "Regresar":** en todas las páginas internas, como detalle de cliente, formulario de cliente, detalle de cuenta, nueva solicitud, revisión de solicitud y "Sin acceso".
  - Está en `EncabezadoPagina` como prop `rutaRegreso` y se dibuja como botón secundario con flecha a la izquierda, sobre el título y junto a las migas.
  - Usa `router.back()` si hay historial dentro de la app; si no, navega a `rutaRegreso`.
  - Las páginas de primer nivel (Resumen, Clientes, Solicitudes, Cuentas) no lo muestran.

---

# Parte 4 · Cargos, requisitos de reposición, renovación, cancelación y ajustes

> Esta parte **reemplaza** lo que contradiga a las anteriores.

## 21. Cargos (nuevo tipo de movimiento)

Nueva tabla `dbo.cargo`:
- `id_cargo`, `id_cuenta` (FK con CASCADE), `tipo_cargo`, `monto > 0`, `fecha`, `descripcion`, `fecha_registro` y `usuario_registro`.
- Columnas de reverso iguales a consumo y pago: `reversado`, `motivo_reverso`, `fecha_reverso` y `usuario_reverso`.

| `tipo_cargo` | Descripción | Origen |
|---|---|---|
| `COMISION_REPOSICION` | Comisión por reposición | **Automático** al reponer por extravío (sección 22) |
| `INTERES` | Intereses | Registro manual (simula el proceso de cierre de mes) |
| `RECARGO_MORA` | Recargo por mora | Registro manual (simula el proceso de cierre de mes) |

- **Saldo:** los cargos **suman al saldo** igual que los consumos. Cuentan en el saldo al corte y en la mora, salvo los reversados.
- **Límite:** a diferencia de los consumos, un cargo **puede dejar el saldo por encima del límite** (sobregiro por cargos). Por eso se elimina el CHECK `saldo_actual <= limite_credito` de la cuenta y solo queda `saldo_actual >= 0`. El límite de los consumos se sigue validando en el servicio. `creditoDisponible` se calcula como `max(0, límite − saldo)`.
- **Endpoint de registro:** `POST /gestion-tarjetas/cuentas/{idCuenta}/cargos`, con cuerpo `{ tipoCargo: INTERES | RECARGO_MORA, monto, descripcion, fecha? }`. Requiere el permiso `CARGOS_REGISTRAR` y responde 201. `COMISION_REPOSICION` no se acepta por este endpoint (400 o 422).
- **Movimientos:** `GET /gestion-tarjetas/cuentas/{id}/movimientos` incluye los cargos con `tipoMovimiento: "CARGO"`, `tipoCargo` y `descripcionTipoCargo`.
- **Constante de la comisión:** clase `Tarifas` con `COMISION_REPOSICION_EXTRAVIO = 45.00`. El catálogo la expone como `comisionReposicionExtravio`.

## 22. Requisitos y costo de la reposición (amplía la sección 11)

`ReposicionTarjetaPeticion` agrega `denunciaPresentada` (boolean) y `tarjetaDeterioradaPresentada` (boolean).

| Motivo | Requisito | Costo |
|---|---|---|
| `ROBO` | `denunciaPresentada` debe ser `true`. Si no, 422: `Para reponer por robo el cliente debe presentar la denuncia del robo` | Sin costo |
| `EXTRAVIO` | Ninguno | **Q 45.00**: se registra automáticamente un cargo `COMISION_REPOSICION` con descripción `Comisión por reposición de tarjeta por extravío` en la misma transacción |
| `DETERIORO` | `tarjetaDeterioradaPresentada` debe ser `true`. Si no, 422: `Para reponer por deterioro el cliente debe presentar la tarjeta deteriorada` | Sin costo |
| `SOSPECHA_FRAUDE` | Ninguno | Sin costo |

- La tarjeta reemplazada guarda `denuncia_presentada` y `tarjeta_deteriorada_presentada` (`BIT NULL`) como evidencia del requisito.
- `TarjetaRespuesta` agrega `denunciaPresentada`, `tarjetaDeterioradaPresentada` e `idCargoReposicion` (o `null`).
- **UI:**
  - El modal de reposición muestra la **casilla** que corresponda al motivo: "El cliente presentó la denuncia del robo" o "El cliente presentó la tarjeta deteriorada". Sin la casilla marcada, el botón se deshabilita.
  - Para extravío muestra el aviso "Se cobrará una comisión de Q 45.00 a la cuenta".

## 23. Renovación por vencimiento

- **Endpoint:** `POST /gestion-tarjetas/tarjetas/{idTarjeta}/renovaciones`, sin cuerpo. Responde **201** con la nueva tarjeta. Requiere el permiso `TARJETAS_RENOVAR`. **No tiene costo.**
- **Cuándo está vencida:** `hoy > fechaVencimiento`. Hay que comparar con `LocalDate` del `Clock`.

| # | Regla | Mensaje (422) |
|---|---|---|
| RNV-01 | Solo se renueva la tarjeta vigente, y debe estar **vencida**. | `La tarjeta vence el {dd/MM/yyyy}; solo puede renovarse cuando haya vencido` |
| RNV-02 | La tarjeta debe estar `ACTIVA`. Si está bloqueada, primero se resuelve el bloqueo o se hace una reposición. | `La tarjeta está bloqueada por {motivo}; resuelva el bloqueo antes de renovarla` |
| RNV-03 | La nueva tarjeta queda en la misma cuenta: número nuevo, vencimiento a 5 años, `id_tarjeta_anterior` apuntando a la vieja. La vieja pasa a `REEMPLAZADA` con `motivo_reposicion = RENOVACION`. | |
| RNV-04 | **Una tarjeta vencida no acepta consumos.** Los pagos sí. | `La tarjeta venció el {dd/MM/yyyy}. Debe renovarse antes de registrar consumos` |

- `MotivoReposicion` agrega `RENOVACION` ("Renovación por vencimiento"). **No se acepta** en el endpoint de reposiciones; solo lo usa la renovación.
- `TarjetaRespuesta` agrega `vencida` (boolean) y `puedeRenovarse` (boolean). `puedeRenovarse` es `true` si la tarjeta es vigente, está activa y está vencida.
- **UI:** si la tarjeta está vencida aparece la insignia "Vencida" y el botón **"Renovar tarjeta"**, visible solo con el permiso y si `puedeRenovarse`. El botón abre una confirmación: "Se emitirá una nueva tarjeta sin costo en la misma cuenta".

## 24. Cancelación de la cuenta (producto)

- **Endpoint:** `PUT /gestion-tarjetas/cuentas/{idCuenta}/cancelar`. Cuerpo `{ motivoCancelacion, comentario? }`. Requiere el permiso `CUENTAS_CANCELAR` y responde 200 con `CuentaRespuesta`.
- **Motivos** (enum `MotivoCancelacion`): `SOLICITUD_CLIENTE` ("Solicitud del cliente") y `DECISION_BANCO` ("Decisión del banco").
- **Columnas nuevas en `cuenta_credito`:**
  - `estado_cuenta` (`ACTIVA` o `CANCELADA`, `DEFAULT 'ACTIVA'`).
  - `motivo_cancelacion`, `comentario_cancelacion`, `fecha_cancelacion`, `usuario_cancelacion`.
  - Un CHECK de coherencia entre el estado y los datos de cancelación.
- `EstadoTarjeta` agrega `CANCELADA`.

| # | Regla | Mensaje (422) |
|---|---|---|
| CA-01 | La cuenta debe estar `ACTIVA`. | `La cuenta ya fue cancelada el {dd/MM/yyyy}` |
| CA-02 | La cuenta debe estar **al día**: estado de pago `AL_DIA`, sin mora ni pago pendiente. | `Para cancelar, la cuenta debe estar al día en sus pagos` |
| CA-03 | El saldo debe ser **Q 0.00**. Una cuenta al día puede tener saldo del ciclo en curso que aún debe pagarse. | `Para cancelar, la cuenta debe tener saldo Q 0.00. Saldo actual: Q {saldo}` |
| CA-04 | Al cancelar, la cuenta pasa a `CANCELADA` con motivo, fecha y usuario, y la tarjeta vigente pasa a `CANCELADA`. **Nada se borra:** se conserva el historial de tarjetas, movimientos y cargos. | |
| CA-05 | Una cuenta cancelada no acepta consumos, pagos, cargos, reposiciones, renovaciones, bloqueos ni ajustes. | `La cuenta está cancelada` |

- **El cliente sigue activo.**
  - `ClienteRespuesta` agrega `cantidadProductosActivos`.
  - La lista de clientes muestra "Sin producto activo" cuando ese valor es 0.
- **Reglas que solo cuentan cuentas `ACTIVA`:** RC-02 (mora) y RC-07 (una cuenta por tipo). Así, un cliente con una cuenta Clásica cancelada **puede solicitar una Clásica nueva**, y recibe un número de cuenta nuevo.
- **Listados:**
  - `GET /gestion-tarjetas/cuentas` acepta `?estado=ACTIVA|CANCELADA`. Sin parámetro devuelve todas.
  - `GET /gestion-tarjetas/clientes/{id}/cuentas` devuelve todas, también las canceladas.
- **UI, detalle del cliente:**
  - Dos paneles: "Productos activos" e "Historial de productos", este último con las cuentas canceladas, su motivo y su fecha.
  - En "Nueva solicitud", el panel del cliente muestra su historial de productos anteriores (tipo, cuenta enmascarada, apertura, cancelación y motivo).
- **UI, detalle de cuenta cancelada:**
  - Insignia "Cancelada", aviso gris con motivo, fecha y usuario, y todas las acciones ocultas.
  - El historial de movimientos y tarjetas sigue visible.
- **UI, botón "Cancelar cuenta":** es un botón de peligro, visible con permiso si la cuenta está activa. Abre un modal que muestra los requisitos (al día y saldo Q 0.00) con una marca de verificación verde o una x roja, y se deshabilita si no se cumplen.

## 25. Ajustes externos: devolución de intereses y exoneración de mora

Son correcciones administrativas sobre **cargos**. Requieren el permiso `CARGOS_AJUSTAR`.

| Método | Ruta | Cuerpo | Descripción |
|---|---|---|---|
| PUT | `/gestion-tarjetas/cargos/{idCargo}/reversar` | `{ motivo }` (de 5 a 250 caracteres) | Reversa **un** cargo. En la UI se llama según su tipo: INTERES → **"Devolución de intereses"**, RECARGO_MORA → **"Exoneración de mora"**, COMISION_REPOSICION → **"Reversión de comisión"**. |
| POST | `/gestion-tarjetas/cuentas/{idCuenta}/exoneracion-mora` | `{ motivo }` | Reversa **todos** los cargos `RECARGO_MORA` no reversados de la cuenta. Responde 200 con `{ cantidadCargosExonerados, montoExonerado, cuenta: CuentaRespuesta }`. Si no hay ninguno, 422: `La cuenta no tiene recargos por mora pendientes de exonerar`. |

- Un cargo reversado resta su monto del saldo, que nunca baja de 0 (si quedaría negativo, 422 como en RV-02). Deja de contar en todos los cálculos y conserva el motivo, la fecha y el usuario (RV-01 y RV-05 aplican igual).
- **La exoneración de mora no paga la deuda.** Elimina los recargos; si la cuenta sigue con saldo al corte sin pagar, sigue en mora hasta que se pague.
- Se bloquea con pesimismo la cuenta. No se permite sobre una cuenta cancelada (CA-05).
- **UI, movimientos:**
  - Los cargos se muestran con su tipo: "Intereses", "Recargo por mora", "Comisión por reposición".
  - La acción depende del tipo: "Devolver", "Exonerar" o "Reversar".
  - En el encabezado de la cuenta hay un botón **"Exonerar mora"** si hay recargos por mora pendientes y el usuario tiene el permiso.
  - Hay un botón **"Registrar cargo"** con el permiso `CARGOS_REGISTRAR`. Abre un modal con el tipo (Intereses o Recargo por mora), el monto, la descripción y una fecha opcional.
  - Los cargos reversados se muestran como "Devuelto", "Exonerado" o "Reversado".

## 26. Permisos nuevos (amplía la sección 18)

| Permiso | Qué permite | Perfiles |
|---|---|---|
| `TARJETAS_RENOVAR` | Renovar tarjetas vencidas | ADMINISTRADOR, OPERADOR |
| `CUENTAS_CANCELAR` | Cancelar cuentas | ADMINISTRADOR |
| `CARGOS_REGISTRAR` | Registrar intereses y recargos por mora | ADMINISTRADOR |
| `CARGOS_AJUSTAR` | Devolución de intereses, exoneración de mora y reversión de comisiones | ADMINISTRADOR |

Solo los **dos usuarios administradores** (`admin` y `supervisor`) tienen todos los permisos.

## 27. Datos semilla (amplía la sección 14)

- **Carlos (PLATINO en mora):** tiene cargos `INTERES` y `RECARGO_MORA` (2 recargos de meses distintos) para probar la exoneración de mora y la devolución de intereses. El saldo es coherente con consumos, pagos y cargos.
- **Ana Lucía:** su cuenta **CLASICA** tiene la tarjeta **vencida** (emitida hace más de 5 años, cuenta abierta hace más de 5 años) para probar la renovación, y un cargo `INTERES` para probar la devolución. Su cuenta ORO mantiene la reposición por robo, ahora con `denunciaPresentada = true`.
- **Cliente nuevo, Jorge Alberto Pineda Cifuentes:**
  - **Sin producto activo.**
  - Tiene una cuenta CLASICA **CANCELADA** por solicitud del cliente hace unos meses, con saldo 0, su historial de movimientos (consumos y pagos que llevaron el saldo a 0) y su tarjeta CANCELADA.
  - Tiene empleo de más de 12 meses e ingreso suficiente, así que puede solicitar una Clásica nueva.
- **Luis:** su cuenta CLASICA con saldo 0, al día y bloqueada por solicitud del cliente sirve para probar la **cancelación**. Cancelar está permitido aunque la tarjeta esté bloqueada.
- Todos los demás escenarios se mantienen.

---

# Parte 5 · Interés automático del 5 % mensual y pago mínimo del 10 %

> Esta parte **reemplaza** lo que contradiga a las anteriores, en especial las secciones 4, 5, 21 y 25.

## 28. Pago mínimo y pago de contado por ciclo

Para cada ciclo con corte `C` (día 5) y fecha límite `P` (día 25):

| Concepto | Cálculo |
|---|---|
| Saldo al corte | Sección 4: consumos + cargos − pagos hasta `C`, sin reversados y nunca negativo |
| **Pago mínimo** | **10 %** del saldo al corte, redondeado **hacia arriba** al centavo. Si el saldo al corte es 0, el pago mínimo es 0. |
| Pago de contado | El saldo al corte completo |
| Pagado en el ciclo | Pagos no reversados con fecha posterior a `C` |
| Pago mínimo pendiente | `max(0, pagoMínimo − pagadoEnElCiclo)` |
| Pago de contado pendiente | `max(0, saldoAlCorte − pagadoEnElCiclo)` |

Constantes en `Tarifas`: `PORCENTAJE_PAGO_MINIMO = 10` y `TASA_INTERES_MENSUAL = 5`. El catálogo las expone como `porcentajePagoMinimo` y `tasaInteresMensual`.

## 29. Estado de pago y mora (reemplaza la sección 5)

1. **Mora:** se toma el último ciclo vencido (`hoy > P`). Si lo pagado desde `C` hasta hoy es **menor que el pago mínimo** de ese ciclo, el estado es **`EN_MORA`**.
   - `montoEnMora` es lo que falta para cubrir el pago mínimo vencido.
   - `diasMora` son los días desde `P`.
   - Al completar el pago mínimo vencido, la mora se quita.
2. **Ciclo actual** (si no hay mora, con el corte `C2` más reciente):
   - Si `hoy ≤ P2` y aún falta pago mínimo, el estado es **`PAGO_PENDIENTE`**.
   - Si no, el estado es **`AL_DIA`**, aunque quede saldo de contado. Esa parte genera intereses.
3. Campos en `CuentaRespuesta`:
   - `pagoMinimo`, `pagoMinimoPendiente`, `pagoContado` (igual al saldo al corte) y `pagoContadoPendiente`.
   - `pagoPendiente` se **elimina**; en su lugar se usa `pagoContadoPendiente`.
   - Se mantienen `saldoAlCorte`, `montoEnMora`, `diasMora` y las fechas.
4. **Cancelación (CA-02 y CA-03 sin cambios):** requiere estado `AL_DIA` y saldo Q 0.00.

## 30. Interés automático del 5 % mensual

- Cuando se vence un ciclo (`hoy > P`), se genera **automáticamente** un cargo `INTERES` sobre la parte del saldo al corte **no pagada a tiempo**:
  - `baseInteres = saldoAlCorte(C) − pagos con fecha en (C, P]`.
  - Si la base es mayor que 0, el interés es **5 % de la base**, redondeado al centavo con HALF_UP.
  - El cargo se fecha en **`P + 1 día` a las 00:00:00** (el día 26), así que entra en el saldo al corte del ciclo siguiente.
  - Descripción: `Intereses del ciclo con corte dd/MM/yyyy (5 % sobre Q {base})`.
- **Si se paga el contado completo antes del día 25, no hay interés.** Pagar solo el mínimo evita la mora pero no el interés.
- **Idempotencia:**
  - El cargo guarda `ciclo_corte DATE` (columna nueva en `cargo`, `NULL` para los demás tipos).
  - Un índice único filtrado `UNIQUE (id_cuenta, ciclo_corte) WHERE tipo_cargo = 'INTERES'` garantiza **un solo interés por ciclo**, aunque esté reversado. Así una devolución no provoca que se vuelva a generar.
- **Cómo se ejecuta:** un servicio `ProcesoInteresesService.generarInteresesPendientes(cuenta)` revisa todos los ciclos vencidos desde la apertura de la cuenta que todavía no tienen interés. Corre:
  1. **Al arrancar** la aplicación (`ApplicationReadyEvent`), para todas las cuentas `ACTIVA`.
  2. **Todos los días** a las 00:05 hora de Guatemala, con `@Scheduled(cron = "0 5 0 * * *", zone = "America/Guatemala")` y `@EnableScheduling`.
  3. **Antes** de cualquier operación que escriba en una cuenta, dentro de la misma transacción y con la cuenta bloqueada: consumo, pago, cargo, reverso, exoneración, cancelación, reposición y renovación.
- **Inserción:** se hace con bloqueo pesimista de la cuenta y actualiza `saldo_actual`. Un cargo de interés puede sobregirar el límite (sección 21).
- **Usuario:** el que registra es `sistema`.
- **Ciclos que no generan interés:**
  - Ciclos anteriores a la apertura de la cuenta.
  - Cuentas `CANCELADA`, después de la fecha de cancelación.
- **Cargos manuales:** el tipo `INTERES` **ya no se registra a mano**. `tiposCargoManual` queda solo con `RECARGO_MORA`; un POST con `INTERES` responde 422 `Los intereses se generan automáticamente al vencer cada ciclo`.
- **Devolución de intereses (sección 25) sin cambios:** se reversa el cargo `INTERES` con motivo. Por la idempotencia, no se regenera.

## 31. Pagos en la interfaz

- El modal "Registrar pago" muestra:
  - Una tarjeta resumen con **Pago mínimo** (pendiente), **Pago de contado** (pendiente) y la fecha límite.
  - **Opciones de monto:** "Monto en mora Q z" (solo si la cuenta está en mora), "Pago mínimo Q x", "Pago de contado Q y" y "Otro monto". Las de monto fijo rellenan el campo.
- Se permiten pagos parciales menores al mínimo, porque los pagos se acumulan. En ese caso aparece un aviso amarillo: "Este pago no cubre el pago mínimo de Q x; la cuenta podría entrar en mora".
- Un aviso informativo explica: "Si no paga el contado antes del día 25, se cobrará un interés del 5 % sobre el saldo no pagado".
- El detalle de cuenta muestra en "Ciclo de facturación":
  - Pago mínimo, pago de contado, lo pagado en el ciclo y lo que falta de cada uno.
  - La tasa de interés.
- En "Registrar cargo", la opción de intereses ya no existe; solo queda recargo por mora.

## 32. Semilla (amplía la sección 27)

- Los intereses de los ciclos ya vencidos **los genera el proceso al arrancar**. La semilla **no** inserta cargos `INTERES` a mano, salvo que sea necesario para la demostración de devolución. En ese caso se insertan con su `ciclo_corte` correcto, y el proceso los respeta por la idempotencia.
- La semilla se diseña para que, **después** de correr el proceso:
  - **Carlos:** está en **mora** porque pagó menos del mínimo, y tiene un interés generado y recargos por mora.
  - **Ana Lucía (ORO):** al día, porque pagó el contado y no generó interés. Su cuenta CLASICA pagó solo el mínimo: está al día pero **con un interés generado**, que sirve para probar la devolución.
  - **Sofía:** en `PAGO_PENDIENTE` en el ciclo actual.
  - **Luis:** saldo 0, para probar la cancelación.
  - **Jorge:** cuenta cancelada.
- El reporte final indica los saldos **después** del proceso de intereses.

---

# Parte 6 · Eliminación de clientes

## 33. Solo se elimina un cliente sin productos activos

| # | Regla | Mensaje (422) |
|---|---|---|
| CL-01 | No se puede eliminar un cliente que tenga **cuentas activas**. Primero hay que cancelarlas (sección 24), lo que exige que estén al día y con saldo Q 0.00. | `No se puede eliminar el cliente: tiene {n} cuenta(s) activa(s). Cancele primero sus cuentas` |

- Un cliente sin cuentas activas (sin productos, o solo con cuentas canceladas) se elimina igual que antes. Se borran sus solicitudes, sus cuentas canceladas y todo su historial (sección 9.4).
- **UI:** `ModalEliminarCliente` marca cada cuenta como activa o cancelada. Si alguna está activa, muestra el aviso "No se puede eliminar este cliente" y deshabilita el botón.
