import clienteHttp from './clienteHttp'

export async function listarCuentas(estado) {
  const parametros = estado ? { estado } : {}
  const respuesta = await clienteHttp.get('/cuentas', { params: parametros })
  return respuesta.data
}

export async function obtenerCuenta(idCuenta) {
  const respuesta = await clienteHttp.get(`/cuentas/${idCuenta}`)
  return respuesta.data
}

export async function listarTarjetasDeCuenta(idCuenta) {
  const respuesta = await clienteHttp.get(`/cuentas/${idCuenta}/tarjetas`)
  return respuesta.data
}

export async function listarMovimientosDeCuenta(idCuenta) {
  const respuesta = await clienteHttp.get(`/cuentas/${idCuenta}/movimientos`)
  return respuesta.data
}

export async function registrarConsumo(idCuenta, datos) {
  const respuesta = await clienteHttp.post(`/cuentas/${idCuenta}/consumos`, datos)
  return respuesta.data
}

export async function registrarPago(idCuenta, datos) {
  const respuesta = await clienteHttp.post(`/cuentas/${idCuenta}/pagos`, datos)
  return respuesta.data
}

export async function listarCuentasDeCliente(idCliente) {
  const respuesta = await clienteHttp.get(`/clientes/${idCliente}/cuentas`)
  return respuesta.data
}

export async function registrarCargo(idCuenta, datos) {
  const respuesta = await clienteHttp.post(`/cuentas/${idCuenta}/cargos`, datos)
  return respuesta.data
}

export async function cancelarCuenta(idCuenta, { motivoCancelacion, comentario }) {
  const respuesta = await clienteHttp.put(`/cuentas/${idCuenta}/cancelar`, { motivoCancelacion, comentario })
  return respuesta.data
}

export async function exonerarMora(idCuenta, motivo) {
  const respuesta = await clienteHttp.post(`/cuentas/${idCuenta}/exoneracion-mora`, { motivo })
  return respuesta.data
}
