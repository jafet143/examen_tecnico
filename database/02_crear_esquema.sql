
USE GestionTarjetasCredito;
GO

IF OBJECT_ID('dbo.seq_codigo_cliente', 'SO') IS NULL
BEGIN
    CREATE SEQUENCE dbo.seq_codigo_cliente AS BIGINT START WITH 10000001 INCREMENT BY 1;
    PRINT 'Secuencia dbo.seq_codigo_cliente creada.';
END
GO

GRANT UPDATE ON OBJECT::dbo.seq_codigo_cliente TO usuario_tarjetas;
GO

IF OBJECT_ID('dbo.cliente', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cliente (
        id_cliente            BIGINT IDENTITY(1,1) NOT NULL,
        codigo_cliente        CHAR(8)      NOT NULL,
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
        CONSTRAINT uq_cliente_numero_identificacion UNIQUE (numero_identificacion),
        CONSTRAINT uq_cliente_codigo_cliente UNIQUE (codigo_cliente),
        CONSTRAINT ck_cliente_codigo_cliente CHECK (codigo_cliente NOT LIKE '%[^0-9]%')
    );
    PRINT 'Tabla dbo.cliente creada.';
END
GO

IF OBJECT_ID('dbo.solicitud_tarjeta', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.solicitud_tarjeta (
        id_solicitud         BIGINT IDENTITY(1,1) NOT NULL,
        id_cliente           BIGINT        NOT NULL,
        tipo_tarjeta         VARCHAR(10)   NOT NULL,
        fecha_solicitud      DATETIME2(3)  NOT NULL CONSTRAINT df_solicitud_tarjeta_fecha_solicitud DEFAULT SYSDATETIME(),
        ingreso_mensual      DECIMAL(18,2) NOT NULL,
        nombre_empleador     VARCHAR(150)  NOT NULL,
        fecha_inicio_laboral DATE          NOT NULL,
        estado               VARCHAR(10)   NOT NULL CONSTRAINT df_solicitud_tarjeta_estado DEFAULT 'PENDIENTE',
        fecha_decision       DATETIME2(3)  NULL,
        comentario_decision  VARCHAR(500)  NULL,
        monto_aprobado       DECIMAL(18,2) NULL,
        usuario_decision     VARCHAR(100)  NULL,
        CONSTRAINT pk_solicitud_tarjeta PRIMARY KEY (id_solicitud),
        CONSTRAINT fk_solicitud_tarjeta_cliente FOREIGN KEY (id_cliente)
            REFERENCES dbo.cliente (id_cliente) ON DELETE CASCADE,
        CONSTRAINT ck_solicitud_tarjeta_tipo_tarjeta CHECK (tipo_tarjeta IN ('CLASICA','ORO','PLATINO','BLACK')),
        CONSTRAINT ck_solicitud_tarjeta_estado CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA')),
        CONSTRAINT ck_solicitud_tarjeta_ingreso_mensual CHECK (ingreso_mensual > 0),
        CONSTRAINT ck_solicitud_tarjeta_nombre_empleador CHECK (LEN(LTRIM(RTRIM(nombre_empleador))) >= 2),
        CONSTRAINT ck_solicitud_tarjeta_monto_aprobado CHECK (monto_aprobado IS NULL OR monto_aprobado > 0),
        CONSTRAINT ck_solicitud_tarjeta_coherencia_decision CHECK (
            (estado = 'PENDIENTE' AND fecha_decision IS NULL AND monto_aprobado IS NULL)
         OR (estado = 'APROBADA'  AND fecha_decision IS NOT NULL AND monto_aprobado IS NOT NULL)
         OR (estado = 'RECHAZADA' AND fecha_decision IS NOT NULL AND monto_aprobado IS NULL AND comentario_decision IS NOT NULL)
        )
    );
    PRINT 'Tabla dbo.solicitud_tarjeta creada.';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_solicitud_tarjeta_id_cliente' AND object_id = OBJECT_ID('dbo.solicitud_tarjeta'))
    CREATE INDEX ix_solicitud_tarjeta_id_cliente ON dbo.solicitud_tarjeta (id_cliente);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_solicitud_tarjeta_estado' AND object_id = OBJECT_ID('dbo.solicitud_tarjeta'))
    CREATE INDEX ix_solicitud_tarjeta_estado ON dbo.solicitud_tarjeta (estado, fecha_solicitud);
GO

IF OBJECT_ID('dbo.cuenta_credito', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cuenta_credito (
        id_cuenta      BIGINT IDENTITY(1,1) NOT NULL,
        numero_cuenta  CHAR(16)      NOT NULL,
        id_cliente     BIGINT        NOT NULL,
        id_solicitud   BIGINT        NOT NULL,
        tipo_tarjeta   VARCHAR(10)   NOT NULL,
        limite_credito DECIMAL(12,2) NOT NULL,
        saldo_actual   DECIMAL(12,2) NOT NULL CONSTRAINT df_cuenta_credito_saldo_actual DEFAULT 0,
        fecha_apertura DATETIME2(0)  NOT NULL CONSTRAINT df_cuenta_credito_fecha_apertura DEFAULT SYSDATETIME(),
        estado_cuenta          VARCHAR(10)  NOT NULL CONSTRAINT df_cuenta_credito_estado_cuenta DEFAULT 'ACTIVA',
        motivo_cancelacion     VARCHAR(20)  NULL,
        comentario_cancelacion VARCHAR(250) NULL,
        fecha_cancelacion      DATETIME2(0) NULL,
        usuario_cancelacion    VARCHAR(100) NULL,
        CONSTRAINT pk_cuenta_credito PRIMARY KEY (id_cuenta),
        CONSTRAINT uq_cuenta_credito_numero_cuenta UNIQUE (numero_cuenta),
        CONSTRAINT uq_cuenta_credito_id_solicitud UNIQUE (id_solicitud),
        CONSTRAINT fk_cuenta_credito_cliente FOREIGN KEY (id_cliente)
            REFERENCES dbo.cliente (id_cliente) ON DELETE NO ACTION,
        CONSTRAINT fk_cuenta_credito_solicitud FOREIGN KEY (id_solicitud)
            REFERENCES dbo.solicitud_tarjeta (id_solicitud) ON DELETE CASCADE,
        CONSTRAINT ck_cuenta_credito_numero_cuenta CHECK (numero_cuenta NOT LIKE '%[^0-9]%'),
        CONSTRAINT ck_cuenta_credito_tipo_tarjeta CHECK (tipo_tarjeta IN ('CLASICA','ORO','PLATINO','BLACK')),
        CONSTRAINT ck_cuenta_credito_limite_credito CHECK (limite_credito > 0),
        CONSTRAINT ck_cuenta_credito_saldo_actual CHECK (saldo_actual >= 0),
        CONSTRAINT ck_cuenta_credito_estado_cuenta CHECK (estado_cuenta IN ('ACTIVA','CANCELADA')),
        CONSTRAINT ck_cuenta_credito_motivo_cancelacion CHECK (motivo_cancelacion IS NULL
            OR motivo_cancelacion IN ('SOLICITUD_CLIENTE','DECISION_BANCO')),
        CONSTRAINT ck_cuenta_credito_coherencia_cancelacion CHECK (
            (estado_cuenta = 'ACTIVA'    AND motivo_cancelacion IS NULL AND fecha_cancelacion IS NULL)
         OR (estado_cuenta = 'CANCELADA' AND motivo_cancelacion IS NOT NULL AND fecha_cancelacion IS NOT NULL))
    );
    PRINT 'Tabla dbo.cuenta_credito creada.';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_cuenta_credito_cliente_tipo_activa' AND object_id = OBJECT_ID('dbo.cuenta_credito'))
    CREATE UNIQUE INDEX ux_cuenta_credito_cliente_tipo_activa ON dbo.cuenta_credito (id_cliente, tipo_tarjeta)
        WHERE estado_cuenta = 'ACTIVA';
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_cuenta_credito_id_cliente' AND object_id = OBJECT_ID('dbo.cuenta_credito'))
    CREATE INDEX ix_cuenta_credito_id_cliente ON dbo.cuenta_credito (id_cliente, estado_cuenta);
GO

IF OBJECT_ID('dbo.cargo', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cargo (
        id_cargo         BIGINT IDENTITY(1,1) NOT NULL,
        id_cuenta        BIGINT        NOT NULL,
        tipo_cargo       VARCHAR(20)   NOT NULL,
        monto            DECIMAL(18,2) NOT NULL,
        fecha            DATETIME2(3)  NOT NULL,
        descripcion      VARCHAR(250)  NOT NULL,
        fecha_registro   DATETIME2(3)  NOT NULL CONSTRAINT df_cargo_fecha_registro DEFAULT SYSDATETIME(),
        usuario_registro VARCHAR(100)  NULL,
        ciclo_corte      DATE          NULL,
        reversado        BIT           NOT NULL CONSTRAINT df_cargo_reversado DEFAULT 0,
        motivo_reverso   VARCHAR(250)  NULL,
        fecha_reverso    DATETIME2(0)  NULL,
        usuario_reverso  VARCHAR(100)  NULL,
        CONSTRAINT pk_cargo PRIMARY KEY (id_cargo),
        CONSTRAINT fk_cargo_cuenta_credito FOREIGN KEY (id_cuenta)
            REFERENCES dbo.cuenta_credito (id_cuenta) ON DELETE CASCADE,
        CONSTRAINT ck_cargo_tipo_cargo CHECK (tipo_cargo IN ('COMISION_REPOSICION','INTERES','RECARGO_MORA')),
        CONSTRAINT ck_cargo_monto CHECK (monto > 0),
        CONSTRAINT ck_cargo_ciclo_corte CHECK (ciclo_corte IS NULL OR tipo_cargo = 'INTERES'),
        CONSTRAINT ck_cargo_coherencia_reverso CHECK (
            (reversado = 0 AND motivo_reverso IS NULL AND fecha_reverso IS NULL)
         OR (reversado = 1 AND motivo_reverso IS NOT NULL AND fecha_reverso IS NOT NULL))
    );
    PRINT 'Tabla dbo.cargo creada.';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_cargo_id_cuenta' AND object_id = OBJECT_ID('dbo.cargo'))
    CREATE INDEX ix_cargo_id_cuenta ON dbo.cargo (id_cuenta, fecha);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_cargo_interes_por_ciclo' AND object_id = OBJECT_ID('dbo.cargo'))
    CREATE UNIQUE INDEX ux_cargo_interes_por_ciclo ON dbo.cargo (id_cuenta, ciclo_corte)
        WHERE tipo_cargo = 'INTERES';
GO

IF OBJECT_ID('dbo.tarjeta_credito', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.tarjeta_credito (
        id_tarjeta            BIGINT IDENTITY(1,1) NOT NULL,
        numero_tarjeta        CHAR(16)     NOT NULL,
        id_cuenta             BIGINT       NOT NULL,
        estado_tarjeta        VARCHAR(12)  NOT NULL CONSTRAINT df_tarjeta_credito_estado_tarjeta DEFAULT 'ACTIVA',
        fecha_emision         DATETIME2(0) NOT NULL CONSTRAINT df_tarjeta_credito_fecha_emision DEFAULT SYSDATETIME(),
        fecha_vencimiento     DATE         NOT NULL,
        motivo_bloqueo        VARCHAR(20)  NULL,
        comentario_bloqueo    VARCHAR(250) NULL,
        fecha_bloqueo         DATETIME2(0) NULL,
        usuario_bloqueo       VARCHAR(100) NULL,
        id_tarjeta_anterior   BIGINT       NULL,
        motivo_reposicion     VARCHAR(20)  NULL,
        comentario_reposicion VARCHAR(250) NULL,
        fecha_reposicion      DATETIME2(0) NULL,
        usuario_reposicion    VARCHAR(100) NULL,
        denuncia_presentada   BIT          NULL,
        tarjeta_deteriorada_presentada BIT NULL,
        id_cargo_reposicion   BIGINT       NULL,
        CONSTRAINT pk_tarjeta_credito PRIMARY KEY (id_tarjeta),
        CONSTRAINT uq_tarjeta_credito_numero_tarjeta UNIQUE (numero_tarjeta),
        CONSTRAINT fk_tarjeta_credito_cuenta FOREIGN KEY (id_cuenta)
            REFERENCES dbo.cuenta_credito (id_cuenta) ON DELETE CASCADE,
        CONSTRAINT fk_tarjeta_credito_anterior FOREIGN KEY (id_tarjeta_anterior)
            REFERENCES dbo.tarjeta_credito (id_tarjeta) ON DELETE NO ACTION,
        CONSTRAINT fk_tarjeta_credito_cargo_reposicion FOREIGN KEY (id_cargo_reposicion)
            REFERENCES dbo.cargo (id_cargo) ON DELETE NO ACTION,
        CONSTRAINT ck_tarjeta_credito_numero_tarjeta CHECK (numero_tarjeta NOT LIKE '%[^0-9]%'),
        CONSTRAINT ck_tarjeta_credito_estado_tarjeta CHECK (estado_tarjeta IN ('ACTIVA','BLOQUEADA','REEMPLAZADA','CANCELADA')),
        CONSTRAINT ck_tarjeta_credito_motivo_bloqueo CHECK (motivo_bloqueo IS NULL
            OR motivo_bloqueo IN ('ROBO','EXTRAVIO','SOSPECHA_FRAUDE','SOLICITUD_CLIENTE')),
        CONSTRAINT ck_tarjeta_credito_motivo_reposicion CHECK (motivo_reposicion IS NULL
            OR motivo_reposicion IN ('ROBO','EXTRAVIO','DETERIORO','SOSPECHA_FRAUDE','RENOVACION')),
        CONSTRAINT ck_tarjeta_credito_coherencia_bloqueo CHECK (
            (estado_tarjeta = 'ACTIVA'      AND motivo_bloqueo IS NULL)
         OR (estado_tarjeta = 'BLOQUEADA'   AND motivo_bloqueo IS NOT NULL AND fecha_bloqueo IS NOT NULL)
         OR (estado_tarjeta IN ('REEMPLAZADA','CANCELADA') AND (motivo_bloqueo IS NULL OR fecha_bloqueo IS NOT NULL))),
        CONSTRAINT ck_tarjeta_credito_coherencia_reposicion CHECK (
            (estado_tarjeta = 'REEMPLAZADA' AND motivo_reposicion IS NOT NULL AND fecha_reposicion IS NOT NULL)
         OR (estado_tarjeta <> 'REEMPLAZADA' AND motivo_reposicion IS NULL AND fecha_reposicion IS NULL))
    );
    PRINT 'Tabla dbo.tarjeta_credito creada.';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_tarjeta_credito_vigente_por_cuenta' AND object_id = OBJECT_ID('dbo.tarjeta_credito'))
    CREATE UNIQUE INDEX ux_tarjeta_credito_vigente_por_cuenta ON dbo.tarjeta_credito (id_cuenta)
        WHERE estado_tarjeta <> 'REEMPLAZADA';
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_tarjeta_credito_anterior' AND object_id = OBJECT_ID('dbo.tarjeta_credito'))
    CREATE UNIQUE INDEX ux_tarjeta_credito_anterior ON dbo.tarjeta_credito (id_tarjeta_anterior)
        WHERE id_tarjeta_anterior IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_tarjeta_credito_id_cuenta' AND object_id = OBJECT_ID('dbo.tarjeta_credito'))
    CREATE INDEX ix_tarjeta_credito_id_cuenta ON dbo.tarjeta_credito (id_cuenta, fecha_emision);
GO

IF OBJECT_ID('dbo.consumo', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.consumo (
        id_consumo       BIGINT IDENTITY(1,1) NOT NULL,
        id_cuenta        BIGINT        NOT NULL,
        id_tarjeta       BIGINT        NOT NULL,
        monto            DECIMAL(18,2) NOT NULL,
        fecha            DATETIME2(3)  NOT NULL,
        descripcion      VARCHAR(250)  NOT NULL,
        fecha_registro   DATETIME2(3)  NOT NULL CONSTRAINT df_consumo_fecha_registro DEFAULT SYSDATETIME(),
        usuario_registro VARCHAR(100)  NULL,
        reversado        BIT           NOT NULL CONSTRAINT df_consumo_reversado DEFAULT 0,
        motivo_reverso   VARCHAR(250)  NULL,
        fecha_reverso    DATETIME2(0)  NULL,
        usuario_reverso  VARCHAR(100)  NULL,
        CONSTRAINT pk_consumo PRIMARY KEY (id_consumo),
        CONSTRAINT fk_consumo_cuenta_credito FOREIGN KEY (id_cuenta)
            REFERENCES dbo.cuenta_credito (id_cuenta) ON DELETE CASCADE,
        CONSTRAINT fk_consumo_tarjeta_credito FOREIGN KEY (id_tarjeta)
            REFERENCES dbo.tarjeta_credito (id_tarjeta) ON DELETE NO ACTION,
        CONSTRAINT ck_consumo_monto CHECK (monto > 0),
        CONSTRAINT ck_consumo_coherencia_reverso CHECK (
            (reversado = 0 AND motivo_reverso IS NULL AND fecha_reverso IS NULL)
         OR (reversado = 1 AND motivo_reverso IS NOT NULL AND fecha_reverso IS NOT NULL))
    );
    PRINT 'Tabla dbo.consumo creada.';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_consumo_id_cuenta' AND object_id = OBJECT_ID('dbo.consumo'))
    CREATE INDEX ix_consumo_id_cuenta ON dbo.consumo (id_cuenta, fecha);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_consumo_id_tarjeta' AND object_id = OBJECT_ID('dbo.consumo'))
    CREATE INDEX ix_consumo_id_tarjeta ON dbo.consumo (id_tarjeta);
GO

IF OBJECT_ID('dbo.pago', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.pago (
        id_pago          BIGINT IDENTITY(1,1) NOT NULL,
        id_cuenta        BIGINT        NOT NULL,
        monto            DECIMAL(18,2) NOT NULL,
        fecha            DATETIME2(3)  NOT NULL,
        descripcion      VARCHAR(250)  NOT NULL,
        fecha_registro   DATETIME2(3)  NOT NULL CONSTRAINT df_pago_fecha_registro DEFAULT SYSDATETIME(),
        usuario_registro VARCHAR(100)  NULL,
        reversado        BIT           NOT NULL CONSTRAINT df_pago_reversado DEFAULT 0,
        motivo_reverso   VARCHAR(250)  NULL,
        fecha_reverso    DATETIME2(0)  NULL,
        usuario_reverso  VARCHAR(100)  NULL,
        CONSTRAINT pk_pago PRIMARY KEY (id_pago),
        CONSTRAINT fk_pago_cuenta_credito FOREIGN KEY (id_cuenta)
            REFERENCES dbo.cuenta_credito (id_cuenta) ON DELETE CASCADE,
        CONSTRAINT ck_pago_monto CHECK (monto > 0),
        CONSTRAINT ck_pago_coherencia_reverso CHECK (
            (reversado = 0 AND motivo_reverso IS NULL AND fecha_reverso IS NULL)
         OR (reversado = 1 AND motivo_reverso IS NOT NULL AND fecha_reverso IS NOT NULL))
    );
    PRINT 'Tabla dbo.pago creada.';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_pago_id_cuenta' AND object_id = OBJECT_ID('dbo.pago'))
    CREATE INDEX ix_pago_id_cuenta ON dbo.pago (id_cuenta, fecha);
GO

PRINT 'Esquema verificado correctamente.';
GO
