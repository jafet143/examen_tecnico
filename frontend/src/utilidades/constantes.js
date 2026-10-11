export const ESTADOS_PAGO = [
  { valor: 'AL_DIA', etiqueta: 'Al día' },
  { valor: 'PAGO_PENDIENTE', etiqueta: 'Pago pendiente' },
  { valor: 'EN_MORA', etiqueta: 'En mora' }
]

export const ESTADOS_SOLICITUD = [
  { valor: 'PENDIENTE', etiqueta: 'Pendiente' },
  { valor: 'APROBADA', etiqueta: 'Aprobada' },
  { valor: 'RECHAZADA', etiqueta: 'Rechazada' }
]

export const ESTADO_TARJETA_ACTIVA = 'ACTIVA'
export const ESTADO_TARJETA_BLOQUEADA = 'BLOQUEADA'
export const ESTADO_TARJETA_REEMPLAZADA = 'REEMPLAZADA'
export const ESTADO_TARJETA_CANCELADA = 'CANCELADA'
export const ESTADO_TARJETA_VENCIDA = 'VENCIDA'

export const ESTADOS_TARJETA = [
  { valor: ESTADO_TARJETA_ACTIVA, etiqueta: 'Activa' },
  { valor: ESTADO_TARJETA_BLOQUEADA, etiqueta: 'Bloqueada' },
  { valor: ESTADO_TARJETA_REEMPLAZADA, etiqueta: 'Reemplazada' },
  { valor: ESTADO_TARJETA_CANCELADA, etiqueta: 'Cancelada' }
]

export const ESTADO_CUENTA_ACTIVA = 'ACTIVA'
export const ESTADO_CUENTA_CANCELADA = 'CANCELADA'

export const MOTIVO_REPOSICION_ROBO = 'ROBO'
export const MOTIVO_REPOSICION_EXTRAVIO = 'EXTRAVIO'
export const MOTIVO_REPOSICION_DETERIORO = 'DETERIORO'
export const MOTIVO_REPOSICION_RENOVACION = 'RENOVACION'

export const ESTADO_MOVIMIENTO_REVERSADO = 'REVERSADO'

export const TIPO_MOVIMIENTO = { CONSUMO: 'CONSUMO', PAGO: 'PAGO', CARGO: 'CARGO' }

export const USUARIO_SISTEMA = 'sistema'

export const TIPO_CARGO = { INTERES: 'INTERES', RECARGO_MORA: 'RECARGO_MORA', COMISION_REPOSICION: 'COMISION_REPOSICION' }

export const TEXTOS_CARGO = {
  [TIPO_CARGO.INTERES]: {
    nombre: 'Intereses',
    accion: 'Devolver',
    estadoReversado: 'Devuelto',
    tituloAjuste: 'Devolución de intereses',
    botonAjuste: 'Devolver intereses',
    exitoAjuste: 'Intereses devueltos correctamente'
  },
  [TIPO_CARGO.RECARGO_MORA]: {
    nombre: 'Recargo por mora',
    accion: 'Exonerar',
    estadoReversado: 'Exonerado',
    tituloAjuste: 'Exoneración de mora',
    botonAjuste: 'Exonerar recargo',
    exitoAjuste: 'Recargo por mora exonerado correctamente'
  },
  [TIPO_CARGO.COMISION_REPOSICION]: {
    nombre: 'Comisión por reposición',
    accion: 'Reversar',
    estadoReversado: 'Reversado',
    tituloAjuste: 'Reversión de comisión',
    botonAjuste: 'Reversar comisión',
    exitoAjuste: 'Comisión revertida correctamente'
  }
}

export const MOTIVO_AJUSTE_MINIMO = 5
export const MOTIVO_AJUSTE_MAXIMO = 250
