
USE master;
GO

IF DB_ID('GestionTarjetasCredito') IS NULL
BEGIN
    CREATE DATABASE GestionTarjetasCredito COLLATE Modern_Spanish_CI_AS;
    PRINT 'Base de datos GestionTarjetasCredito creada.';
END
ELSE
    PRINT 'La base de datos GestionTarjetasCredito ya existe.';
GO

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'usuario_tarjetas')
BEGIN
    CREATE LOGIN usuario_tarjetas
        WITH PASSWORD = '$(CLAVE_USUARIO_APLICACION)',
             DEFAULT_DATABASE = GestionTarjetasCredito,
             CHECK_POLICY = OFF;
    PRINT 'Login usuario_tarjetas creado.';
END
ELSE
    PRINT 'El login usuario_tarjetas ya existe.';
GO

USE GestionTarjetasCredito;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'usuario_tarjetas')
BEGIN
    CREATE USER usuario_tarjetas FOR LOGIN usuario_tarjetas;
    PRINT 'Usuario de base de datos usuario_tarjetas creado.';
END
GO

ALTER ROLE db_datareader ADD MEMBER usuario_tarjetas;
ALTER ROLE db_datawriter ADD MEMBER usuario_tarjetas;
GO

IF EXISTS (SELECT 1 FROM sys.database_scoped_configurations
           WHERE name = 'IDENTITY_CACHE' AND CAST(value AS NVARCHAR(10)) IN (N'1', N'ON'))
BEGIN
    ALTER DATABASE SCOPED CONFIGURATION SET IDENTITY_CACHE = OFF;
    PRINT 'IDENTITY_CACHE desactivada en GestionTarjetasCredito.';
END
ELSE
    PRINT 'IDENTITY_CACHE ya estaba desactivada.';
GO
