import clienteHttp from './clienteHttp'

export async function obtenerCatalogoTiposTarjeta() {
  const respuesta = await clienteHttp.get('/catalogos/tipos-tarjeta')
  return respuesta.data
}
