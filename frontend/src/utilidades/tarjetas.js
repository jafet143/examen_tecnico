import { ESTADO_TARJETA_REEMPLAZADA, ESTADO_TARJETA_CANCELADA } from './constantes'

export function esTarjetaVigenteVencida(tarjeta) {
  return Boolean(tarjeta?.vencida) && tarjeta.estadoTarjeta !== ESTADO_TARJETA_REEMPLAZADA && tarjeta.estadoTarjeta !== ESTADO_TARJETA_CANCELADA
}
