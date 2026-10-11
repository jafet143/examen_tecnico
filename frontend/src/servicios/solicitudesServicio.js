import clienteHttp from './clienteHttp'

export async function listarSolicitudes(estado) {
  const parametros = estado ? { estado } : {}
  const respuesta = await clienteHttp.get('/solicitudes', { params: parametros })
  return respuesta.data
}

export async function crearSolicitud(datos) {
  const respuesta = await clienteHttp.post('/solicitudes', datos)
  return respuesta.data
}

export async function aprobarSolicitud(idSolicitud, datos) {
  const respuesta = await clienteHttp.put(`/solicitudes/${idSolicitud}/aprobar`, datos)
  return respuesta.data
}

export async function rechazarSolicitud(idSolicitud, datos) {
  const respuesta = await clienteHttp.put(`/solicitudes/${idSolicitud}/rechazar`, datos)
  return respuesta.data
}
