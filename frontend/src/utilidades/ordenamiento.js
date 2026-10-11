export function ordenarSolicitudesRecientesPrimero(solicitudes) {
  return [...solicitudes].sort(
    (anterior, siguiente) => new Date(siguiente.fechaSolicitud) - new Date(anterior.fechaSolicitud) || siguiente.id - anterior.id
  )
}
