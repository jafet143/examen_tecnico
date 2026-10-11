export function obtenerMensajeError(error) {
  return error.response?.data?.mensaje ?? 'No fue posible conectar con el servidor'
}

export function obtenerErroresDeCampos(error) {
  return error.response?.data?.errores ?? {}
}
