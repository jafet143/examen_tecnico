<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import EsqueletoCarga from '../componentes/EsqueletoCarga.vue'
import EncabezadoPagina from '../componentes/EncabezadoPagina.vue'
import IconoSistema from '../componentes/IconoSistema.vue'
import MensajeAlerta from '../componentes/MensajeAlerta.vue'
import AvisoInformativo from '../componentes/AvisoInformativo.vue'
import TarjetaVisual from '../componentes/TarjetaVisual.vue'
import EtiquetaEstado from '../componentes/EtiquetaEstado.vue'
import InsigniasEstadoCuenta from '../componentes/InsigniasEstadoCuenta.vue'
import ModalBloqueoTarjeta from '../componentes/ModalBloqueoTarjeta.vue'
import ModalReposicionTarjeta from '../componentes/ModalReposicionTarjeta.vue'
import ModalReversoMovimiento from '../componentes/ModalReversoMovimiento.vue'
import ModalCargo from '../componentes/ModalCargo.vue'
import ModalExoneracionMora from '../componentes/ModalExoneracionMora.vue'
import ModalCancelacionCuenta from '../componentes/ModalCancelacionCuenta.vue'
import MenuAcciones from '../componentes/MenuAcciones.vue'
import VentanaModal from '../componentes/VentanaModal.vue'
import ModalMovimiento from '../componentes/ModalMovimiento.vue'
import NumeroProtegido from '../componentes/NumeroProtegido.vue'
import { obtenerCuenta, listarTarjetasDeCuenta, listarMovimientosDeCuenta } from '../servicios/cuentasServicio'
import { desbloquearTarjeta, renovarTarjeta } from '../servicios/tarjetasServicio'
import { usarNotificacion } from '../composables/usarNotificacion'
import { usarCatalogoTiposTarjeta } from '../composables/usarCatalogoTiposTarjeta'
import { tienePermiso } from '../autenticacion/servicioAutenticacion'
import { PERMISOS } from '../utilidades/permisos'
import {
  ESTADO_TARJETA_ACTIVA,
  ESTADO_TARJETA_BLOQUEADA,
  ESTADO_TARJETA_REEMPLAZADA,
  ESTADO_TARJETA_VENCIDA,
  ESTADO_CUENTA_CANCELADA,
  ESTADO_MOVIMIENTO_REVERSADO,
  TIPO_MOVIMIENTO,
  TEXTOS_CARGO,
  USUARIO_SISTEMA
} from '../utilidades/constantes'
import { esTarjetaVigenteVencida } from '../utilidades/tarjetas'
import { formatearMoneda, formatearFecha, formatearNumeroEnmascarado, obtenerUltimosCuatroDigitos } from '../utilidades/formatos'
import { obtenerMensajeError } from '../utilidades/errores'

const ruta = useRoute()
const idCuenta = computed(() => Number(ruta.params.idCuenta))
const { mostrarNotificacion } = usarNotificacion()
const { diaCorte, diaLimitePago, comisionReposicionExtravio, porcentajePagoMinimo, tasaInteresMensual, cargarCatalogo } = usarCatalogoTiposTarjeta()

const cuenta = ref(null)
const tarjetas = ref([])
const movimientos = ref([])
const cargando = ref(true)
const mensajeError = ref('')
const bloqueoSolicitado = ref(false)
const reposicionSolicitada = ref(false)
const desbloqueoSolicitado = ref(false)
const desbloqueando = ref(false)
const renovacionSolicitada = ref(false)
const renovando = ref(false)
const cargoSolicitado = ref(false)
const exoneracionSolicitada = ref(false)
const cancelacionSolicitada = ref(false)
const tipoMovimientoAbierto = ref(null)
const movimientoAReversar = ref(null)
const puedeRegistrarConsumo = tienePermiso(PERMISOS.CONSUMOS_REGISTRAR)
const puedeRegistrarPago = tienePermiso(PERMISOS.PAGOS_REGISTRAR)
const puedeReponer = tienePermiso(PERMISOS.TARJETAS_REPONER)
const puedeBloquearTarjetas = tienePermiso(PERMISOS.TARJETAS_BLOQUEAR)
const puedeDesbloquearTarjetas = tienePermiso(PERMISOS.TARJETAS_DESBLOQUEAR)
const puedeReversar = tienePermiso(PERMISOS.MOVIMIENTOS_REVERSAR)
const puedeRenovarTarjetas = tienePermiso(PERMISOS.TARJETAS_RENOVAR)
const puedeCancelarCuentas = tienePermiso(PERMISOS.CUENTAS_CANCELAR)
const puedeRegistrarCargo = tienePermiso(PERMISOS.CARGOS_REGISTRAR)
const puedeAjustarCargos = tienePermiso(PERMISOS.CARGOS_AJUSTAR)

const tarjetaVigente = computed(() => cuenta.value?.tarjetaVigente ?? null)
const cancelada = computed(() => cuenta.value?.estadoCuenta === ESTADO_CUENTA_CANCELADA)
const bloqueada = computed(() => tarjetaVigente.value?.estadoTarjeta === ESTADO_TARJETA_BLOQUEADA)
const vencida = computed(() => !cancelada.value && esTarjetaVigenteVencida(tarjetaVigente.value))
const mostrarRenovar = computed(() => !cancelada.value && puedeRenovarTarjetas && tarjetaVigente.value?.puedeRenovarse === true)
const mostrarBloquear = computed(() => !cancelada.value && puedeBloquearTarjetas && tarjetaVigente.value?.estadoTarjeta === ESTADO_TARJETA_ACTIVA)
const mostrarDesbloquear = computed(() => !cancelada.value && puedeDesbloquearTarjetas && bloqueada.value && tarjetaVigente.value.puedeDesbloquearse === true)
const mostrarExonerarMora = computed(() => !cancelada.value && puedeAjustarCargos && cuenta.value.tieneRecargosMoraPendientes === true)
const mostrarAjustes = computed(() => !cancelada.value && (puedeReversar || puedeAjustarCargos))
const bloqueoDefinitivo = computed(() => bloqueada.value && tarjetaVigente.value.puedeDesbloquearse === false)
const enMora = computed(() => cuenta.value?.estadoPago === 'EN_MORA')
const consumoDeshabilitado = computed(() => bloqueada.value || vencida.value || enMora.value)
const motivoConsumoDeshabilitado = computed(() => {
  if (bloqueada.value) return 'La tarjeta vigente está bloqueada: no se permiten consumos'
  if (vencida.value) return `La tarjeta venció el ${formatearFecha(tarjetaVigente.value.fechaVencimiento)}: renuévela para registrar consumos`
  if (enMora.value) return 'La cuenta está en mora: registra un pago para ponerte al día antes de realizar consumos'
  return ''
})
const detalleBloqueo = computed(() =>
  [
    tarjetaVigente.value.fechaBloqueo && `el ${formatearFecha(tarjetaVigente.value.fechaBloqueo)}`,
    tarjetaVigente.value.usuarioBloqueo && `por ${tarjetaVigente.value.usuarioBloqueo}`
  ].filter(Boolean).join(' ')
)
const subtituloPagina = computed(() => `Titular: ${cuenta.value.nombreCliente} · Código de cliente ${cuenta.value.codigoCliente}`)
const tieneCicloFacturacion = computed(() => Boolean(cuenta.value?.fechaUltimoCorte))
const pagoMinimoPendienteMayorACero = computed(() => Number(cuenta.value?.pagoMinimoPendiente) > 0)
const pagoContadoPendienteMayorACero = computed(() => Number(cuenta.value?.pagoContadoPendiente) > 0)
const pagadoDelCorte = computed(() => Number(cuenta.value?.pagoContado ?? 0) - Number(cuenta.value?.pagoContadoPendiente ?? 0))
const montoSobregiro = computed(() => Math.max(0, Number(cuenta.value.saldoActual) - Number(cuenta.value.limiteCredito)))
const creditoDisponible = computed(() => Math.max(0, Number(cuenta.value.creditoDisponible)))
const etiquetaTarjetaActual = computed(() => (cancelada.value ? 'Última tarjeta' : 'Tarjeta vigente'))
const detalleCancelacion = computed(() =>
  [
    cuenta.value.descripcionMotivoCancelacion && `Motivo: ${cuenta.value.descripcionMotivoCancelacion}`,
    cuenta.value.fechaCancelacion && `Cancelada el ${formatearFecha(cuenta.value.fechaCancelacion)}`,
    cuenta.value.usuarioCancelacion && `por ${cuenta.value.usuarioCancelacion}`,
    cuenta.value.comentarioCancelacion && `Comentario: ${cuenta.value.comentarioCancelacion}`
  ].filter(Boolean).join(' · ')
)
const porcentajeUsado = computed(() => {
  const limite = Number(cuenta.value.limiteCredito)
  return limite > 0 ? Math.min(100, (Number(cuenta.value.saldoActual) / limite) * 100) : 0
})
const porcentajeDisponible = computed(() => Math.round(100 - porcentajeUsado.value))

const movimientosVigentes = computed(() => movimientos.value.filter((movimiento) => !movimiento.reversado))
const sumarMonto = (tipo) =>
  movimientosVigentes.value.filter((movimiento) => movimiento.tipoMovimiento === tipo).reduce((total, movimiento) => total + Number(movimiento.monto), 0)
const totalConsumos = computed(() => sumarMonto(TIPO_MOVIMIENTO.CONSUMO))
const totalPagos = computed(() => sumarMonto(TIPO_MOVIMIENTO.PAGO))
const totalCargos = computed(() => sumarMonto(TIPO_MOVIMIENTO.CARGO))
const cantidadReversados = computed(() => movimientos.value.length - movimientosVigentes.value.length)

const esPago = (movimiento) => movimiento.tipoMovimiento === TIPO_MOVIMIENTO.PAGO

function describirTipoMovimiento(movimiento) {
  if (movimiento.tipoMovimiento === TIPO_MOVIMIENTO.PAGO) return { nombre: 'Pago', icono: 'flecha-abajo-izquierda', clase: 'tipo-pago' }
  if (movimiento.tipoMovimiento === TIPO_MOVIMIENTO.CARGO) return { nombre: TEXTOS_CARGO[movimiento.tipoCargo]?.nombre ?? 'Cargo', icono: 'recibo', clase: 'tipo-cargo' }
  return { nombre: 'Consumo', icono: 'flecha-arriba-derecha', clase: 'tipo-consumo' }
}

function etiquetaReverso(movimiento) {
  return (movimiento.tipoMovimiento === TIPO_MOVIMIENTO.CARGO && TEXTOS_CARGO[movimiento.tipoCargo]?.estadoReversado) || 'Reversado'
}

function textoAccionMovimiento(movimiento) {
  if (movimiento.reversado) return ''
  if (movimiento.tipoMovimiento === TIPO_MOVIMIENTO.CARGO) return puedeAjustarCargos ? (TEXTOS_CARGO[movimiento.tipoCargo]?.accion ?? 'Reversar') : ''
  return puedeReversar ? 'Reversar' : ''
}
const esVigente = (tarjeta) => tarjeta.estadoTarjeta !== ESTADO_TARJETA_REEMPLAZADA

const opcionesMenu = computed(() =>
  cancelada.value
    ? []
    : [
        puedeReponer && { clave: 'reponer', texto: 'Reponer tarjeta', icono: 'tarjeta' },
        mostrarRenovar.value && { clave: 'renovar', texto: 'Renovar tarjeta', icono: 'renovar' },
        mostrarBloquear.value && { clave: 'bloquear', texto: 'Bloquear tarjeta', icono: 'candado' },
        mostrarDesbloquear.value && { clave: 'desbloquear', texto: 'Desbloquear tarjeta', icono: 'candado-abierto' },
        puedeRegistrarCargo && { clave: 'cargo', texto: 'Registrar cargo', icono: 'recibo' },
        mostrarExonerarMora.value && { clave: 'exonerar-mora', texto: 'Exonerar mora', icono: 'documento-check' },
        puedeCancelarCuentas && { clave: 'cancelar', texto: 'Cancelar cuenta', icono: 'prohibido', peligro: true }
      ].filter(Boolean)
)

function ejecutarAccionDelMenu(clave) {
  const acciones = {
    reponer: () => (reposicionSolicitada.value = true),
    renovar: () => (renovacionSolicitada.value = true),
    bloquear: () => (bloqueoSolicitado.value = true),
    desbloquear: () => (desbloqueoSolicitado.value = true),
    cargo: () => (cargoSolicitado.value = true),
    'exonerar-mora': () => (exoneracionSolicitada.value = true),
    cancelar: () => (cancelacionSolicitada.value = true)
  }
  acciones[clave]()
}

function describirReverso(movimiento) {
  return [
    `Motivo: ${movimiento.motivoReverso}`,
    movimiento.fechaReverso && formatearFecha(movimiento.fechaReverso),
    movimiento.usuarioReverso && `por ${movimiento.usuarioReverso}`
  ].filter(Boolean).join(' · ')
}

async function cargarDatos() {
  mensajeError.value = ''
  try {
    ;[cuenta.value, tarjetas.value, movimientos.value] = await Promise.all([
      obtenerCuenta(idCuenta.value),
      listarTarjetasDeCuenta(idCuenta.value),
      listarMovimientosDeCuenta(idCuenta.value)
    ])
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    cargando.value = false
  }
}

async function alBloquearTarjeta() {
  bloqueoSolicitado.value = false
  mostrarNotificacion('Tarjeta bloqueada correctamente')
  await cargarDatos()
}

async function alReponerTarjeta(tarjetaNueva, comisionCobrada) {
  reposicionSolicitada.value = false
  const cobro = comisionCobrada ? ` Se cobró una comisión de ${formatearMoneda(comisionCobrada)} a la cuenta.` : ''
  mostrarNotificacion(`Tarjeta repuesta correctamente. La nueva tarjeta termina en ${obtenerUltimosCuatroDigitos(tarjetaNueva.numeroTarjetaEnmascarado)}.${cobro}`)
  await cargarDatos()
}

async function confirmarRenovacion() {
  renovando.value = true
  mensajeError.value = ''
  try {
    const tarjetaNueva = await renovarTarjeta(tarjetaVigente.value.id)
    renovacionSolicitada.value = false
    mostrarNotificacion(`Tarjeta renovada correctamente. La nueva tarjeta termina en ${obtenerUltimosCuatroDigitos(tarjetaNueva.numeroTarjetaEnmascarado)}`)
    await cargarDatos()
  } catch (error) {
    renovacionSolicitada.value = false
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    renovando.value = false
  }
}

async function alRegistrarCargo(mensaje) {
  cargoSolicitado.value = false
  mostrarNotificacion(mensaje)
  await cargarDatos()
}

async function alExonerarMora(resultado) {
  exoneracionSolicitada.value = false
  const cantidad = resultado.cantidadCargosExonerados
  mostrarNotificacion(`Se ${cantidad === 1 ? 'exoneró 1 recargo' : `exoneraron ${cantidad} recargos`} por ${formatearMoneda(resultado.montoExonerado)}`)
  await cargarDatos()
}

async function alCancelarCuenta() {
  cancelacionSolicitada.value = false
  mostrarNotificacion('Cuenta cancelada correctamente')
  await cargarDatos()
}

async function confirmarDesbloqueo() {
  desbloqueando.value = true
  mensajeError.value = ''
  try {
    await desbloquearTarjeta(tarjetaVigente.value.id)
    desbloqueoSolicitado.value = false
    mostrarNotificacion('Tarjeta desbloqueada correctamente')
    await cargarDatos()
  } catch (error) {
    desbloqueoSolicitado.value = false
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    desbloqueando.value = false
  }
}

async function alRegistrarMovimiento(mensaje) {
  tipoMovimientoAbierto.value = null
  mostrarNotificacion(mensaje)
  await cargarDatos()
}

async function alReversarMovimiento(mensaje) {
  movimientoAReversar.value = null
  mostrarNotificacion(mensaje)
  await cargarDatos()
}

onMounted(() => {
  cargarDatos()
  cargarCatalogo()
})
</script>

<template>
  <div>
    <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />
    <EsqueletoCarga v-if="cargando" variante="detalle" etiqueta="Cargando cuenta" />

    <template v-else-if="cuenta && tarjetaVigente">
      <EncabezadoPagina
        :migas="[{ texto: 'Cuentas y tarjetas', ruta: '/cuentas' }, { texto: 'Detalle de cuenta' }]"
        :titulo="`Cuenta ${cuenta.descripcionTipoTarjeta} · ${formatearNumeroEnmascarado(cuenta.numeroCuentaEnmascarado)}`"
        :descripcion="subtituloPagina"
        ruta-regreso="/cuentas"
      >
        <template v-if="!cancelada">
          <button
            v-if="puedeRegistrarConsumo"
            type="button"
            class="boton boton-secundario"
            :disabled="consumoDeshabilitado"
            :title="motivoConsumoDeshabilitado"
            @click="tipoMovimientoAbierto = 'CONSUMO'"
          >
            <IconoSistema nombre="mas" :tamano="18" />Registrar consumo
          </button>
          <button v-if="puedeRegistrarPago" type="button" class="boton" @click="tipoMovimientoAbierto = 'PAGO'">
            <IconoSistema nombre="flecha-abajo-izquierda" :tamano="18" />Registrar pago
          </button>
          <MenuAcciones :opciones="opcionesMenu" @seleccionar="ejecutarAccionDelMenu" />
        </template>
        <template #insignia>
          <InsigniasEstadoCuenta :cuenta="cuenta" />
        </template>
      </EncabezadoPagina>

      <div v-if="cancelada" class="aviso-superior">
        <AvisoInformativo variante="neutro" titulo="Cuenta cancelada">
          {{ detalleCancelacion }}. La cuenta y su tarjeta están canceladas; el historial de tarjetas y movimientos se conserva y no admite nuevas operaciones.
        </AvisoInformativo>
      </div>

      <div v-if="vencida" class="aviso-superior">
        <AvisoInformativo variante="advertencia" titulo="Tarjeta vencida">
          La tarjeta venció el {{ formatearFecha(tarjetaVigente.fechaVencimiento) }}. Renuévela para seguir registrando consumos.
          <button v-if="mostrarRenovar" type="button" class="boton boton-pequeno boton-en-aviso" @click="renovacionSolicitada = true">
            <IconoSistema nombre="renovar" :tamano="16" />Renovar tarjeta
          </button>
        </AvisoInformativo>
      </div>

      <div v-if="bloqueada" class="aviso-superior">
        <AvisoInformativo variante="error" :titulo="`Tarjeta vigente bloqueada · ${tarjetaVigente.descripcionMotivoBloqueo ?? ''}`">
          Bloqueada {{ detalleBloqueo }}.
          <template v-if="tarjetaVigente.comentarioBloqueo">Comentario: {{ tarjetaVigente.comentarioBloqueo }}.</template>
          No se permiten consumos; los pagos siguen habilitados.
          <template v-if="bloqueoDefinitivo">Esta tarjeta no puede desbloquearse. La única opción es generar una reposición.</template>
          <button v-if="bloqueoDefinitivo && puedeReponer" type="button" class="boton boton-peligro boton-pequeno boton-en-aviso" @click="reposicionSolicitada = true">
            <IconoSistema nombre="tarjeta" :tamano="16" />Generar reposición
          </button>
        </AvisoInformativo>
      </div>

      <div v-if="enMora && !cancelada" class="aviso-superior">
        <AvisoInformativo variante="error" :titulo="`Cuenta en mora · ${cuenta.diasMora} ${cuenta.diasMora === 1 ? 'día' : 'días'}`">
          Monto vencido {{ formatearMoneda(cuenta.montoEnMora) }}. Registra un pago para ponerte al día; mientras tanto no se permiten consumos ni nuevas solicitudes del titular.
        </AvisoInformativo>
      </div>

      <div class="fila-tarjeta">
        <TarjetaVisual
          :numero-tarjeta="tarjetaVigente.numeroTarjetaEnmascarado"
          :fecha-vencimiento="tarjetaVigente.fechaVencimiento"
          :codigo-tipo="cuenta.tipoTarjeta"
          :tipo="cuenta.descripcionTipoTarjeta"
          :titular="cuenta.nombreCliente"
          :bloqueada="bloqueada"
          :cancelada="cancelada"
        />
        <section class="panel panel-credito">
          <div class="identificadores-cuenta">
            <div><p class="dato-etiqueta">N.º de cuenta</p><p class="dato-valor celda-fuerte"><NumeroProtegido :enmascarado="cuenta.numeroCuentaEnmascarado" :completo="cuenta.numeroCuentaCompleto" /></p></div>
            <div><p class="dato-etiqueta">{{ etiquetaTarjetaActual }}</p><p class="dato-valor celda-fuerte"><NumeroProtegido :enmascarado="tarjetaVigente.numeroTarjetaEnmascarado" :completo="tarjetaVigente.numeroTarjetaCompleto" /></p></div>
          </div>
          <div class="cifras-credito">
            <div><p class="dato-etiqueta">Límite de crédito</p><p class="cifra">{{ formatearMoneda(cuenta.limiteCredito) }}</p></div>
            <div><p class="dato-etiqueta">Saldo / deuda actual</p><p class="cifra">{{ formatearMoneda(cuenta.saldoActual) }}</p></div>
            <div><p class="dato-etiqueta">Crédito disponible</p><p class="cifra cifra-destacada">{{ formatearMoneda(creditoDisponible) }}</p></div>
          </div>
          <AvisoInformativo v-if="montoSobregiro > 0" variante="advertencia" :titulo="`Sobregiro por cargos de ${formatearMoneda(montoSobregiro)}`">
            Los intereses y recargos llevaron el saldo por encima del límite de crédito. No hay crédito disponible hasta que el saldo baje del límite.
          </AvisoInformativo>
          <div>
            <div class="barra-uso" role="progressbar" :aria-valuenow="Math.round(porcentajeUsado)" aria-valuemin="0" aria-valuemax="100">
              <div class="barra-uso-relleno" :style="{ width: `${porcentajeUsado}%` }"></div>
            </div>
            <div class="leyenda-barra">
              <span>Utilizado: {{ formatearMoneda(cuenta.saldoActual) }}</span>
              <span>{{ porcentajeDisponible }}% disponible</span>
            </div>
          </div>
        </section>
      </div>

      <section v-if="tieneCicloFacturacion && !cancelada" class="panel">
        <div class="panel-cabecera">
          <div>
            <h2 class="panel-titulo">Ciclo de facturación</h2>
            <p class="panel-subtitulo">
              Estado de pago de la cuenta según el último corte
              <template v-if="porcentajePagoMinimo && tasaInteresMensual"> · Pago mínimo {{ porcentajePagoMinimo }} % del saldo al corte · Interés {{ tasaInteresMensual }} % mensual sobre lo no pagado</template>
            </p>
          </div>
        </div>
        <div class="panel-cuerpo rejilla-datos">
          <div><p class="dato-etiqueta">Fecha de corte</p><p class="dato-valor">{{ formatearFecha(cuenta.fechaUltimoCorte) }}</p></div>
          <div><p class="dato-etiqueta">Fecha límite de pago</p><p class="dato-valor">{{ formatearFecha(cuenta.fechaLimitePago) }}</p></div>
          <div><p class="dato-etiqueta">Próximo corte</p><p class="dato-valor">{{ formatearFecha(cuenta.fechaProximoCorte) }}</p></div>
          <div><p class="dato-etiqueta">Saldo al corte</p><p class="dato-valor celda-fuerte">{{ formatearMoneda(cuenta.saldoAlCorte) }}</p></div>
          <div><p class="dato-etiqueta">Pagado del corte</p><p class="dato-valor">{{ formatearMoneda(pagadoDelCorte) }}</p></div>
          <div>
            <p class="dato-etiqueta">Pago mínimo</p>
            <p class="dato-valor celda-fuerte" :class="{ 'valor-pendiente': pagoMinimoPendienteMayorACero }">{{ formatearMoneda(cuenta.pagoMinimoPendiente) }}</p>
            <p class="tabla-secundaria">pendiente de {{ formatearMoneda(cuenta.pagoMinimo) }}</p>
          </div>
          <div>
            <p class="dato-etiqueta">Pago de contado</p>
            <p class="dato-valor celda-fuerte" :class="{ 'valor-pendiente': pagoContadoPendienteMayorACero }">{{ formatearMoneda(cuenta.pagoContadoPendiente) }}</p>
            <p class="tabla-secundaria">pendiente de {{ formatearMoneda(cuenta.pagoContado) }}</p>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-cabecera">
          <div>
            <h2 class="panel-titulo">Tarjetas de la cuenta</h2>
            <p class="panel-subtitulo">
              {{ tarjetas.length }} {{ tarjetas.length === 1 ? 'tarjeta emitida' : 'tarjetas emitidas' }} · {{ cuenta.cantidadReposiciones }}
              {{ cuenta.cantidadReposiciones === 1 ? 'reposición' : 'reposiciones' }} · La más reciente primero
            </p>
          </div>
        </div>
        <div class="contenedor-tabla">
          <table class="tabla">
            <thead>
              <tr>
                <th>Tarjeta</th>
                <th>Estado</th>
                <th>Emisión</th>
                <th>Vencimiento</th>
                <th>Reposición / renovación</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="tarjeta in tarjetas" :key="tarjeta.id" :class="{ 'fila-vigente': esVigente(tarjeta) }">
                <td>
                  <span class="celda-fuerte"><NumeroProtegido :enmascarado="tarjeta.numeroTarjetaEnmascarado" :completo="tarjeta.numeroTarjetaCompleto" /></span>
                  <span v-if="esVigente(tarjeta)" class="marca-vigente">Vigente</span>
                </td>
                <td>
                  <span class="insignias-celda">
                    <EtiquetaEstado :estado="tarjeta.estadoTarjeta" :etiqueta="tarjeta.descripcionEstadoTarjeta" />
                    <EtiquetaEstado v-if="esTarjetaVigenteVencida(tarjeta)" :estado="ESTADO_TARJETA_VENCIDA" />
                  </span>
                  <p v-if="tarjeta.motivoBloqueo" class="tabla-secundaria">Bloqueo: {{ tarjeta.descripcionMotivoBloqueo }}</p>
                </td>
                <td>{{ formatearFecha(tarjeta.fechaEmision) }}</td>
                <td>{{ formatearFecha(tarjeta.fechaVencimiento) }}</td>
                <td class="celda-ajustable">
                  <template v-if="tarjeta.motivoReposicion">
                    <p>{{ tarjeta.descripcionMotivoReposicion }}</p>
                    <p class="tabla-secundaria">{{ formatearFecha(tarjeta.fechaReposicion) }}<template v-if="tarjeta.usuarioReposicion"> · {{ tarjeta.usuarioReposicion }}</template></p>
                    <p v-if="tarjeta.comentarioReposicion" class="tabla-secundaria">{{ tarjeta.comentarioReposicion }}</p>
                    <p v-if="tarjeta.denunciaPresentada" class="tabla-secundaria">Denuncia presentada</p>
                    <p v-if="tarjeta.tarjetaDeterioradaPresentada" class="tabla-secundaria">Tarjeta deteriorada presentada</p>
                    <p v-if="tarjeta.idCargoReposicion" class="tabla-secundaria">Comisión {{ formatearMoneda(comisionReposicionExtravio) }}</p>
                  </template>
                  <span v-else class="tabla-secundaria">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <div class="panel-cabecera">
          <div>
            <h2 class="panel-titulo">Historial de movimientos</h2>
            <p class="panel-subtitulo">
              {{ movimientos.length }} {{ movimientos.length === 1 ? 'movimiento' : 'movimientos' }}
              <template v-if="cantidadReversados > 0">({{ cantidadReversados }} {{ cantidadReversados === 1 ? 'reversado' : 'reversados' }})</template>
              · Ordenados por fecha, más recientes primero
            </p>
          </div>
        </div>
        <p v-if="movimientos.length === 0" class="estado-vacio">Esta cuenta aún no tiene movimientos registrados.</p>
        <div v-else class="contenedor-tabla">
          <table class="tabla">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Descripción</th>
                <th>Tarjeta</th>
                <th>Tipo</th>
                <th class="numero">Monto</th>
                <th v-if="mostrarAjustes">Acción</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="movimiento in movimientos" :key="`${movimiento.tipoMovimiento}-${movimiento.id}`" :class="{ 'fila-reversada': movimiento.reversado }">
                <td>{{ formatearFecha(movimiento.fecha) }}</td>
                <td class="celda-ajustable">
                  <span class="texto-movimiento">{{ movimiento.descripcion }}</span>
                  <p v-if="movimiento.usuarioRegistro === USUARIO_SISTEMA" class="tabla-secundaria">Generado automáticamente al vencer el ciclo</p>
                  <template v-if="movimiento.reversado">
                    <EtiquetaEstado :estado="ESTADO_MOVIMIENTO_REVERSADO" :etiqueta="etiquetaReverso(movimiento)" class="etiqueta-pequena etiqueta-junto" />
                    <p class="tabla-secundaria">{{ describirReverso(movimiento) }}</p>
                  </template>
                </td>
                <td>
                  <template v-if="movimiento.tipoMovimiento === TIPO_MOVIMIENTO.CONSUMO && movimiento.numeroTarjetaEnmascarado">•••• {{ obtenerUltimosCuatroDigitos(movimiento.numeroTarjetaEnmascarado) }}</template>
                  <span v-else class="tabla-secundaria">—</span>
                </td>
                <td>
                  <span class="tipo-movimiento" :class="describirTipoMovimiento(movimiento).clase">
                    <IconoSistema :nombre="describirTipoMovimiento(movimiento).icono" :tamano="14" />
                    {{ describirTipoMovimiento(movimiento).nombre }}
                  </span>
                </td>
                <td class="numero celda-fuerte texto-movimiento" :class="describirTipoMovimiento(movimiento).clase">
                  {{ esPago(movimiento) ? '−' : '+' }} {{ formatearMoneda(movimiento.monto) }}
                </td>
                <td v-if="mostrarAjustes">
                  <button v-if="textoAccionMovimiento(movimiento)" type="button" class="boton boton-secundario boton-pequeno boton-secundario-peligro" @click="movimientoAReversar = movimiento">
                    {{ textoAccionMovimiento(movimiento) }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pie-tabla">
          <span>Consumos: {{ formatearMoneda(totalConsumos) }} · Pagos: {{ formatearMoneda(totalPagos) }} · Cargos: {{ formatearMoneda(totalCargos) }} (sin reversados)</span>
          <strong>Deuda actual: {{ formatearMoneda(cuenta.saldoActual) }}</strong>
        </div>
      </section>

      <AvisoInformativo titulo="El saldo representa deuda, no dinero a favor">
        Los consumos suman al saldo y no pueden superar el límite de crédito. Los cargos (intereses, recargos por mora y comisiones) también suman al saldo y sí pueden superar el límite. Los pagos restan del saldo. Los movimientos reversados no cuentan. Disponible = límite de crédito − deuda actual (nunca menor a Q 0.00).
        <template v-if="diaCorte && diaLimitePago">
          El corte es el día {{ diaCorte }} de cada mes y el pago debe hacerse hasta el día {{ diaLimitePago }}.
          Si no se cubre el pago mínimo<template v-if="porcentajePagoMinimo"> ({{ porcentajePagoMinimo }} % del saldo al corte)</template>, la cuenta entra en mora;
          si no se paga el contado, se cobra automáticamente un interés<template v-if="tasaInteresMensual"> del {{ tasaInteresMensual }} %</template> sobre el saldo no pagado.
        </template>
      </AvisoInformativo>

      <ModalBloqueoTarjeta :visible="bloqueoSolicitado" :tarjeta="tarjetaVigente" @cerrar="bloqueoSolicitado = false" @bloqueada="alBloquearTarjeta" />
      <ModalReposicionTarjeta :visible="reposicionSolicitada" :tarjeta="tarjetaVigente" @cerrar="reposicionSolicitada = false" @repuesta="alReponerTarjeta" />

      <VentanaModal :visible="renovacionSolicitada" titulo="Renovar tarjeta" descripcion="La tarjeta actual quedará reemplazada por una nueva." @cerrar="renovacionSolicitada = false">
        <div class="modal-cuerpo">
          <p>
            Se emitirá una nueva tarjeta sin costo en la misma cuenta {{ formatearNumeroEnmascarado(cuenta.numeroCuentaEnmascarado) }}.
            El saldo, el límite y el historial se conservan.
          </p>
        </div>
        <div class="modal-pie">
          <button type="button" class="boton boton-secundario" :disabled="renovando" @click="renovacionSolicitada = false">Cancelar</button>
          <button type="button" class="boton" :disabled="renovando" @click="confirmarRenovacion">{{ renovando ? 'Renovando...' : 'Renovar tarjeta' }}</button>
        </div>
      </VentanaModal>

      <ModalCargo :visible="cargoSolicitado" :cuenta="cuenta" @cerrar="cargoSolicitado = false" @registrado="alRegistrarCargo" />
      <ModalExoneracionMora :visible="exoneracionSolicitada" :cuenta="cuenta" @cerrar="exoneracionSolicitada = false" @exonerada="alExonerarMora" />
      <ModalCancelacionCuenta :visible="cancelacionSolicitada" :cuenta="cuenta" @cerrar="cancelacionSolicitada = false" @cancelada="alCancelarCuenta" />

      <VentanaModal :visible="desbloqueoSolicitado" titulo="Desbloquear tarjeta" descripcion="La tarjeta volverá a aceptar consumos." @cerrar="desbloqueoSolicitado = false">
        <div class="modal-cuerpo">
          <p>¿Confirmas que deseas desbloquear la tarjeta terminada en {{ obtenerUltimosCuatroDigitos(tarjetaVigente.numeroTarjetaEnmascarado) }} de {{ cuenta.nombreCliente }}?</p>
        </div>
        <div class="modal-pie">
          <button type="button" class="boton boton-secundario" :disabled="desbloqueando" @click="desbloqueoSolicitado = false">Cancelar</button>
          <button type="button" class="boton" :disabled="desbloqueando" @click="confirmarDesbloqueo">{{ desbloqueando ? 'Desbloqueando...' : 'Desbloquear tarjeta' }}</button>
        </div>
      </VentanaModal>

      <ModalMovimiento
        :visible="tipoMovimientoAbierto !== null"
        :cuenta="cuenta"
        :tipo-movimiento="tipoMovimientoAbierto ?? 'CONSUMO'"
        @cerrar="tipoMovimientoAbierto = null"
        @registrado="alRegistrarMovimiento"
      />

      <ModalReversoMovimiento
        :visible="movimientoAReversar !== null"
        :movimiento="movimientoAReversar"
        :cuenta="cuenta"
        @cerrar="movimientoAReversar = null"
        @reversado="alReversarMovimiento"
      />
    </template>
  </div>
</template>

<style scoped>
.fila-tarjeta { display: grid; grid-template-columns: minmax(280px, 380px) minmax(0, 1fr); gap: 24px; margin-bottom: 24px; }
.fila-tarjeta > .tarjeta-visual { align-self: start; }
.panel-credito { display: flex; flex-direction: column; justify-content: space-between; gap: 20px; margin-bottom: 0; padding: 24px; }
.identificadores-cuenta { display: flex; flex-wrap: wrap; gap: 16px 40px; }
.boton-en-aviso { display: flex; width: fit-content; margin-top: 10px; }
.cifras-credito { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.cifra { font-size: 20px; font-weight: 600; }
.cifra-destacada { font-size: 28px; font-weight: 700; color: var(--color-primario); }
.leyenda-barra { display: flex; justify-content: space-between; gap: 12px; margin-top: 10px; font-size: 12px; color: var(--color-texto-suave); }
.tipo-movimiento { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 500; }
.tipo-pago { color: var(--color-exito); }
.tipo-consumo { color: var(--color-primario); }
.tipo-cargo { color: var(--color-advertencia); }
.insignias-celda { display: inline-flex; flex-wrap: wrap; gap: 6px; }
.boton-secundario-peligro { color: var(--color-peligro); }
.boton-pequeno { height: 32px; padding: 0 12px; font-size: 13px; }
.aviso-superior { margin-bottom: 24px; }
.valor-pendiente { color: var(--color-advertencia); }
.marca-vigente { margin-left: 10px; padding: 2px 8px; border-radius: 999px; background: var(--color-primario-suave); color: var(--color-primario); font-size: 11px; font-weight: 600; }
.fila-vigente { background: #f7faff; }
.fila-reversada { color: var(--color-texto-suave); }
.fila-reversada td { background: #fafbfd; }
.fila-reversada .texto-movimiento { text-decoration: line-through; opacity: 0.6; }
.etiqueta-junto { margin-left: 8px; }
.pie-tabla strong { color: var(--color-texto); font-weight: 500; }
.panel .pie-tabla { margin: 0; }
@media (max-width: 1280px) { .cifras-credito { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 900px) { .fila-tarjeta { grid-template-columns: 1fr; } }
@media (max-width: 700px) { .cifras-credito { grid-template-columns: 1fr; } }
</style>
