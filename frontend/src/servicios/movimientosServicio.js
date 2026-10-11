import clienteHttp from './clienteHttp'

export async function reversarConsumo(idConsumo, motivo) {
  const respuesta = await clienteHttp.put(`/consumos/${idConsumo}/reversar`, { motivo })
  return respuesta.data
}

export async function reversarPago(idPago, motivo) {
  const respuesta = await clienteHttp.put(`/pagos/${idPago}/reversar`, { motivo })
  return respuesta.data
}

export async function reversarCargo(idCargo, motivo) {
  const respuesta = await clienteHttp.put(`/cargos/${idCargo}/reversar`, { motivo })
  return respuesta.data
}
