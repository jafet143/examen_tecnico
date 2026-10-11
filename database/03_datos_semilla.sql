
USE GestionTarjetasCredito;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.cliente)
BEGIN
    BEGIN TRANSACTION;

    DECLARE @ahora DATETIME2(3) = DATEADD(HOUR, -6, SYSUTCDATETIME());
    DECLARE @corte_del_mes DATE = DATEFROMPARTS(YEAR(@ahora), MONTH(@ahora), 5);

    DECLARE @corte_actual DATE = CASE
        WHEN @ahora < DATEADD(SECOND, 86399, CAST(@corte_del_mes AS DATETIME2(3)))
            THEN DATEADD(MONTH, -1, @corte_del_mes)
        ELSE @corte_del_mes END;

    DECLARE @corte_vencido DATE = CASE
        WHEN @ahora > DATEADD(SECOND, 86399, CAST(DATEADD(DAY, 20, @corte_actual) AS DATETIME2(3)))
            THEN @corte_actual
        ELSE DATEADD(MONTH, -1, @corte_actual) END;

    DECLARE @base DATETIME2(3) = DATEADD(HOUR, 12, CAST(@corte_vencido AS DATETIME2(3)));

    DECLARE @fin_corte_actual DATETIME2(3) = DATEADD(SECOND, 86399, CAST(@corte_actual AS DATETIME2(3)));
    DECLARE @fecha_reciente DATETIME2(3) = CASE
        WHEN DATEADD(HOUR, -1, @ahora) > @fin_corte_actual THEN DATEADD(HOUR, -1, @ahora)
        ELSE DATEADD(SECOND, 1, @fin_corte_actual) END;

    DECLARE @id_cliente_ana     BIGINT,
            @id_cliente_carlos  BIGINT,
            @id_cliente_maria   BIGINT,
            @id_cliente_luis    BIGINT,
            @id_cliente_sofia   BIGINT,
            @id_cliente_andrea  BIGINT,
            @id_cliente_jorge   BIGINT;

    DECLARE @id_solicitud_ana_oro        BIGINT,
            @id_solicitud_ana_clasica    BIGINT,
            @id_solicitud_carlos_platino BIGINT,
            @id_solicitud_luis_clasica   BIGINT,
            @id_solicitud_sofia_black    BIGINT,
            @id_solicitud_jorge_clasica  BIGINT;

    DECLARE @id_cuenta_ana_oro     BIGINT,
            @id_cuenta_ana_clasica BIGINT,
            @id_cuenta_carlos      BIGINT,
            @id_cuenta_luis        BIGINT,
            @id_cuenta_sofia       BIGINT,
            @id_cuenta_jorge       BIGINT;

    DECLARE @id_tarjeta_ana_vieja   BIGINT,
            @id_tarjeta_ana_nueva   BIGINT,
            @id_tarjeta_ana_clasica BIGINT,
            @id_tarjeta_carlos      BIGINT,
            @id_tarjeta_luis        BIGINT,
            @id_tarjeta_sofia       BIGINT,
            @id_tarjeta_jorge       BIGINT;

    DECLARE @hoy DATE = CAST(@ahora AS DATE);

    DECLARE @base_previo DATETIME2(3) = DATEADD(MONTH, -1, @base);

    DECLARE @emision_vieja DATETIME2(0) = DATEADD(MONTH, -61, @ahora);

    DECLARE @apertura_jorge DATETIME2(0) = DATEADD(MONTH, -9, @ahora);
    DECLARE @cancelacion_jorge DATETIME2(0) = DATEADD(MONTH, -3, @ahora);

    DECLARE @pago_minimo_ciclo_actual DECIMAL(12,2) = CASE WHEN @corte_actual > @corte_vencido THEN 189.00 ELSE 0.00 END;

    ALTER SEQUENCE dbo.seq_codigo_cliente RESTART WITH 10000001;

    INSERT INTO dbo.cliente (codigo_cliente, nombre, apellido, numero_identificacion, fecha_nacimiento, direccion, correo_electronico, telefono)
    VALUES (CAST(NEXT VALUE FOR dbo.seq_codigo_cliente AS CHAR(8)), 'Ana Lucía', 'Morales Pérez', '2456789010101', '1990-04-15',
            '5a. Avenida 10-25, Zona 1, Ciudad de Guatemala', 'ana.morales@correo.com', '5555-1234');
    SET @id_cliente_ana = SCOPE_IDENTITY();

    INSERT INTO dbo.cliente (codigo_cliente, nombre, apellido, numero_identificacion, fecha_nacimiento, direccion, correo_electronico, telefono)
    VALUES (CAST(NEXT VALUE FOR dbo.seq_codigo_cliente AS CHAR(8)), 'Carlos Eduardo', 'Ramírez López', '3012456780101', '1985-11-02',
            '12 Calle 3-40, Zona 10, Ciudad de Guatemala', 'carlos.ramirez@correo.com', '4444-5678');
    SET @id_cliente_carlos = SCOPE_IDENTITY();

    INSERT INTO dbo.cliente (codigo_cliente, nombre, apellido, numero_identificacion, fecha_nacimiento, direccion, correo_electronico, telefono)
    VALUES (CAST(NEXT VALUE FOR dbo.seq_codigo_cliente AS CHAR(8)), 'María José', 'Castillo Herrera', '1987654320101', '1995-07-21',
            'Diagonal 6 15-20, Zona 13, Mixco', 'maria.castillo@correo.com', '3333-9012');
    SET @id_cliente_maria = SCOPE_IDENTITY();

    INSERT INTO dbo.cliente (codigo_cliente, nombre, apellido, numero_identificacion, fecha_nacimiento, direccion, correo_electronico, telefono)
    VALUES (CAST(NEXT VALUE FOR dbo.seq_codigo_cliente AS CHAR(8)), 'Luis Fernando', 'Gómez Ortiz', '2678901230101', '1978-02-10',
            '4a. Calle 7-11, Zona 2, Quetzaltenango', 'luis.gomez@correo.com', '5222-3456');
    SET @id_cliente_luis = SCOPE_IDENTITY();

    INSERT INTO dbo.cliente (codigo_cliente, nombre, apellido, numero_identificacion, fecha_nacimiento, direccion, correo_electronico, telefono)
    VALUES (CAST(NEXT VALUE FOR dbo.seq_codigo_cliente AS CHAR(8)), 'Sofía Alejandra', 'Méndez Rivas', '2345678900101', '1982-09-30',
            '15 Avenida 14-60, Zona 15, Vista Hermosa, Ciudad de Guatemala', 'sofia.mendez@correo.com', '4111-7890');
    SET @id_cliente_sofia = SCOPE_IDENTITY();

    INSERT INTO dbo.cliente (codigo_cliente, nombre, apellido, numero_identificacion, fecha_nacimiento, direccion, correo_electronico, telefono)
    VALUES (CAST(NEXT VALUE FOR dbo.seq_codigo_cliente AS CHAR(8)), 'Andrea Paola', 'Solís Barrios', '1765432100101', '2000-01-18',
            '3a. Avenida 5-30, Zona 4, Antigua Guatemala', 'andrea.solis@correo.com', '5333-4455');
    SET @id_cliente_andrea = SCOPE_IDENTITY();

    INSERT INTO dbo.cliente (codigo_cliente, nombre, apellido, numero_identificacion, fecha_nacimiento, direccion, correo_electronico, telefono)
    VALUES (CAST(NEXT VALUE FOR dbo.seq_codigo_cliente AS CHAR(8)), 'Jorge Alberto', 'Pineda Cifuentes', '2890123450101', '1988-03-12',
            '2a. Calle 8-15, Zona 3, Mixco', 'jorge.pineda@correo.com', '5444-7788');
    SET @id_cliente_jorge = SCOPE_IDENTITY();

    INSERT INTO dbo.solicitud_tarjeta (id_cliente, tipo_tarjeta, fecha_solicitud, ingreso_mensual, nombre_empleador, fecha_inicio_laboral,
                                       estado, fecha_decision, comentario_decision, monto_aprobado, usuario_decision)
    VALUES (@id_cliente_ana, 'ORO', DATEADD(DAY, -46, @base), 12000.00, 'Banco Industrial de Occidente', DATEADD(YEAR, -9, @hoy),
            'APROBADA', DATEADD(DAY, -45, @base), 'Cliente con historial crediticio favorable', 15000.00, 'admin');
    SET @id_solicitud_ana_oro = SCOPE_IDENTITY();

    INSERT INTO dbo.solicitud_tarjeta (id_cliente, tipo_tarjeta, fecha_solicitud, ingreso_mensual, nombre_empleador, fecha_inicio_laboral,
                                       estado, fecha_decision, comentario_decision, monto_aprobado, usuario_decision)
    VALUES (@id_cliente_ana, 'CLASICA', DATEADD(DAY, -1, @emision_vieja), 12000.00, 'Banco Industrial de Occidente', DATEADD(YEAR, -9, @hoy),
            'APROBADA', @emision_vieja, 'Segunda tarjeta solicitada por la clienta', 4000.00, 'admin');
    SET @id_solicitud_ana_clasica = SCOPE_IDENTITY();

    INSERT INTO dbo.solicitud_tarjeta (id_cliente, tipo_tarjeta, fecha_solicitud, ingreso_mensual, nombre_empleador, fecha_inicio_laboral,
                                       estado, fecha_decision, comentario_decision, monto_aprobado, usuario_decision)
    VALUES (@id_cliente_carlos, 'PLATINO', DATEADD(DAY, -41, @base_previo), 28000.00, 'Constructora Ramírez y Asociados', DATEADD(YEAR, -9, @hoy),
            'APROBADA', DATEADD(DAY, -40, @base_previo), 'Ingresos verificados', 40000.00, 'admin');
    SET @id_solicitud_carlos_platino = SCOPE_IDENTITY();

    INSERT INTO dbo.solicitud_tarjeta (id_cliente, tipo_tarjeta, fecha_solicitud, ingreso_mensual, nombre_empleador, fecha_inicio_laboral,
                                       estado, fecha_decision, comentario_decision, monto_aprobado, usuario_decision)
    VALUES (@id_cliente_luis, 'CLASICA', DATEADD(DAY, -41, @base), 3500.00, 'Ferretería El Constructor', DATEADD(YEAR, -6, @hoy),
            'APROBADA', DATEADD(DAY, -40, @base), 'Primera tarjeta del cliente', 5000.00, 'admin');
    SET @id_solicitud_luis_clasica = SCOPE_IDENTITY();

    INSERT INTO dbo.solicitud_tarjeta (id_cliente, tipo_tarjeta, fecha_solicitud, ingreso_mensual, nombre_empleador, fecha_inicio_laboral,
                                       estado, fecha_decision, comentario_decision, monto_aprobado, usuario_decision)
    VALUES (@id_cliente_sofia, 'BLACK', DATEADD(DAY, -51, @base), 45000.00, 'Corporación Méndez Rivas', DATEADD(YEAR, -12, @hoy),
            'APROBADA', DATEADD(DAY, -50, @base), 'Perfil de alto valor', 60000.00, 'admin');
    SET @id_solicitud_sofia_black = SCOPE_IDENTITY();

    INSERT INTO dbo.solicitud_tarjeta (id_cliente, tipo_tarjeta, fecha_solicitud, ingreso_mensual, nombre_empleador, fecha_inicio_laboral,
                                       estado, fecha_decision, comentario_decision, monto_aprobado, usuario_decision)
    VALUES (@id_cliente_jorge, 'CLASICA', DATEADD(DAY, -1, @apertura_jorge), 3800.00, 'Distribuidora La Esperanza', DATEADD(YEAR, -6, @hoy),
            'APROBADA', @apertura_jorge, 'Primera tarjeta del cliente', 4500.00, 'admin');
    SET @id_solicitud_jorge_clasica = SCOPE_IDENTITY();

    INSERT INTO dbo.solicitud_tarjeta (id_cliente, tipo_tarjeta, fecha_solicitud, ingreso_mensual, nombre_empleador, fecha_inicio_laboral, estado)
    VALUES (@id_cliente_maria, 'CLASICA', DATEADD(DAY, -2, @ahora), 4500.00, 'Municipalidad de Mixco', DATEADD(YEAR, -3, @hoy), 'PENDIENTE');

    INSERT INTO dbo.solicitud_tarjeta (id_cliente, tipo_tarjeta, fecha_solicitud, ingreso_mensual, nombre_empleador, fecha_inicio_laboral, estado)
    VALUES (@id_cliente_andrea, 'CLASICA', DATEADD(DAY, -1, @ahora), 3200.00, 'Café Antigua', DATEADD(MONTH, -6, @hoy), 'PENDIENTE');

    INSERT INTO dbo.solicitud_tarjeta (id_cliente, tipo_tarjeta, fecha_solicitud, ingreso_mensual, nombre_empleador, fecha_inicio_laboral,
                                       estado, fecha_decision, comentario_decision, monto_aprobado, usuario_decision)
    VALUES (@id_cliente_luis, 'ORO', DATEADD(DAY, -6, @ahora), 9000.00, 'Ferretería El Constructor', DATEADD(YEAR, -6, @hoy),
            'RECHAZADA', DATEADD(DAY, -5, @ahora), 'No se pudo verificar el ingreso declarado', NULL, 'admin');

    INSERT INTO dbo.cuenta_credito (numero_cuenta, id_cliente, id_solicitud, tipo_tarjeta, limite_credito, saldo_actual, fecha_apertura)
    VALUES ('4200123456789015', @id_cliente_ana, @id_solicitud_ana_oro, 'ORO', 15000.00, 1550.00, DATEADD(DAY, -45, @base));
    SET @id_cuenta_ana_oro = SCOPE_IDENTITY();

    INSERT INTO dbo.cuenta_credito (numero_cuenta, id_cliente, id_solicitud, tipo_tarjeta, limite_credito, saldo_actual, fecha_apertura)
    VALUES ('4100204813579629', @id_cliente_ana, @id_solicitud_ana_clasica, 'CLASICA', 4000.00, 1800.00 - @pago_minimo_ciclo_actual, @emision_vieja);
    SET @id_cuenta_ana_clasica = SCOPE_IDENTITY();

    INSERT INTO dbo.cuenta_credito (numero_cuenta, id_cliente, id_solicitud, tipo_tarjeta, limite_credito, saldo_actual, fecha_apertura)
    VALUES ('4300987654321014', @id_cliente_carlos, @id_solicitud_carlos_platino, 'PLATINO', 40000.00, 15300.75, DATEADD(DAY, -40, @base_previo));
    SET @id_cuenta_carlos = SCOPE_IDENTITY();

    INSERT INTO dbo.cuenta_credito (numero_cuenta, id_cliente, id_solicitud, tipo_tarjeta, limite_credito, saldo_actual, fecha_apertura)
    VALUES ('4100456789012015', @id_cliente_luis, @id_solicitud_luis_clasica, 'CLASICA', 5000.00, 0.00, DATEADD(DAY, -40, @base));
    SET @id_cuenta_luis = SCOPE_IDENTITY();

    INSERT INTO dbo.cuenta_credito (numero_cuenta, id_cliente, id_solicitud, tipo_tarjeta, limite_credito, saldo_actual, fecha_apertura)
    VALUES ('4400765432109011', @id_cliente_sofia, @id_solicitud_sofia_black, 'BLACK', 60000.00, 6300.00, DATEADD(DAY, -50, @base));
    SET @id_cuenta_sofia = SCOPE_IDENTITY();

    INSERT INTO dbo.cuenta_credito (numero_cuenta, id_cliente, id_solicitud, tipo_tarjeta, limite_credito, saldo_actual, fecha_apertura,
                                    estado_cuenta, motivo_cancelacion, comentario_cancelacion, fecha_cancelacion, usuario_cancelacion)
    VALUES ('4100315974628011', @id_cliente_jorge, @id_solicitud_jorge_clasica, 'CLASICA', 4500.00, 0.00, @apertura_jorge,
            'CANCELADA', 'SOLICITUD_CLIENTE', 'El cliente ya no utiliza la tarjeta', @cancelacion_jorge, 'admin');
    SET @id_cuenta_jorge = SCOPE_IDENTITY();

    INSERT INTO dbo.tarjeta_credito (numero_tarjeta, id_cuenta, estado_tarjeta, fecha_emision, fecha_vencimiento,
                                     motivo_bloqueo, comentario_bloqueo, fecha_bloqueo, usuario_bloqueo,
                                     motivo_reposicion, comentario_reposicion, fecha_reposicion, usuario_reposicion,
                                     denuncia_presentada)
    VALUES ('4200123456789015', @id_cuenta_ana_oro, 'REEMPLAZADA', DATEADD(DAY, -45, @base), EOMONTH(DATEADD(YEAR, 5, DATEADD(DAY, -45, @base))),
            'ROBO', 'Tarjeta robada en un autobús, reportada por la clienta', DATEADD(DAY, 12, @base), 'operador',
            'ROBO', 'Tarjeta robada en un autobús, reportada por la clienta', DATEADD(DAY, 12, @base), 'operador',
            1);
    SET @id_tarjeta_ana_vieja = SCOPE_IDENTITY();

    INSERT INTO dbo.tarjeta_credito (numero_tarjeta, id_cuenta, estado_tarjeta, fecha_emision, fecha_vencimiento, id_tarjeta_anterior)
    VALUES ('4200555123409875', @id_cuenta_ana_oro, 'ACTIVA', DATEADD(DAY, 12, @base), EOMONTH(DATEADD(YEAR, 5, DATEADD(DAY, 12, @base))),
            @id_tarjeta_ana_vieja);
    SET @id_tarjeta_ana_nueva = SCOPE_IDENTITY();

    INSERT INTO dbo.tarjeta_credito (numero_tarjeta, id_cuenta, estado_tarjeta, fecha_emision, fecha_vencimiento)
    VALUES ('4100204813579629', @id_cuenta_ana_clasica, 'ACTIVA', @emision_vieja, EOMONTH(DATEADD(YEAR, 5, @emision_vieja)));
    SET @id_tarjeta_ana_clasica = SCOPE_IDENTITY();

    INSERT INTO dbo.tarjeta_credito (numero_tarjeta, id_cuenta, estado_tarjeta, fecha_emision, fecha_vencimiento)
    VALUES ('4300987654321014', @id_cuenta_carlos, 'ACTIVA', DATEADD(DAY, -40, @base_previo), EOMONTH(DATEADD(YEAR, 5, DATEADD(DAY, -40, @base_previo))));
    SET @id_tarjeta_carlos = SCOPE_IDENTITY();

    INSERT INTO dbo.tarjeta_credito (numero_tarjeta, id_cuenta, estado_tarjeta, fecha_emision, fecha_vencimiento,
                                     motivo_bloqueo, comentario_bloqueo, fecha_bloqueo, usuario_bloqueo)
    VALUES ('4100456789012015', @id_cuenta_luis, 'BLOQUEADA', DATEADD(DAY, -40, @base), EOMONTH(DATEADD(YEAR, 5, DATEADD(DAY, -40, @base))),
            'SOLICITUD_CLIENTE', 'El cliente la bloqueó por seguridad durante un viaje', DATEADD(DAY, -2, @ahora), 'operador');
    SET @id_tarjeta_luis = SCOPE_IDENTITY();

    INSERT INTO dbo.tarjeta_credito (numero_tarjeta, id_cuenta, estado_tarjeta, fecha_emision, fecha_vencimiento,
                                     motivo_bloqueo, comentario_bloqueo, fecha_bloqueo, usuario_bloqueo)
    VALUES ('4400765432109011', @id_cuenta_sofia, 'BLOQUEADA', DATEADD(DAY, -50, @base), EOMONTH(DATEADD(YEAR, 5, DATEADD(DAY, -50, @base))),
            'SOSPECHA_FRAUDE', 'Compras en el extranjero no reconocidas por la clienta', DATEADD(DAY, -1, @ahora), 'operador');
    SET @id_tarjeta_sofia = SCOPE_IDENTITY();

    INSERT INTO dbo.tarjeta_credito (numero_tarjeta, id_cuenta, estado_tarjeta, fecha_emision, fecha_vencimiento)
    VALUES ('4100315974628011', @id_cuenta_jorge, 'CANCELADA', @apertura_jorge, EOMONTH(DATEADD(YEAR, 5, @apertura_jorge)));
    SET @id_tarjeta_jorge = SCOPE_IDENTITY();

    INSERT INTO dbo.consumo (id_cuenta, id_tarjeta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_ana_oro, @id_tarjeta_ana_vieja, 1500.00, DATEADD(DAY, -22, @base), 'Supermercado La Torre', 'operador'),
           (@id_cuenta_ana_oro, @id_tarjeta_ana_vieja, 3250.50, DATEADD(DAY, -17, @base), 'Gasolinera Puma', 'operador'),
           (@id_cuenta_ana_oro, @id_tarjeta_ana_vieja,  800.00, DATEADD(DAY,  -9, @base), 'Restaurante Los Cebollines', 'operador'),
           (@id_cuenta_ana_oro, @id_tarjeta_ana_nueva, 1200.00, @fecha_reciente,          'Farmacia Fischer', 'operador'),
           (@id_cuenta_ana_oro, @id_tarjeta_ana_nueva,  350.00, @fecha_reciente,          'Cafetería Barista', 'operador');

    INSERT INTO dbo.consumo (id_cuenta, id_tarjeta, monto, fecha, descripcion, usuario_registro,
                             reversado, motivo_reverso, fecha_reverso, usuario_reverso)
    VALUES (@id_cuenta_ana_oro, @id_tarjeta_ana_vieja, 800.00, DATEADD(HOUR, 1, DATEADD(DAY, -9, @base)), 'Restaurante Los Cebollines',
            'operador', 1, 'Cargo duplicado por el comercio', DATEADD(DAY, -8, @base), 'admin');

    INSERT INTO dbo.pago (id_cuenta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_ana_oro, 5550.50, DATEADD(DAY, 10, @base), 'Pago total en agencia CHN', 'operador');

    INSERT INTO dbo.consumo (id_cuenta, id_tarjeta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_ana_clasica, @id_tarjeta_ana_clasica, 1250.00, DATEADD(DAY, 30, @emision_vieja), 'Supermercado Paiz', 'operador'),
           (@id_cuenta_ana_clasica, @id_tarjeta_ana_clasica, 2000.00, DATEADD(DAY, -15, @base), 'Electrodomésticos Max', 'operador');

    INSERT INTO dbo.pago (id_cuenta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_ana_clasica, 1250.00, DATEADD(DAY, 38, @emision_vieja), 'Pago total en agencia CHN', 'operador'),
           (@id_cuenta_ana_clasica, 200.00, DATEADD(DAY, 10, @base), 'Pago mínimo por banca en línea', 'operador');

    IF @pago_minimo_ciclo_actual > 0
        INSERT INTO dbo.pago (id_cuenta, monto, fecha, descripcion, usuario_registro)
        VALUES (@id_cuenta_ana_clasica, @pago_minimo_ciclo_actual, @fecha_reciente, 'Pago mínimo por banca en línea', 'operador');

    INSERT INTO dbo.consumo (id_cuenta, id_tarjeta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_carlos, @id_tarjeta_carlos, 12000.00, DATEADD(DAY, -24, @base_previo), 'Boletos aéreos', 'operador'),
           (@id_cuenta_carlos, @id_tarjeta_carlos,  4500.75, DATEADD(DAY, -15, @base), 'Hotel en Antigua Guatemala', 'operador');

    INSERT INTO dbo.pago (id_cuenta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_carlos,  500.00, DATEADD(DAY, 8, @base_previo), 'Abono por banca en línea', 'operador'),
           (@id_cuenta_carlos, 1000.00, DATEADD(DAY, 8, @base), 'Abono por banca en línea', 'operador');

    INSERT INTO dbo.cargo (id_cuenta, tipo_cargo, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_carlos, 'RECARGO_MORA', 150.00, DATEADD(DAY, 20, @base_previo), 'Recargo por mora del ciclo anterior', 'admin'),
           (@id_cuenta_carlos, 'RECARGO_MORA', 150.00, DATEADD(DAY, 20, @base), 'Recargo por mora del ciclo vencido', 'admin');

    INSERT INTO dbo.consumo (id_cuenta, id_tarjeta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_luis, @id_tarjeta_luis, 800.00, DATEADD(DAY, -20, @base), 'Ferretería El Constructor', 'operador');

    INSERT INTO dbo.pago (id_cuenta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_luis, 800.00, DATEADD(DAY, -8, @base), 'Pago total por banca en línea', 'operador');

    INSERT INTO dbo.consumo (id_cuenta, id_tarjeta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_sofia, @id_tarjeta_sofia, 8500.00, DATEADD(DAY, -20, @base), 'Boutique en Zona 10', 'operador'),
           (@id_cuenta_sofia, @id_tarjeta_sofia, 3750.25, DATEADD(DAY, -12, @base), 'Joyería Alta Gama', 'operador'),
           (@id_cuenta_sofia, @id_tarjeta_sofia, 6300.00, DATEADD(DAY,  14, @base), 'Hotel en Riviera Maya', 'operador');

    INSERT INTO dbo.pago (id_cuenta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_sofia, 12250.25, DATEADD(DAY, 8, @base), 'Pago total en agencia CHN', 'operador');

    INSERT INTO dbo.consumo (id_cuenta, id_tarjeta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_jorge, @id_tarjeta_jorge,  450.00, DATEADD(DAY, 20, @apertura_jorge), 'Supermercado La Torre', 'operador'),
           (@id_cuenta_jorge, @id_tarjeta_jorge, 1200.50, DATEADD(DAY, 50, @apertura_jorge), 'Tienda de electrónica', 'operador');

    INSERT INTO dbo.pago (id_cuenta, monto, fecha, descripcion, usuario_registro)
    VALUES (@id_cuenta_jorge,  450.00, DATEADD(DAY, 28, @apertura_jorge), 'Pago total en agencia CHN', 'operador'),
           (@id_cuenta_jorge, 1200.50, DATEADD(DAY, 58, @apertura_jorge), 'Pago total por banca en línea', 'operador');

    COMMIT TRANSACTION;
    PRINT 'Datos semilla insertados correctamente.';
END
ELSE
    PRINT 'La tabla dbo.cliente ya tiene datos; no se insertan datos semilla.';
GO
