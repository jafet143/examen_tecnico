const formateadorMoneda = new Intl.NumberFormat('es-GT', { style: 'currency', currency: 'GTQ' })
const formateadorFecha = new Intl.DateTimeFormat('es-GT', { day: '2-digit', month: '2-digit', year: 'numeric' })
const formateadorFechaCorta = new Intl.DateTimeFormat('es-GT', { day: '2-digit', month: 'short', year: 'numeric' })

export function formatearMoneda(valor) {
  if (valor === null || valor === undefined || valor === '') return '—'
  return formateadorMoneda.format(Number(valor))
}

function convertirAFecha(texto) {
  if (texto.length === 10) {
    const [anio, mes, dia] = texto.split('-').map(Number)
    return new Date(anio, mes - 1, dia)
  }
  return new Date(texto)
}

export function formatearFecha(texto) {
  if (!texto) return '—'
  return formateadorFecha.format(convertirAFecha(texto))
}

export function obtenerUltimosCuatroDigitos(numeroTarjeta) {
  return (numeroTarjeta ?? '').slice(-4)
}

export function formatearVencimiento(fechaVencimiento) {
  if (!fechaVencimiento) return '--/--'
  const [anio, mes] = fechaVencimiento.split('-')
  return `${mes}/${anio.slice(-2)}`
}

export function calcularMesesAntiguedad(fechaInicio) {
  if (!fechaInicio) return null
  const [anio, mes, dia] = fechaInicio.split('-').map(Number)
  const hoy = new Date()
  const meses = (hoy.getFullYear() - anio) * 12 + (hoy.getMonth() + 1 - mes) - (hoy.getDate() < dia ? 1 : 0)
  return Math.max(0, meses)
}

export function describirAntiguedad(totalMeses) {
  if (totalMeses === null || totalMeses === undefined) return '—'
  const anios = Math.floor(totalMeses / 12)
  const meses = totalMeses % 12
  const textoAnios = anios > 0 ? `${anios} ${anios === 1 ? 'año' : 'años'}` : ''
  const textoMeses = meses > 0 || anios === 0 ? `${meses} ${meses === 1 ? 'mes' : 'meses'}` : ''
  return [textoAnios, textoMeses].filter(Boolean).join(' y ')
}

export function obtenerFechaIsoHoy() {
  const hoy = new Date()
  hoy.setMinutes(hoy.getMinutes() - hoy.getTimezoneOffset())
  return hoy.toISOString().slice(0, 10)
}

export function formatearNumeroEnmascarado(numeroTarjeta) {
  return `•••• •••• •••• ${obtenerUltimosCuatroDigitos(numeroTarjeta)}`
}

export function formatearNumeroCompleto(numeroCompleto) {
  return (numeroCompleto ?? '').replace(/\D/g, '').replace(/(.{4})(?=.)/g, '$1 ')
}

export function formatearCodigoSolicitud(idSolicitud) {
  return `SOL-${String(idSolicitud).padStart(3, '0')}`
}

export function formatearFechaCorta(texto) {
  if (!texto) return '—'
  return formateadorFechaCorta.format(convertirAFecha(texto)).replace(/\./g, '')
}

export function formatearPorcentaje(valor) {
  return `${Number(valor).toFixed(1)}%`
}

export function obtenerIniciales(nombre, apellido) {
  return `${(nombre ?? '').trim().charAt(0)}${(apellido ?? '').trim().charAt(0)}`.toUpperCase()
}

export function obtenerCiudadDeDireccion(direccion) {
  const partes = (direccion ?? '').split(',')
  return partes[partes.length - 1].trim()
}

export function formatearFechaHoy() {
  return formateadorFecha.format(new Date())
}

export function obtenerFechaHoraLocalActual() {
  const ahora = new Date()
  ahora.setMinutes(ahora.getMinutes() - ahora.getTimezoneOffset())
  return ahora.toISOString().slice(0, 16)
}
