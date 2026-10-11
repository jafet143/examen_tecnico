import clienteHttp from './clienteHttp'

export async function obtenerTarjeta(idTarjeta) {
  const respuesta = await clienteHttp.get(`/tarjetas/${idTarjeta}`)
  return respuesta.data
}

export async function bloquearTarjeta(idTarjeta, datos) {
  const respuesta = await clienteHttp.put(`/tarjetas/${idTarjeta}/bloquear`, datos)
  return respuesta.data
}

export async function desbloquearTarjeta(idTarjeta) {
  const respuesta = await clienteHttp.put(`/tarjetas/${idTarjeta}/desbloquear`)
  return respuesta.data
}

export async function reponerTarjeta(idTarjeta, { motivoReposicion, comentario, denunciaPresentada, tarjetaDeterioradaPresentada }) {
  const respuesta = await clienteHttp.post(`/tarjetas/${idTarjeta}/reposiciones`, {
    motivoReposicion,
    comentario,
    denunciaPresentada,
    tarjetaDeterioradaPresentada
  })
  return respuesta.data
}

export async function renovarTarjeta(idTarjeta) {
  const respuesta = await clienteHttp.post(`/tarjetas/${idTarjeta}/renovaciones`)
  return respuesta.data
}
