# CONTRATO DE ARQUITECTURA — Sistema de Gestión de Tarjetas de Crédito (CHN)

> Este documento es la **única fuente de verdad**. Los agentes de Base de Datos, Backend y Frontend lo siguen literalmente: nombres, rutas, tipos y valores exactos. Si algo no aparece aquí, se elige la opción más simple y no se cambia nada de lo que sí está definido.
> Raíz: `/Users/jafetchacon/Documents/Codex/2026-08-06/g/sistema-tarjetas-credito` (hoy solo contiene `docs/` vacío).

---

> **Modelo vigente:** el modelo de datos y la API de este documento (tarjeta con límite y saldo, rutas `/gestion-tarjetas/tarjetas/{id}/consumos`, `/gestion-tarjetas/tarjetas/{id}/pagos`, `GET /gestion-tarjetas/tarjetas`, `/gestion-tarjetas/clientes/{id}/tarjetas`) fueron **reemplazados** por [REGLAS_CREDITO.md](REGLAS_CREDITO.md), **Parte 2** (secciones 9 a 15): el crédito vive en la entidad `CuentaCredito` y la tarjeta es solo el plástico anclado a la cuenta, con reposiciones, reversos de movimientos y API por cuenta. El diagrama actualizado está en [diagrama-entidad-relacion.md](diagrama-entidad-relacion.md). Donde este documento contradiga a la Parte 2, manda la Parte 2.
>
> **Parte 3 (secciones 16 a 20 de REGLAS_CREDITO.md):** también reemplaza lo que aquí se diga sobre seguridad y datos. Los roles ADMINISTRADOR/OPERADOR de este documento ya no se usan para autorizar: ahora hay 14 **permisos** (roles de realm) agrupados en 4 perfiles compuestos (ADMINISTRADOR, OPERADOR, CAJERO, ANALISTA_CREDITO), 5 usuarios demo y un backend que autoriza por permiso con `denyAll` por defecto. Además, el cliente tiene un código de 8 dígitos (secuencia `dbo.seq_codigo_cliente`), solo el bloqueo por solicitud del cliente se puede desbloquear (RB-04) y los números completos de cuenta y tarjeta solo se envían a quien tiene `DATOS_SENSIBLES_VER`.
>
> **Partes 4 y 5 (secciones 21 a 32 de REGLAS_CREDITO.md):** agregan la tabla `cargo` (comisión de reposición, intereses y recargos por mora, que pueden sobregirar el límite), requisitos y costo de la reposición (denuncia por robo, tarjeta deteriorada, Q 45.00 por extravío), la renovación de tarjetas vencidas, la cancelación de cuentas (estado `CANCELADA` en cuenta y tarjeta, nada se borra), los ajustes sobre cargos (devolución de intereses, exoneración de mora y reversión de comisión), 4 permisos nuevos (`TARJETAS_RENOVAR`, `CUENTAS_CANCELAR`, `CARGOS_REGISTRAR`, `CARGOS_AJUSTAR`; 18 en total), el **pago mínimo del 10 %** con la mora calculada contra ese mínimo y el **interés automático del 5 % mensual** que genera `ProcesoInteresesService`. Reemplazan lo que aquí se diga sobre saldo (ya no hay tope `saldo <= límite` en la BD), estado de pago y endpoints.

> **Nota:** las reglas de crédito, el ciclo de facturación, el estado de pago, el bloqueo de tarjetas y los datos semilla vigentes están en [REGLAS_CREDITO.md](REGLAS_CREDITO.md), que prevalece sobre las tablas de datos semilla de este documento.

## 0. Responsables por archivo (trabajo en paralelo)

| Agente | Es dueño de |
|---|---|
| **Base de Datos (BD)** | `database/**`, `keycloak/realm-tarjetas.json`, `docker-compose.yml`, `.env.example`, `.gitignore`, `.gitattributes`, `docs/diagrama-entidad-relacion.md` |
| **Backend (BE)** | `backend/**` (incluye su `Dockerfile` y el Maven Wrapper) |
| **Frontend (FE)** | `frontend/**` (incluye su `Dockerfile` y `nginx.conf`) |
| **Orquestador** | `README.md`, `docs/ARQUITECTURA.md` (este archivo), `docs/MANUAL_TECNICO.md`, `docs/MANUAL_USUARIO.md` |

Ningún agente edita archivos de otro.

---

## 1. Árbol de carpetas

```
sistema-tarjetas-credito/
├── docker-compose.yml
├── .env.example
├── .gitignore
├── .gitattributes                      # *.sh text eol=lf ; mvnw text eol=lf
├── README.md
├── docs/
│   ├── ARQUITECTURA.md
│   ├── MANUAL_TECNICO.md
│   ├── MANUAL_USUARIO.md
│   └── diagrama-entidad-relacion.md    # Mermaid erDiagram
├── database/
│   ├── inicializar-base-datos.sh       # lo ejecuta el contenedor inicializador
│   ├── 01_crear_base_datos.sql         # BD + login/usuario de la aplicación
│   ├── 02_crear_esquema.sql            # tablas, constraints, índices (idempotente)
│   └── 03_datos_semilla.sql            # datos demo (solo si dbo.cliente está vacía)
├── keycloak/
│   └── realm-tarjetas.json
├── backend/
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd
│   ├── .mvn/wrapper/maven-wrapper.properties
│   ├── Dockerfile
│   ├── .dockerignore                   # target/, .idea/, *.iml
│   └── src/
│       ├── main/java/gt/com/chn/tarjetas/
│       │   ├── AplicacionGestionTarjetas.java
│       │   ├── config/
│       │   │   ├── ConfiguracionSeguridad.java
│       │   │   ├── ConvertidorRolesKeycloak.java
│       │   │   ├── ManejadorErroresSeguridad.java
│       │   │   └── ConfiguracionOpenApi.java
│       │   ├── controller/
│       │   │   ├── ClienteController.java
│       │   │   ├── SolicitudController.java
│       │   │   └── TarjetaController.java
│       │   ├── service/
│       │   │   ├── ClienteService.java
│       │   │   ├── SolicitudService.java
│       │   │   ├── TarjetaService.java
│       │   │   ├── MovimientoTarjetaService.java
│       │   │   └── GeneradorNumeroTarjeta.java
│       │   ├── repository/
│       │   │   ├── ClienteRepository.java
│       │   │   ├── SolicitudTarjetaRepository.java
│       │   │   ├── TarjetaCreditoRepository.java
│       │   │   ├── ConsumoRepository.java
│       │   │   └── PagoRepository.java
│       │   ├── entity/
│       │   │   ├── Cliente.java
│       │   │   ├── SolicitudTarjeta.java
│       │   │   ├── TarjetaCredito.java
│       │   │   ├── Consumo.java
│       │   │   ├── Pago.java
│       │   │   ├── TipoTarjeta.java
│       │   │   └── EstadoSolicitud.java
│       │   ├── dto/
│       │   │   ├── ClientePeticion.java
│       │   │   ├── ClienteRespuesta.java
│       │   │   ├── SolicitudPeticion.java
│       │   │   ├── AprobacionPeticion.java
│       │   │   ├── RechazoPeticion.java
│       │   │   ├── SolicitudRespuesta.java
│       │   │   ├── TarjetaRespuesta.java
│       │   │   ├── MovimientoPeticion.java
│       │   │   ├── MovimientoRespuesta.java
│       │   │   └── ErrorRespuesta.java
│       │   ├── mapper/
│       │   │   ├── ClienteMapper.java
│       │   │   ├── SolicitudMapper.java
│       │   │   ├── TarjetaMapper.java
│       │   │   └── MovimientoMapper.java
│       │   └── exception/
│       │       ├── RecursoNoEncontradoException.java
│       │       ├── RecursoDuplicadoException.java
│       │       ├── ReglaNegocioException.java
│       │       └── ManejadorGlobalExcepciones.java
│       ├── main/resources/application.yml
│       └── test/java/gt/com/chn/tarjetas/service/
│           ├── GeneradorNumeroTarjetaTest.java
│           ├── SolicitudServiceTest.java
│           └── MovimientoTarjetaServiceTest.java
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── index.html
    ├── .env.example
    ├── .env.development
    ├── Dockerfile
    ├── nginx.conf
    ├── .dockerignore                   # node_modules, dist
    └── src/
        ├── main.js
        ├── App.vue
        ├── estilos/principal.css
        ├── autenticacion/servicioAutenticacion.js
        ├── router/index.js
        ├── servicios/
        │   ├── clienteHttp.js
        │   ├── clientesServicio.js
        │   ├── solicitudesServicio.js
        │   └── tarjetasServicio.js
        ├── utilidades/
        │   ├── constantes.js
        │   ├── formatos.js
        │   └── errores.js
        ├── componentes/
        │   ├── BarraNavegacion.vue
        │   ├── MensajeAlerta.vue
        │   ├── VentanaModal.vue
        │   ├── EtiquetaEstado.vue
        │   ├── FormularioSolicitud.vue
        │   ├── FormularioDecision.vue
        │   ├── FormularioMovimiento.vue
        │   └── TablaMovimientos.vue
        └── vistas/
            ├── ListaClientesVista.vue
            ├── FormularioClienteVista.vue
            ├── DetalleClienteVista.vue
            ├── BandejaSolicitudesVista.vue
            ├── DetalleTarjetaVista.vue
            └── NoEncontradoVista.vue
```

---

## 2. Modelo de datos (T-SQL)

### 2.1 Convenciones
- BD: **`GestionTarjetasCredito`**, collation **`Modern_Spanish_CI_AS`**, esquema `dbo`.
- Tablas y columnas en **snake_case minúscula, en singular**: `dbo.cliente`, `dbo.solicitud_tarjeta`, `dbo.tarjeta_credito`, `dbo.consumo`, `dbo.pago`.
- PK: `id_<tabla corta>` como `BIGINT IDENTITY(1,1)`.
- Constraints con nombre: `pk_`, `fk_<tabla>_<referenciada>`, `uq_<tabla>_<columna>`, `ck_<tabla>_<regla>`, `df_<tabla>_<columna>`, `ix_<tabla>_<columna>`.
- **Texto: siempre `VARCHAR`, nunca `NVARCHAR` ni `CHAR`.** Con Hibernate `ddl-auto=validate`, String se valida contra varchar; NVARCHAR/CHAR harían fallar el arranque. Modern_Spanish_CI_AS (cp1252) soporta á, é, ñ.
- Dinero: `DECIMAL(18,2)`. Fechas sin hora: `DATE`. Fecha y hora: `DATETIME2(3)`.
- Tipo y estado: **CHECK sobre VARCHAR(10)**, sin tablas catálogo. Así coinciden 1:1 con `@Enumerated(EnumType.STRING)` y es más simple. Las etiquetas visibles ("Clásica", etc.) viven en el enum Java y en `constantes.js`.

### 2.2 DDL exacto (`02_crear_esquema.sql`, cada CREATE dentro de `IF OBJECT_ID('dbo.x','U') IS NULL BEGIN ... END`)

```sql
CREATE TABLE dbo.cliente (
    id_cliente            BIGINT IDENTITY(1,1) NOT NULL,
    nombre                VARCHAR(100) NOT NULL,
    apellido              VARCHAR(100) NOT NULL,
    numero_identificacion VARCHAR(20)  NOT NULL,
    fecha_nacimiento      DATE         NOT NULL,
    direccion             VARCHAR(250) NOT NULL,
    correo_electronico    VARCHAR(150) NOT NULL,
    telefono              VARCHAR(20)  NOT NULL,
    fecha_creacion        DATETIME2(3) NOT NULL CONSTRAINT df_cliente_fecha_creacion DEFAULT SYSDATETIME(),
    fecha_actualizacion   DATETIME2(3) NULL,
    CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
    CONSTRAINT uq_cliente_numero_identificacion UNIQUE (numero_identificacion)
);

CREATE TABLE dbo.solicitud_tarjeta (
    id_solicitud        BIGINT IDENTITY(1,1) NOT NULL,
    id_cliente          BIGINT        NOT NULL,
    tipo_tarjeta        VARCHAR(10)   NOT NULL,
    fecha_solicitud     DATETIME2(3)  NOT NULL CONSTRAINT df_solicitud_tarjeta_fecha_solicitud DEFAULT SYSDATETIME(),
    ingreso_mensual     DECIMAL(18,2) NOT NULL,
    estado              VARCHAR(10)   NOT NULL CONSTRAINT df_solicitud_tarjeta_estado DEFAULT 'PENDIENTE',
    fecha_decision      DATETIME2(3)  NULL,
    comentario_decision VARCHAR(500)  NULL,
    monto_aprobado      DECIMAL(18,2) NULL,
    usuario_decision    VARCHAR(100)  NULL,
    CONSTRAINT pk_solicitud_tarjeta PRIMARY KEY (id_solicitud),
    CONSTRAINT fk_solicitud_tarjeta_cliente FOREIGN KEY (id_cliente)
        REFERENCES dbo.cliente (id_cliente) ON DELETE CASCADE,
    CONSTRAINT ck_solicitud_tarjeta_tipo_tarjeta CHECK (tipo_tarjeta IN ('CLASICA','ORO','PLATINO','BLACK')),
    CONSTRAINT ck_solicitud_tarjeta_estado CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA')),
    CONSTRAINT ck_solicitud_tarjeta_ingreso_mensual CHECK (ingreso_mensual > 0),
    CONSTRAINT ck_solicitud_tarjeta_monto_aprobado CHECK (monto_aprobado IS NULL OR monto_aprobado > 0),
    CONSTRAINT ck_solicitud_tarjeta_coherencia_decision CHECK (
        (estado = 'PENDIENTE' AND fecha_decision IS NULL AND monto_aprobado IS NULL)
     OR (estado = 'APROBADA'  AND fecha_decision IS NOT NULL AND monto_aprobado IS NOT NULL)
     OR (estado = 'RECHAZADA' AND fecha_decision IS NOT NULL AND monto_aprobado IS NULL AND comentario_decision IS NOT NULL)
    )
);
CREATE INDEX ix_solicitud_tarjeta_id_cliente ON dbo.solicitud_tarjeta (id_cliente);
CREATE INDEX ix_solicitud_tarjeta_estado     ON dbo.solicitud_tarjeta (estado, fecha_solicitud);

CREATE TABLE dbo.tarjeta_credito (
    id_tarjeta     BIGINT IDENTITY(1,1) NOT NULL,
    numero_tarjeta VARCHAR(16)   NOT NULL,
    id_cliente     BIGINT        NOT NULL,
    id_solicitud   BIGINT        NOT NULL,
    tipo_tarjeta   VARCHAR(10)   NOT NULL,
    limite_credito DECIMAL(18,2) NOT NULL,
    saldo_actual   DECIMAL(18,2) NOT NULL CONSTRAINT df_tarjeta_credito_saldo_actual DEFAULT 0,
    fecha_emision  DATETIME2(3)  NOT NULL CONSTRAINT df_tarjeta_credito_fecha_emision DEFAULT SYSDATETIME(),
    CONSTRAINT pk_tarjeta_credito PRIMARY KEY (id_tarjeta),
    CONSTRAINT uq_tarjeta_credito_numero_tarjeta UNIQUE (numero_tarjeta),
    CONSTRAINT uq_tarjeta_credito_id_solicitud UNIQUE (id_solicitud),
    CONSTRAINT fk_tarjeta_credito_cliente FOREIGN KEY (id_cliente)
        REFERENCES dbo.cliente (id_cliente) ON DELETE NO ACTION,
    CONSTRAINT fk_tarjeta_credito_solicitud_tarjeta FOREIGN KEY (id_solicitud)
        REFERENCES dbo.solicitud_tarjeta (id_solicitud) ON DELETE CASCADE,
    CONSTRAINT ck_tarjeta_credito_numero_tarjeta CHECK (LEN(numero_tarjeta) = 16 AND numero_tarjeta NOT LIKE '%[^0-9]%'),
    CONSTRAINT ck_tarjeta_credito_tipo_tarjeta CHECK (tipo_tarjeta IN ('CLASICA','ORO','PLATINO','BLACK')),
    CONSTRAINT ck_tarjeta_credito_limite_credito CHECK (limite_credito > 0),
    CONSTRAINT ck_tarjeta_credito_saldo_actual CHECK (saldo_actual >= 0 AND saldo_actual <= limite_credito)
);
CREATE INDEX ix_tarjeta_credito_id_cliente ON dbo.tarjeta_credito (id_cliente);

CREATE TABLE dbo.consumo (
    id_consumo       BIGINT IDENTITY(1,1) NOT NULL,
    id_tarjeta       BIGINT        NOT NULL,
    monto            DECIMAL(18,2) NOT NULL,
    fecha            DATETIME2(3)  NOT NULL,
    descripcion      VARCHAR(250)  NOT NULL,
    fecha_registro   DATETIME2(3)  NOT NULL CONSTRAINT df_consumo_fecha_registro DEFAULT SYSDATETIME(),
    usuario_registro VARCHAR(100)  NULL,
    CONSTRAINT pk_consumo PRIMARY KEY (id_consumo),
    CONSTRAINT fk_consumo_tarjeta_credito FOREIGN KEY (id_tarjeta)
        REFERENCES dbo.tarjeta_credito (id_tarjeta) ON DELETE CASCADE,
    CONSTRAINT ck_consumo_monto CHECK (monto > 0)
);
CREATE INDEX ix_consumo_id_tarjeta ON dbo.consumo (id_tarjeta, fecha);

CREATE TABLE dbo.pago (
    -- idéntica a consumo: id_pago, id_tarjeta, monto, fecha, descripcion, fecha_registro, usuario_registro
    -- constraints: pk_pago, fk_pago_tarjeta_credito (ON DELETE CASCADE), ck_pago_monto (monto > 0),
    -- df_pago_fecha_registro; índice ix_pago_id_tarjeta (id_tarjeta, fecha)
);
```

### 2.3 Cascadas y el problema de "multiple cascade paths"
Si `tarjeta_credito` tuviera CASCADE hacia `cliente` **y además** hacia `solicitud_tarjeta` (que a su vez tiene CASCADE hacia `cliente`), habría dos caminos de cascada desde `cliente` hasta `tarjeta_credito`, y SQL Server rechaza el CREATE. Solución:
- `cliente → solicitud_tarjeta`: **CASCADE**
- `solicitud_tarjeta → tarjeta_credito` (`id_solicitud`): **CASCADE** (la tarjeta nace de la solicitud)
- `cliente → tarjeta_credito` (`id_cliente`): **NO ACTION** (la columna se conserva para consultar por cliente y para la integridad)
- `tarjeta_credito → consumo/pago`: **CASCADE**

Queda un solo camino de cascada. Para no depender del orden interno en que SQL Server revisa la FK NO ACTION, **el backend borra en orden explícito dentro de una sola transacción** (ver 4.6): primero las tarjetas del cliente (la BD borra en cascada sus consumos y pagos) y después el cliente (la BD borra en cascada sus solicitudes).

### 2.4 `01_crear_base_datos.sql` (se ejecuta en master)
- `IF DB_ID('GestionTarjetasCredito') IS NULL CREATE DATABASE GestionTarjetasCredito COLLATE Modern_Spanish_CI_AS;` y luego `GO`.
- Si no existe en `sys.server_principals`: `CREATE LOGIN usuario_tarjetas WITH PASSWORD = '$(CLAVE_USUARIO_APLICACION)', CHECK_POLICY = OFF;`
- `USE GestionTarjetasCredito;` Si no existe en `sys.database_principals`: `CREATE USER usuario_tarjetas FOR LOGIN usuario_tarjetas;` y luego `ALTER ROLE db_datareader ADD MEMBER usuario_tarjetas; ALTER ROLE db_datawriter ADD MEMBER usuario_tarjetas;`
- El esquema lo crea `sa`; la aplicación se conecta como `usuario_tarjetas` (privilegio mínimo).

### 2.5 `inicializar-base-datos.sh` (LF, `set -euo pipefail`)
```bash
SQLCMD=/opt/mssql-tools18/bin/sqlcmd
CONEXION=(-S "$SERVIDOR_BASE_DATOS" -U sa -P "$MSSQL_SA_PASSWORD" -C -b -I)
# esperar hasta 30 intentos x 2s con: "${CONEXION[@]}" -Q "SELECT 1"
"$SQLCMD" "${CONEXION[@]}" -i /scripts/01_crear_base_datos.sql -v CLAVE_USUARIO_APLICACION="$CLAVE_USUARIO_APLICACION"
"$SQLCMD" "${CONEXION[@]}" -d GestionTarjetasCredito -i /scripts/02_crear_esquema.sql
"$SQLCMD" "${CONEXION[@]}" -d GestionTarjetasCredito -i /scripts/03_datos_semilla.sql
echo "Base de datos inicializada correctamente"
```
`-I` activa QUOTED_IDENTIFIER y `-C` confía en el certificado. Los scripts deben poder ejecutarse varias veces sin error, porque el volumen persiste entre reinicios.

---

## 3. Entidades JPA (paquete `entity`)

Clases simples con Lombok: `@NoArgsConstructor` (requerido por JPA), `@Getter` en la clase y `@Setter` solo en los campos que se modifican desde fuera (ver sección 10). Todas usan `@Table(name="...", schema="dbo")` y `@Column(name="...")` explícitos.

| Clase | Tabla | Campos Java → columna |
|---|---|---|
| `Cliente` | `cliente` | `Long id`→id_cliente (`@GeneratedValue(strategy=IDENTITY)`), `String nombre`, `String apellido`, `String numeroIdentificacion`, `LocalDate fechaNacimiento`, `String direccion`, `String correoElectronico`, `String telefono`, `LocalDateTime fechaCreacion` (`updatable=false`), `LocalDateTime fechaActualizacion`. **Sin colecciones.** `@PrePersist` asigna fechaCreacion=now; `@PreUpdate` asigna fechaActualizacion=now. |
| `SolicitudTarjeta` | `solicitud_tarjeta` | `Long id`→id_solicitud, `@ManyToOne(fetch=LAZY, optional=false) @JoinColumn(name="id_cliente") Cliente cliente`, `@Enumerated(STRING) TipoTarjeta tipoTarjeta`, `LocalDateTime fechaSolicitud`, `BigDecimal ingresoMensual` (`precision=18, scale=2`), `@Enumerated(STRING) EstadoSolicitud estado`, `LocalDateTime fechaDecision`, `String comentarioDecision`, `BigDecimal montoAprobado`, `String usuarioDecision`, `@OneToOne(mappedBy="solicitud", fetch=LAZY) TarjetaCredito tarjeta` (lado inverso, solo lectura). `@PrePersist`: si fechaSolicitud es null → now; si estado es null → PENDIENTE. |
| `TarjetaCredito` | `tarjeta_credito` | `Long id`→id_tarjeta, `String numeroTarjeta`, `@ManyToOne(LAZY, optional=false) @JoinColumn(name="id_cliente") Cliente cliente`, `@OneToOne(LAZY, optional=false) @JoinColumn(name="id_solicitud", unique=true) SolicitudTarjeta solicitud`, `@Enumerated(STRING) TipoTarjeta tipoTarjeta`, `BigDecimal limiteCredito`, `BigDecimal saldoActual`, `LocalDateTime fechaEmision`. Métodos de dominio: `BigDecimal obtenerCreditoDisponible()` (límite − saldo). |
| `Consumo` | `consumo` | `Long id`→id_consumo, `@ManyToOne(LAZY, optional=false) @JoinColumn(name="id_tarjeta") TarjetaCredito tarjeta`, `BigDecimal monto`, `LocalDateTime fecha`, `String descripcion`, `LocalDateTime fechaRegistro`, `String usuarioRegistro`. `@PrePersist` asigna fechaRegistro=now. |
| `Pago` | `pago` | Igual que Consumo, con `id`→id_pago. |

**Enums** (los valores de `name()` coinciden exactamente con los CHECK):
```java
public enum TipoTarjeta {
    CLASICA("Clásica", 3000.00, "4100"), ORO("Oro", 8000.00, "4200"), PLATINO("Platino", 15000.00, "4300"), BLACK("Black", 30000.00, "4400");  // descripción, ingreso mínimo, prefijo
    private final String descripcion; private final String prefijoNumeroTarjeta;
    // constructor + getDescripcion() + getPrefijoNumeroTarjeta()
}
public enum EstadoSolicitud { PENDIENTE, APROBADA, RECHAZADA }
```
Correspondencia de tipos: `BIGINT`↔`Long`, `DECIMAL(18,2)`↔`BigDecimal`, `DATE`↔`LocalDate`, `DATETIME2`↔`LocalDateTime`, `VARCHAR`↔`String`. No usar `@Version`.

### Repositorios (Spring Data JPA)
```java
ClienteRepository extends JpaRepository<Cliente, Long>
  boolean existsByNumeroIdentificacion(String numeroIdentificacion);
  boolean existsByNumeroIdentificacionAndIdNot(String numeroIdentificacion, Long id);
  @Query("select c from Cliente c where c.nombre like concat('%', :texto, '%') or c.apellido like concat('%', :texto, '%') or c.numeroIdentificacion like concat('%', :texto, '%') order by c.apellido, c.nombre")
  List<Cliente> buscarPorTexto(@Param("texto") String texto);
  List<Cliente> findAllByOrderByApellidoAscNombreAsc();

SolicitudTarjetaRepository extends JpaRepository<SolicitudTarjeta, Long>
  @Query("select s from SolicitudTarjeta s join fetch s.cliente left join fetch s.tarjeta where s.cliente.id = :idCliente order by s.fechaSolicitud desc")
  List<SolicitudTarjeta> buscarPorCliente(@Param("idCliente") Long idCliente);
  @Query("select s from SolicitudTarjeta s join fetch s.cliente left join fetch s.tarjeta where (:estado is null or s.estado = :estado) order by s.fechaSolicitud asc")
  List<SolicitudTarjeta> buscarPorEstado(@Param("estado") EstadoSolicitud estado);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from SolicitudTarjeta s where s.id = :id")
  Optional<SolicitudTarjeta> buscarPorIdConBloqueo(@Param("id") Long id);
  boolean existsByClienteIdAndTipoTarjetaAndEstado(Long idCliente, TipoTarjeta tipoTarjeta, EstadoSolicitud estado);

TarjetaCreditoRepository extends JpaRepository<TarjetaCredito, Long>
  List<TarjetaCredito> findByClienteIdOrderByFechaEmisionDesc(Long idCliente);
  boolean existsByNumeroTarjeta(String numeroTarjeta);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from TarjetaCredito t where t.id = :id")
  Optional<TarjetaCredito> buscarPorIdConBloqueo(@Param("id") Long id);
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from TarjetaCredito t where t.cliente.id = :idCliente")
  int eliminarPorIdCliente(@Param("idCliente") Long idCliente);

ConsumoRepository: List<Consumo> findByTarjetaIdOrderByFechaDesc(Long idTarjeta);
PagoRepository:    List<Pago>    findByTarjetaIdOrderByFechaDesc(Long idTarjeta);
```

---

## 4. Reglas de negocio

Todos los métodos públicos de los servicios llevan `@Transactional` (los de consulta con `readOnly = true`) y **devuelven DTOs**. Los controladores nunca reciben ni devuelven entidades. `open-in-view=false`.

### 4.1 ClienteService
- `listar(String busqueda)`: si la búsqueda está vacía o en blanco usa `findAllByOrderByApellidoAscNombreAsc`; si no, `buscarPorTexto(busqueda.trim())`.
- `obtener(id)`: si no existe lanza `RecursoNoEncontradoException("No se encontró el cliente con id " + id)`.
- `crear` / `actualizar`: aplicar `trim()` a todos los textos.
  - Identificación duplicada → `RecursoDuplicadoException("Ya existe un cliente con el número de identificación X")` (409).
  - Menor de 18 años → `ReglaNegocioException("El cliente debe ser mayor de edad (18 años o más)")` (422).
  - Al actualizar, la unicidad se verifica excluyendo al propio cliente.
- `eliminar(id)`: ver 4.6.

### 4.2 SolicitudService
- `crear(SolicitudPeticion)`:
  - Si el cliente no existe → 404.
  - Si ya hay una solicitud PENDIENTE del mismo tipo para ese cliente → 422 "El cliente ya tiene una solicitud pendiente de tarjeta {descripcion}".
  - Estado = PENDIENTE y fechaSolicitud = now, ambos asignados por el servidor (el cliente no los envía).
- `listarPorCliente(idCliente)`: 404 si el cliente no existe.
- `listarPorEstado(EstadoSolicitud estadoOpcional)`: devuelve todas si no se indica estado. Orden: la más antigua primero.
- `aprobar(id, AprobacionPeticion, String usuario)`:
  1. Obtiene la solicitud con `buscarPorIdConBloqueo` (404 si no existe).
  2. Si el estado no es PENDIENTE → 422 "Solo se pueden aprobar solicitudes en estado PENDIENTE. Estado actual: APROBADA".
  3. Asigna estado=APROBADA, fechaDecision=now, montoAprobado, comentarioDecision (opcional) y usuarioDecision=usuario.
  4. Llama a `tarjetaService.emitirTarjeta(solicitud)`, todo en la misma transacción.
  5. Devuelve SolicitudRespuesta con idTarjeta y numeroTarjetaEnmascarado.
- `rechazar(id, RechazoPeticion, usuario)`: mismo bloqueo y validación de PENDIENTE. Asigna estado=RECHAZADA, fechaDecision, comentarioDecision (obligatorio) y usuarioDecision. montoAprobado queda en null.
- `usuario` = `authentication.getName()`, que corresponde al `preferred_username` del token.

### 4.3 TarjetaService
- `emitirTarjeta(SolicitudTarjeta)` (paquete-privado o público, sin endpoint propio):
  - cliente = solicitud.cliente, tipo = solicitud.tipoTarjeta, limiteCredito = montoAprobado, saldoActual = `BigDecimal.ZERO` (escala 2), fechaEmision = now.
  - Número: hasta 10 intentos de `generadorNumeroTarjeta.generar(tipo)` hasta que `!existsByNumeroTarjeta`. Si se agotan los intentos, `IllegalStateException` (500).
  - El UNIQUE de la BD es la última barrera.
- `obtener(idTarjeta)` y `listarPorCliente(idCliente)` (404 si el cliente no existe).

### 4.4 GeneradorNumeroTarjeta (`@Component`)
- `String generar(TipoTarjeta tipo)` = prefijo (4 dígitos: CLASICA 4100, ORO 4200, PLATINO 4300, BLACK 4400) + 11 dígitos aleatorios (`SecureRandom`) + 1 dígito verificador **Luhn**.
- Total: 16 dígitos.
- `static boolean esNumeroValidoLuhn(String numero)` sirve para las pruebas.

### 4.5 MovimientoTarjetaService (concurrencia del saldo)
Se usa **bloqueo pesimista** (`buscarPorIdConBloqueo`, que en SQL Server genera `UPDLOCK, HOLDLOCK, ROWLOCK`) dentro de una transacción.
- `registrarConsumo(idTarjeta, MovimientoPeticion, usuario)`:
  - Bloquea la tarjeta (404 si no existe) y calcula `nuevoSaldo = saldoActual + monto`.
  - Si `nuevoSaldo.compareTo(limiteCredito) > 0` → 422 "El consumo de Q {monto} excede el crédito disponible de Q {disponible}".
  - Si no, guarda el consumo (fecha = peticion.fecha o now) y actualiza el saldo.
- `registrarPago(...)`:
  - Si `saldoActual` es 0 → 422 "La tarjeta no tiene saldo pendiente de pago".
  - Si `monto > saldoActual` → 422 "El pago de Q {monto} excede el saldo actual de Q {saldo}". **No se permite saldo negativo.**
  - Si no, `saldo = saldo − monto`.
- `listarConsumos(idTarjeta)` / `listarPagos(idTarjeta)`: 404 si la tarjeta no existe. Orden: fecha descendente.
- Los montos se formatean en los mensajes con `setScale(2)` (`toPlainString()`).
- El CHECK `ck_tarjeta_credito_saldo_actual` actúa como defensa final.

### 4.6 Eliminación de cliente (ClienteService.eliminar, solo ADMINISTRADOR)
```java
@Transactional
public void eliminar(Long id) {
    Cliente cliente = buscarEntidad(id);                  // 404
    tarjetaCreditoRepository.eliminarPorIdCliente(id);    // BD cascada → consumo, pago
    clienteRepository.delete(cliente);                    // BD cascada → solicitud_tarjeta
}
```

---

## 5. API REST

### 5.1 Convenciones generales
- Prefijo `/gestion-tarjetas`. Cuerpos en JSON con campos en camelCase en español.
- `LocalDate` → `"2026-10-09"`. `LocalDateTime` → `"2026-10-09T14:30:00"` (ISO sin zona, hora de Guatemala).
- Dinero → número JSON (`1500.00`).
- Enums → `"CLASICA"`, `"PENDIENTE"`, etc.
- Listas sin paginar.
- `POST` responde **201** con el cuerpo y la cabecera `Location`. `DELETE` responde **204**.
- Los DTO son **`record`** de Java 17.

### 5.2 DTOs

**ClientePeticion**
| campo | tipo | validaciones (mensajes en español) |
|---|---|---|
| nombre | String | @NotBlank, @Size(max=100) |
| apellido | String | @NotBlank, @Size(max=100) |
| numeroIdentificacion | String | @NotBlank, @Pattern(`^[A-Za-z0-9-]{5,20}$`) |
| fechaNacimiento | LocalDate | @NotNull, @Past |
| direccion | String | @NotBlank, @Size(max=250) |
| correoElectronico | String | @NotBlank, @Email, @Size(max=150) |
| telefono | String | @NotBlank, @Pattern(`^[0-9+()\- ]{8,20}$`) |

**ClienteRespuesta**: `id, nombre, apellido, nombreCompleto, numeroIdentificacion, fechaNacimiento, direccion, correoElectronico, telefono, fechaCreacion, fechaActualizacion`

**SolicitudPeticion**: `idCliente` (Long, @NotNull @Positive), `tipoTarjeta` (TipoTarjeta, @NotNull), `ingresoMensual` (BigDecimal, @NotNull @DecimalMin("0.01") @Digits(integer=16, fraction=2))

**AprobacionPeticion**: `montoAprobado` (BigDecimal, @NotNull @DecimalMin("0.01") @Digits(16,2)), `comentarioDecision` (String, opcional, @Size(max=500))

**RechazoPeticion**: `comentarioDecision` (String, @NotBlank @Size(max=500))

**SolicitudRespuesta**: `id, idCliente, nombreCliente, numeroIdentificacionCliente, tipoTarjeta, descripcionTipoTarjeta, fechaSolicitud, ingresoMensual, estado, fechaDecision, comentarioDecision, montoAprobado, usuarioDecision, idTarjeta, numeroTarjetaEnmascarado` (los dos últimos son null si no hay tarjeta)

**TarjetaRespuesta**: `id, numeroTarjeta, numeroTarjetaEnmascarado` (`"**** **** **** 9015"`), `idCliente, nombreCliente, idSolicitud, tipoTarjeta, descripcionTipoTarjeta, limiteCredito, saldoActual, creditoDisponible, fechaEmision`

**MovimientoPeticion** (para consumo y pago): `monto` (BigDecimal, @NotNull @DecimalMin("0.01") @Digits(16,2)), `descripcion` (String, @NotBlank @Size(max=250)), `fecha` (LocalDateTime, opcional, @PastOrPresent; si es null se usa now)

**MovimientoRespuesta**: `id, idTarjeta, tipoMovimiento` (`"CONSUMO"` | `"PAGO"`), `monto, fecha, descripcion, fechaRegistro, usuarioRegistro`

**ErrorRespuesta**: `fechaHora` (LocalDateTime), `estado` (int), `error` (String, reason phrase), `mensaje` (String), `ruta` (String), `errores` (`Map<String,String>`, **siempre presente**, `{}` si está vacío)
```json
{ "fechaHora":"2026-10-09T10:15:00", "estado":400, "error":"Bad Request",
  "mensaje":"La petición contiene datos inválidos", "ruta":"/gestion-tarjetas/clientes",
  "errores":{ "correoElectronico":"El correo electrónico no tiene un formato válido" } }
```

### 5.3 Endpoints
Roles: **A** = ADMINISTRADOR, **O** = OPERADOR.

| Método | Ruta | Cuerpo | Respuesta | Códigos | Rol |
|---|---|---|---|---|---|
| GET | `/gestion-tarjetas/clientes?busqueda=` | – | `ClienteRespuesta[]` | 200 | A, O |
| GET | `/gestion-tarjetas/clientes/{idCliente}` | – | `ClienteRespuesta` | 200, 404 | A, O |
| POST | `/gestion-tarjetas/clientes` | ClientePeticion | `ClienteRespuesta` | 201, 400, 409, 422 | A, O |
| PUT | `/gestion-tarjetas/clientes/{idCliente}` | ClientePeticion | `ClienteRespuesta` | 200, 400, 404, 409, 422 | A, O |
| DELETE | `/gestion-tarjetas/clientes/{idCliente}` | – | – | 204, 404 | **A** |
| GET | `/gestion-tarjetas/clientes/{idCliente}/solicitudes` | – | `SolicitudRespuesta[]` | 200, 404 | A, O |
| GET | `/gestion-tarjetas/clientes/{idCliente}/tarjetas` | – | `TarjetaRespuesta[]` | 200, 404 | A, O |
| GET | `/gestion-tarjetas/solicitudes?estado=PENDIENTE` (opcional) | – | `SolicitudRespuesta[]` | 200, 400 | A, O |
| GET | `/gestion-tarjetas/solicitudes/{idSolicitud}` | – | `SolicitudRespuesta` | 200, 404 | A, O |
| POST | `/gestion-tarjetas/solicitudes` | SolicitudPeticion | `SolicitudRespuesta` | 201, 400, 404, 422 | A, O |
| PUT | `/gestion-tarjetas/solicitudes/{idSolicitud}/aprobar` | AprobacionPeticion | `SolicitudRespuesta` | 200, 400, 404, 422 | **A** |
| PUT | `/gestion-tarjetas/solicitudes/{idSolicitud}/rechazar` | RechazoPeticion | `SolicitudRespuesta` | 200, 400, 404, 422 | **A** |
| GET | `/gestion-tarjetas/tarjetas/{idTarjeta}` | – | `TarjetaRespuesta` | 200, 404 | A, O |
| GET | `/gestion-tarjetas/tarjetas/{idTarjeta}/consumos` | – | `MovimientoRespuesta[]` | 200, 404 | A, O |
| POST | `/gestion-tarjetas/tarjetas/{idTarjeta}/consumos` | MovimientoPeticion | `MovimientoRespuesta` | 201, 400, 404, 422 | A, O |
| GET | `/gestion-tarjetas/tarjetas/{idTarjeta}/pagos` | – | `MovimientoRespuesta[]` | 200, 404 | A, O |
| POST | `/gestion-tarjetas/tarjetas/{idTarjeta}/pagos` | MovimientoPeticion | `MovimientoRespuesta` | 201, 400, 404, 422 | A, O |
| PUT | `/gestion-tarjetas/tarjetas/{idTarjeta}/bloquear` | BloqueoTarjetaPeticion | `TarjetaRespuesta` | 200, 400, 404, 422 | A, O |
| PUT | `/gestion-tarjetas/tarjetas/{idTarjeta}/desbloquear` | – | `TarjetaRespuesta` | 200, 404, 422 | A |
| GET | `/gestion-tarjetas/catalogos/tipos-tarjeta` | – | `CatalogoTiposTarjetaRespuesta` | 200 | A, O |

Además, todo `/gestion-tarjetas/**` puede responder 401 (sin token o token inválido) y 403 (rol insuficiente). Las rutas públicas son: `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`.

Controladores:
- `ClienteController` maneja `/gestion-tarjetas/clientes/**`, incluidas las subrutas solicitudes y tarjetas (delega en SolicitudService/TarjetaService).
- `SolicitudController` maneja `/gestion-tarjetas/solicitudes/**`.
- `TarjetaController` maneja `/gestion-tarjetas/tarjetas/**` (delega en TarjetaService y MovimientoTarjetaService).
- El controlador obtiene el usuario con el parámetro `Authentication autenticacion`.

### 5.4 Mapeo de excepciones (`ManejadorGlobalExcepciones`, `@RestControllerAdvice`)
| Excepción | HTTP | mensaje |
|---|---|---|
| MethodArgumentNotValidException | 400 | "La petición contiene datos inválidos" + `errores` (campo → primer mensaje) |
| HttpMessageNotReadableException | 400 | "El cuerpo de la petición no es válido o tiene un formato incorrecto" |
| MethodArgumentTypeMismatchException | 400 | "El parámetro '{nombre}' tiene un valor inválido" |
| ConstraintViolationException | 400 | "La petición contiene datos inválidos" + errores |
| RecursoNoEncontradoException | 404 | mensaje de la excepción |
| RecursoDuplicadoException | 409 | mensaje de la excepción |
| DataIntegrityViolationException | 409 | "La operación viola una restricción de integridad de los datos" |
| ReglaNegocioException | **422** | mensaje de la excepción |
| AccessDeniedException | 403 | "No tiene permisos para realizar esta operación" |
| Exception | 500 | "Ocurrió un error interno. Intente nuevamente más tarde." (se registra con log.error) |

Las tres excepciones propias extienden `RuntimeException(String mensaje)`.

---

## 6. Seguridad

### 6.1 Keycloak (`keycloak/realm-tarjetas.json`)
- Realm `tarjetas`, `enabled: true`, `sslRequired: "none"`, `registrationAllowed: false`, `accessTokenLifespan: 900`.
- `roles.realm`: `ADMINISTRADOR` y `OPERADOR`, cada uno con su descripción.
- Cliente `frontend-tarjetas`:
  - `publicClient: true`, `standardFlowEnabled: true`, `directAccessGrantsEnabled: true` (solo para pruebas con curl y Swagger en la demo), `implicitFlowEnabled: false`, `protocol: "openid-connect"`.
  - `redirectUris: ["http://localhost:5173/*","http://localhost:3000/*"]`, `webOrigins: ["http://localhost:5173","http://localhost:3000"]`.
  - `attributes: {"pkce.code.challenge.method":"S256","post.logout.redirect.uris":"http://localhost:5173/*##http://localhost:3000/*"}`.
- Usuarios (todos con `enabled: true`, `emailVerified: true`, `requiredActions: []` y `credentials: [{type:"password", value, temporary:false}]`):
  - `admin` / `admin123`: firstName "Administrador", lastName "Sistema", email `admin@tarjetas.example.com`, `realmRoles: ["ADMINISTRADOR"]`.
  - `operador` / `operador123`: firstName "Operador", lastName "Sistema", email `operador@tarjetas.example.com`, `realmRoles: ["OPERADOR"]`.
- Los usuarios llevan nombre, apellido y correo para que Keycloak 25 (user profile) no exija "Actualizar perfil" en el primer inicio de sesión.

### 6.2 Issuer dentro de Docker frente a ejecución local
- El navegador obtiene el token en `http://localhost:8180`, así que el token trae `iss = http://localhost:8180/realms/tarjetas`.
- Keycloak usa `KC_HOSTNAME=http://localhost:8180` y `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`, de modo que el issuer siempre es localhost:8180, aunque se acceda internamente por `keycloak:8080`.
- El backend **no usa discovery** (`issuer-uri` de Spring). Define su propio `JwtDecoder`:
```java
@Bean JwtDecoder decodificadorJwt(@Value("${seguridad.keycloak.jwk-set-uri}") String jwkSetUri,
                                  @Value("${seguridad.keycloak.issuer-esperado}") String issuerEsperado) {
    NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerEsperado));
    return decodificador;
}
```
- En Docker: `KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/tarjetas/protocol/openid-connect/certs` y `KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/tarjetas`. En local ambas variables apuntan a localhost:8180 (son los valores por defecto).
- La audiencia no se valida.

### 6.3 Configuración en `ConfiguracionSeguridad`
- Sesiones stateless, CSRF deshabilitado, `cors(Customizer.withDefaults())`.
- `oauth2ResourceServer(jwt -> jwt.jwtAuthenticationConverter(convertidorRolesKeycloak))`.
- `authenticationEntryPoint` y `accessDeniedHandler` = `ManejadorErroresSeguridad`, que escribe ErrorRespuesta en JSON (401: "Debe iniciar sesión para acceder a este recurso"; 403: el mensaje de la tabla 5.4).
- Reglas por URL, centralizadas aquí en este orden (no se usa @PreAuthorize):
```
permitAll: /swagger-ui.html, /swagger-ui/**, /v3/api-docs/**, /actuator/health
OPTIONS /** permitAll
DELETE /gestion-tarjetas/clientes/**              -> hasRole("ADMINISTRADOR")
PUT    /gestion-tarjetas/solicitudes/*/aprobar     -> hasRole("ADMINISTRADOR")
PUT    /gestion-tarjetas/solicitudes/*/rechazar    -> hasRole("ADMINISTRADOR")
/gestion-tarjetas/**                               -> hasAnyRole("ADMINISTRADOR","OPERADOR")
anyRequest                            -> authenticated
```
- `ConvertidorRolesKeycloak implements Converter<Jwt, AbstractAuthenticationToken>`: lee `realm_access.roles` (lista vacía si no existe), crea `SimpleGrantedAuthority("ROLE_" + rol)` y devuelve `new JwtAuthenticationToken(jwt, autoridades, jwt.getClaimAsString("preferred_username"))`.
- Bean `CorsConfigurationSource`:
  - Orígenes desde `seguridad.cors.origenes-permitidos` (separados por coma).
  - Métodos GET, POST, PUT, DELETE y OPTIONS. Cabeceras `Authorization` y `Content-Type`.
  - Expone `Location`. `allowCredentials=false`, `maxAge=3600`.
- `ConfiguracionOpenApi`: título "API Gestión de Tarjetas de Crédito – CHN", security scheme `bearerAuth` (HTTP bearer JWT) aplicado globalmente.

### 6.4 `application.yml`
```yaml
server:
  port: 8080
spring:
  application:
    name: gestion-tarjetas-credito
  datasource:
    url: "${URL_BASE_DATOS:jdbc:sqlserver://localhost:1433;databaseName=GestionTarjetasCredito;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=false}"
    username: ${USUARIO_BASE_DATOS:usuario_tarjetas}
    password: ${CLAVE_BASE_DATOS:Aplicacion_Tarjetas2026}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    show-sql: false
  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: America/Guatemala
seguridad:
  keycloak:
    issuer-esperado: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/tarjetas}
    jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://localhost:8180/realms/tarjetas/protocol/openid-connect/certs}
  cors:
    origenes-permitidos: ${CORS_ORIGENES_PERMITIDOS:http://localhost:5173,http://localhost:3000}
management:
  endpoints:
    web:
      exposure:
        include: health
springdoc:
  swagger-ui:
    path: /swagger-ui.html
```
El `main` ejecuta `TimeZone.setDefault(TimeZone.getTimeZone("America/Guatemala"))` antes de `SpringApplication.run`.

### 6.5 Construcción del backend
- `pom.xml`: parent `spring-boot-starter-parent` **3.3.5**, `java.version` 17.
- Dependencias: starters web, data-jpa, validation, oauth2-resource-server y actuator; `com.microsoft.sqlserver:mssql-jdbc` (runtime, versión gestionada por Boot); `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0`; para pruebas, spring-boot-starter-test y spring-security-test.
- Maven Wrapper: como `mvn` no está instalado, se genera con
  `docker run --rm -v "$PWD":/app -w /app maven:3.9.9-eclipse-temurin-17 mvn -N wrapper:wrapper -Dmaven=3.9.9` (ejecutado dentro de `backend/`).
- `Dockerfile`:
  - Etapa 1 `maven:3.9.9-eclipse-temurin-17`: copia pom, ejecuta `mvn -B dependency:go-offline`, copia src y ejecuta `mvn -B -DskipTests package`.
  - Etapa 2 `eclipse-temurin:17-jre`: instala `curl` (apt, para el healthcheck), `ENV TZ=America/Guatemala`, copia `target/*.jar` como `app.jar`, `EXPOSE 8080`, `ENTRYPOINT ["java","-jar","/app/app.jar"]`.
- Pruebas unitarias (JUnit 5 + Mockito, sin contexto Spring):
  - Luhn y prefijo del generador.
  - Aprobar una solicitud no pendiente lanza ReglaNegocio.
  - Aprobar crea la tarjeta.
  - Consumo que excede el límite lanza 422.
  - Pago mayor al saldo lanza 422.
  - Un consumo válido actualiza el saldo.

---

## 7. Frontend (Vue 3 + Vite, JavaScript, Composition API con `<script setup>`)

> **Nota:** esta sección describe la primera versión del frontend. La interfaz se rediseñó según el diseño de Figma (barra lateral, Resumen, Tarjetas emitidas, revisión de solicitudes como página, modales de movimiento y bloqueo). La estructura vigente de vistas, componentes y composables está en [MANUAL_TECNICO.md](MANUAL_TECNICO.md#21-frontend), sección 21 (Frontend). La API ya no devuelve `numeroTarjeta` completo; solo `numeroTarjetaEnmascarado`.

**Versiones:** vue ^3.4, vue-router ^4.4, axios ^1.7, keycloak-js **25.0.6** (igual a la versión del servidor), vite ^5.4, @vitejs/plugin-vue ^5.1. Sin librería de UI: CSS propio en `estilos/principal.css` con variables CSS y estética sobria (verde institucional `#00653A`). Puerto de desarrollo 5173.

**Variables de entorno** (`.env.example` y `.env.development` tienen los mismos valores):
```
VITE_URL_API=http://localhost:8080/gestion-tarjetas
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=tarjetas
VITE_KEYCLOAK_CLIENTE_ID=frontend-tarjetas
```

**`autenticacion/servicioAutenticacion.js`** exporta:
- `inicializarAutenticacion()`: `new Keycloak({url, realm, clientId})` y `init({ onLoad:'login-required', pkceMethod:'S256', checkLoginIframe:false })`. Registra `onTokenExpired` que llama a `updateToken(30)`.
- `obtenerTokenActualizado()`: `await keycloak.updateToken(30)`; si falla, llama a `keycloak.login()`. Devuelve `keycloak.token`.
- `obtenerNombreUsuario()` (preferred_username), `obtenerRoles()` (`tokenParsed.realm_access.roles`), `tieneRol(rol)`, `esAdministrador()`, `cerrarSesion()` (`logout({redirectUri: window.location.origin})`).

**`main.js`**: `await inicializarAutenticacion()` y después `createApp(App).use(router).mount('#app')`.

**`servicios/clienteHttp.js`**:
- `axios.create({ baseURL: import.meta.env.VITE_URL_API, headers: {'Content-Type':'application/json'} })`.
- Interceptor de request: `config.headers.Authorization = 'Bearer ' + await obtenerTokenActualizado()`.
- Interceptor de response: si el estado es 401, llama a `keycloak.login()`; en todos los casos rechaza el error.

**Servicios** (todas las funciones devuelven `respuesta.data`):
- `clientesServicio.js`: `listarClientes(busqueda)`, `obtenerCliente(idCliente)`, `crearCliente(datos)`, `actualizarCliente(idCliente, datos)`, `eliminarCliente(idCliente)`, `listarSolicitudesDeCliente(idCliente)`, `listarTarjetasDeCliente(idCliente)`
- `solicitudesServicio.js`: `listarSolicitudes(estado)`, `obtenerSolicitud(idSolicitud)`, `crearSolicitud(datos)`, `aprobarSolicitud(idSolicitud, datos)`, `rechazarSolicitud(idSolicitud, datos)`
- `tarjetasServicio.js`: `obtenerTarjeta(idTarjeta)`, `listarConsumos(idTarjeta)`, `registrarConsumo(idTarjeta, datos)`, `listarPagos(idTarjeta)`, `registrarPago(idTarjeta, datos)`

**Utilidades:**
- `errores.js`: `obtenerMensajeError(error)` devuelve `error.response?.data?.mensaje ?? 'No fue posible conectar con el servidor'`; `obtenerErroresDeCampos(error)` devuelve `error.response?.data?.errores ?? {}`. Los formularios muestran el error de cada campo debajo de su input y el mensaje general en `MensajeAlerta`.
- `formatos.js`: `formatearMoneda(valor)` con `Intl.NumberFormat('es-GT',{style:'currency',currency:'GTQ'})`, `formatearFecha(texto)` y `formatearFechaHora(texto)`.
- `constantes.js`: `TIPOS_TARJETA = [{valor:'CLASICA',etiqueta:'Clásica'},{valor:'ORO',etiqueta:'Oro'},{valor:'PLATINO',etiqueta:'Platino'}]`, `ESTADOS_SOLICITUD = [{valor:'PENDIENTE',etiqueta:'Pendiente'},{valor:'APROBADA',etiqueta:'Aprobada'},{valor:'RECHAZADA',etiqueta:'Rechazada'}]`, `ROLES = {ADMINISTRADOR:'ADMINISTRADOR', OPERADOR:'OPERADOR'}`.

**Rutas** (`router/index.js`, `createWebHistory`):
| ruta | name | vista |
|---|---|---|
| `/` | – | redirect a `/clientes` |
| `/clientes` | `lista-clientes` | ListaClientesVista: buscador, tabla (nombre completo, identificación, correo, teléfono) y acciones Ver, Editar y Eliminar (solo admin, con VentanaModal de confirmación que advierte que se borrarán sus solicitudes, tarjetas y movimientos) |
| `/clientes/nuevo` | `nuevo-cliente` | FormularioClienteVista |
| `/clientes/:idCliente/editar` | `editar-cliente` | FormularioClienteVista (en modo edición, según el parámetro) |
| `/clientes/:idCliente` | `detalle-cliente` | DetalleClienteVista: datos de contacto y dos pestañas. "Solicitudes" muestra una tabla con EtiquetaEstado, el botón "Nueva solicitud" (abre un modal con FormularioSolicitud) y, para el admin, Aprobar/Rechazar en las pendientes. "Tarjetas" muestra una tabla (número enmascarado, tipo, límite, saldo, disponible) con un enlace al detalle |
| `/solicitudes` | `bandeja-solicitudes` | BandejaSolicitudesVista: filtro de estado (por defecto PENDIENTE, con opción "Todas") y, para el admin, Aprobar/Rechazar con FormularioDecision en un modal. El operador ve la tabla sin botones |
| `/tarjetas/:idTarjeta` | `detalle-tarjeta` | DetalleTarjetaVista: resumen (número, titular, tipo, límite, saldo, disponible y una barra de uso), dos FormularioMovimiento (consumo y pago) y dos TablaMovimientos. Después de registrar un movimiento recarga la tarjeta y las listas |
| `/:pathMatch(.*)*` | `no-encontrado` | NoEncontradoVista |

**Componentes:**
- `BarraNavegacion`: enlaces Clientes y Solicitudes, nombre del usuario, rol y botón "Cerrar sesión".
- `VentanaModal`: props `visible` y `titulo`; emite `cerrar`; usa slot.
- `EtiquetaEstado`: prop `estado`; badge de color según el estado.
- `FormularioSolicitud`: prop `idCliente`; emite `guardada`.
- `FormularioDecision`: props `solicitud` y `tipoDecision` (`'APROBAR'|'RECHAZAR'`); emite `decidida`. Al aprobar pide monto (sugerencia visual: el ingreso mensual × 2) y comentario opcional; al rechazar pide comentario obligatorio.
- `FormularioMovimiento`: props `idTarjeta` y `tipoMovimiento` (`'CONSUMO'|'PAGO'`); emite `registrado`.
- `TablaMovimientos`: props `titulo` y `movimientos`.

La visibilidad de acciones por rol usa `esAdministrador()`. Ocultar botones es solo experiencia de usuario: el backend es quien hace cumplir los permisos.

**Docker (frontend):**
- Etapa `node:20-alpine` con `ARG`/`ENV` para las 4 variables `VITE_*`, `npm ci` (o `npm install` si no hay lockfile) y `npm run build`.
- Etapa `nginx:1.27-alpine` que copia `dist` a `/usr/share/nginx/html` y `nginx.conf` (`listen 80; location / { try_files $uri $uri/ /index.html; }`).
- Mapeo de puertos `3000:80`.

---

## 8. Docker Compose

**`.env.example`** (se copia a `.env`):
```
MSSQL_SA_PASSWORD=Tarjetas_Chn2026
CLAVE_USUARIO_APLICACION=Aplicacion_Tarjetas2026
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
```

**`docker-compose.yml`**:
```yaml
name: sistema-tarjetas-credito

services:
  base-datos:
    image: mcr.microsoft.com/mssql/server:2022-latest
    platform: linux/amd64
    container_name: tarjetas-base-datos
    environment:
      ACCEPT_EULA: "Y"
      MSSQL_PID: Developer
      MSSQL_SA_PASSWORD: ${MSSQL_SA_PASSWORD}
      TZ: America/Guatemala
    ports:
      - "1433:1433"
    volumes:
      - datos-sql-server:/var/opt/mssql
    healthcheck:
      test: ["CMD-SHELL", "/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P \"$$MSSQL_SA_PASSWORD\" -C -b -Q \"SELECT 1\" -o /dev/null"]
      interval: 10s
      timeout: 5s
      retries: 20
      start_period: 30s

  inicializador-base-datos:
    image: mcr.microsoft.com/mssql/server:2022-latest
    platform: linux/amd64
    container_name: tarjetas-inicializador-base-datos
    depends_on:
      base-datos:
        condition: service_healthy
    environment:
      SERVIDOR_BASE_DATOS: base-datos
      MSSQL_SA_PASSWORD: ${MSSQL_SA_PASSWORD}
      CLAVE_USUARIO_APLICACION: ${CLAVE_USUARIO_APLICACION}
    volumes:
      - ./database:/scripts:ro
    entrypoint: ["/bin/bash", "/scripts/inicializar-base-datos.sh"]
    restart: "no"

  keycloak:
    image: quay.io/keycloak/keycloak:25.0.6
    container_name: tarjetas-keycloak
    command: ["start-dev", "--import-realm"]
    environment:
      KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN}
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
      KC_HOSTNAME: http://localhost:8180
      KC_HOSTNAME_BACKCHANNEL_DYNAMIC: "true"
      KC_HTTP_ENABLED: "true"
      KC_HEALTH_ENABLED: "true"
    ports:
      - "8180:8080"
    volumes:
      - ./keycloak/realm-tarjetas.json:/opt/keycloak/data/import/realm-tarjetas.json:ro
    healthcheck:
      test: ["CMD", "bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/9000 && printf 'GET /health/ready HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && grep -q 'UP' <&3"]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 40s

  backend:
    build: ./backend
    container_name: tarjetas-backend
    depends_on:
      inicializador-base-datos:
        condition: service_completed_successfully
      keycloak:
        condition: service_healthy
    environment:
      URL_BASE_DATOS: "jdbc:sqlserver://base-datos:1433;databaseName=GestionTarjetasCredito;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=false"
      USUARIO_BASE_DATOS: usuario_tarjetas
      CLAVE_BASE_DATOS: ${CLAVE_USUARIO_APLICACION}
      KEYCLOAK_ISSUER_URI: http://localhost:8180/realms/tarjetas
      KEYCLOAK_JWK_SET_URI: http://keycloak:8080/realms/tarjetas/protocol/openid-connect/certs
      CORS_ORIGENES_PERMITIDOS: http://localhost:3000,http://localhost:5173
      TZ: America/Guatemala
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-fs", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 20
      start_period: 40s

  frontend:
    build:
      context: ./frontend
      args:
        VITE_URL_API: http://localhost:8080/gestion-tarjetas
        VITE_KEYCLOAK_URL: http://localhost:8180
        VITE_KEYCLOAK_REALM: tarjetas
        VITE_KEYCLOAK_CLIENTE_ID: frontend-tarjetas
    container_name: tarjetas-frontend
    depends_on:
      backend:
        condition: service_healthy
    ports:
      - "3000:80"

volumes:
  datos-sql-server:
```
Notas:
- `platform: linux/amd64` es necesario porque SQL Server no tiene imagen ARM; en Apple Silicon funciona con la emulación Rosetta de Docker Desktop.
- Para desarrollo local: `docker compose up -d base-datos inicializador-base-datos keycloak`, luego `./mvnw spring-boot:run` en `backend/` y `npm run dev` en `frontend/`.

**`.gitignore`**: `.env`, `backend/target/`, `frontend/node_modules/`, `frontend/dist/`, `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`, `*.log`.

---

## 9. Datos semilla (`03_datos_semilla.sql`)

- Todo va dentro de `IF NOT EXISTS (SELECT 1 FROM dbo.cliente) BEGIN ... END`, en una transacción.
- Los ids se obtienen con `SCOPE_IDENTITY()` en variables `DECLARE @id_cliente_ana BIGINT`, etc. Nada de IDs fijos.

**Clientes**
| var | nombre | apellido | identificación | nacimiento | dirección | correo | teléfono |
|---|---|---|---|---|---|---|---|
| ana | Ana Lucía | Morales Pérez | 2456789010101 | 1990-04-15 | 5a. Avenida 10-25, Zona 1, Ciudad de Guatemala | ana.morales@correo.com | 5555-1234 |
| carlos | Carlos Eduardo | Ramírez López | 3012456780101 | 1985-11-02 | 12 Calle 3-40, Zona 10, Ciudad de Guatemala | carlos.ramirez@correo.com | 4444-5678 |
| maria | María José | Castillo Herrera | 1987654320101 | 1995-07-21 | Diagonal 6 15-20, Zona 13, Mixco | maria.castillo@correo.com | 3333-9012 |
| luis | Luis Fernando | Gómez Ortiz | 2678901230101 | 1978-02-10 | 4a. Calle 7-11, Zona 2, Quetzaltenango | luis.gomez@correo.com | 5222-3456 |

**Solicitudes** (usuario_decision = 'admin' en las decididas)
| cliente | tipo | fecha_solicitud | ingreso | estado | fecha_decision | monto_aprobado | comentario |
|---|---|---|---|---|---|---|---|
| ana | ORO | 2026-09-01 09:00 | 12000.00 | APROBADA | 2026-09-02 10:00 | 25000.00 | Cliente con historial crediticio favorable |
| carlos | PLATINO | 2026-09-03 11:30 | 30000.00 | APROBADA | 2026-09-04 09:15 | 60000.00 | Ingresos verificados |
| maria | CLASICA | 2026-10-01 14:00 | 4500.00 | PENDIENTE | NULL | NULL | NULL |
| luis | ORO | 2026-09-20 08:45 | 6000.00 | RECHAZADA | 2026-09-22 16:00 | NULL | Ingreso insuficiente para el tipo de tarjeta solicitado |
| ana | PLATINO | 2026-10-05 10:20 | 12000.00 | PENDIENTE | NULL | NULL | NULL |

**Tarjetas** (los números cumplen Luhn y el prefijo de su tipo)
| cliente / solicitud | número | tipo | límite | saldo_actual | emisión |
|---|---|---|---|---|---|
| ana / ORO | 4200123456789015 | ORO | 25000.00 | **3550.50** | 2026-09-02 10:00 |
| carlos / PLATINO | 4300987654321014 | PLATINO | 60000.00 | **10000.00** | 2026-09-04 09:15 |

**Movimientos** (usuario_registro = 'operador')
- Tarjeta de Ana:
  - Consumos: 1500.00 "Supermercado La Torre" (2026-09-05 18:20), 3250.50 "Gasolinera Puma" (2026-09-10 07:45), 800.00 "Restaurante Los Cebollines" (2026-09-15 20:10).
  - Pago: 2000.00 "Pago en agencia CHN" (2026-09-25 12:00).
  - Saldo: 5550.50 − 2000.00 = **3550.50**.
- Tarjeta de Carlos:
  - Consumos: 12000.00 "Boletos aéreos" (2026-09-06 15:00), 4500.75 "Hotel en Antigua Guatemala" (2026-09-12 13:30).
  - Pago: 6500.75 "Pago por banca en línea" (2026-09-28 09:00).
  - Saldo: 16500.75 − 6500.75 = **10000.00**.

`docs/diagrama-entidad-relacion.md`: un `erDiagram` de Mermaid con las 5 tablas, sus columnas, PK/FK/UK y las cardinalidades (cliente 1–N solicitud_tarjeta; solicitud_tarjeta 1–0..1 tarjeta_credito; cliente 1–N tarjeta_credito; tarjeta_credito 1–N consumo y pago), más la explicación de las cascadas de la sección 2.3.

---

## 10. Convenciones de código

- **Lombok en las entidades** (`@Getter`, `@Setter` por campo y `@NoArgsConstructor`); no se usa `@Data` porque su `equals`/`hashCode`/`toString` recorre relaciones perezosas. Los DTO son `record`. Sin MapStruct: los mappers son clases con métodos estáticos.
- Nombres de dominio en español y descriptivos (`idCliente`, `montoAprobado`, `creditoDisponible`). Los sufijos de capa siguen la convención de Spring (`Controller`, `Service`, `Repository`, `Mapper`, `Exception`). Peticiones con `*Peticion` y respuestas con `*Respuesta`.
- Inyección por constructor y campos `private final`. Nada de `@Autowired` en campos.
- Dinero siempre con `BigDecimal` y `compareTo`, nunca `double` ni `equals`.
- Mensajes de validación y de error en español y orientados al usuario.
- SQL: palabras clave en MAYÚSCULAS, identificadores en snake_case y constraints con nombre.
- Frontend: archivos `.vue` en PascalCase con sufijo `Vista` para las vistas; funciones en camelCase con verbo en español (`cargarClientes`, `guardarCliente`).

### NO hacer
1. No usar `ddl-auto` distinto de `validate`, ni que Hibernate cree tablas.
2. No usar `NVARCHAR`, `CHAR`, `FLOAT`/`REAL`/`MONEY` ni `DATETIME` (el antiguo) en la BD.
3. No poner `ON DELETE CASCADE` en `tarjeta_credito.id_cliente`.
4. No mapear colecciones `@OneToMany` con `cascade` en `Cliente`.
5. No devolver entidades JPA desde los controladores ni activar `open-in-view`.
6. No validar el issuer por discovery (`issuer-uri` de Spring) dentro de Docker.
7. No poner secretos reales en el repositorio: solo `.env.example`, y `.env` en `.gitignore`.
8. No permitir que un pago deje el saldo negativo, que un consumo supere el límite ni decidir solicitudes que no estén PENDIENTE.
9. No usar variables de una letra, abreviaturas crípticas ni nombres en inglés para conceptos del dominio.
10. No agregar endpoints, campos JSON o rutas que no estén en este contrato sin coordinarlo antes.

---

### Archivos críticos para la implementación
- /Users/jafetchacon/Documents/Codex/2026-08-06/g/sistema-tarjetas-credito/database/02_crear_esquema.sql
- /Users/jafetchacon/Documents/Codex/2026-08-06/g/sistema-tarjetas-credito/docker-compose.yml
- /Users/jafetchacon/Documents/Codex/2026-08-06/g/sistema-tarjetas-credito/backend/src/main/java/gt/com/chn/tarjetas/config/ConfiguracionSeguridad.java
- /Users/jafetchacon/Documents/Codex/2026-08-06/g/sistema-tarjetas-credito/backend/src/main/java/gt/com/chn/tarjetas/service/MovimientoTarjetaService.java
- /Users/jafetchacon/Documents/Codex/2026-08-06/g/sistema-tarjetas-credito/frontend/src/servicios/clienteHttp.js