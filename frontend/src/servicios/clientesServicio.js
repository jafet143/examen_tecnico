import clienteHttp from './clienteHttp'

export async function listarClientes(busqueda) {
  const parametros = busqueda ? { busqueda } : {}
  const respuesta = await clienteHttp.get('/clientes', { params: parametros })
  return respuesta.data
}

export async function obtenerCliente(idCliente) {
  const respuesta = await clienteHttp.get(`/clientes/${idCliente}`)
  return respuesta.data
}

export async function crearCliente(datos) {
  const respuesta = await clienteHttp.post('/clientes', datos)
  return respuesta.data
}

export async function actualizarCliente(idCliente, datos) {
  const respuesta = await clienteHttp.put(`/clientes/${idCliente}`, datos)
  return respuesta.data
}

export async function eliminarCliente(idCliente) {
  const respuesta = await clienteHttp.delete(`/clientes/${idCliente}`)
  return respuesta.data
}

export async function listarSolicitudesDeCliente(idCliente) {
  const respuesta = await clienteHttp.get(`/clientes/${idCliente}/solicitudes`)
  return respuesta.data
}
