#!/bin/bash
set -euo pipefail

SQLCMD=/opt/mssql-tools18/bin/sqlcmd
CONEXION=(-S "$SERVIDOR_BASE_DATOS" -U sa -P "$MSSQL_SA_PASSWORD" -C -b -I)

MAXIMO_INTENTOS=30
intento=1
echo "Esperando a que SQL Server ($SERVIDOR_BASE_DATOS) responda..."
until "$SQLCMD" "${CONEXION[@]}" -Q "SELECT 1" -o /dev/null > /dev/null 2>&1; do
    if [ "$intento" -ge "$MAXIMO_INTENTOS" ]; then
        echo "ERROR: SQL Server no respondió después de $MAXIMO_INTENTOS intentos." >&2
        exit 1
    fi
    echo "Intento $intento de $MAXIMO_INTENTOS: SQL Server aún no está listo, reintentando en 2 segundos..."
    intento=$((intento + 1))
    sleep 2
done
echo "SQL Server está disponible."

echo "1/3 Creando base de datos y usuario de la aplicación..."
"$SQLCMD" "${CONEXION[@]}" -i /scripts/01_crear_base_datos.sql -v CLAVE_USUARIO_APLICACION="$CLAVE_USUARIO_APLICACION"

echo "2/3 Creando esquema (tablas, restricciones e índices)..."
"$SQLCMD" "${CONEXION[@]}" -d GestionTarjetasCredito -i /scripts/02_crear_esquema.sql

echo "3/3 Cargando datos semilla..."
"$SQLCMD" "${CONEXION[@]}" -d GestionTarjetasCredito -i /scripts/03_datos_semilla.sql

echo "Base de datos inicializada correctamente"
