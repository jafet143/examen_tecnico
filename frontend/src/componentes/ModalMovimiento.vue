<script setup>
import CampoMoneda from './CampoMoneda.vue'
import { reactive, ref, computed, watch } from 'vue'
import VentanaModal from './VentanaModal.vue'
import CampoFormulario from './CampoFormulario.vue'
import AvisoInformativo from './AvisoInformativo.vue'
import MensajeAlerta from './MensajeAlerta.vue'
import { registrarConsumo, registrarPago } from '../servicios/cuentasServicio'
import { formatearMoneda, formatearFecha, formatearNumeroEnmascarado, obtenerFechaHoraLocalActual, obtenerUltimosCuatroDigitos } from '../utilidades/formatos'
import { esTarjetaVigenteVencida } from '../utilidades/tarjetas'
import { usarCatalogoTiposTarjeta } from '../composables/usarCatalogoTiposTarjeta'
import { obtenerMensajeError, obtenerErroresDeCampos } from '../utilidades/errores'

const props = defineProps({
  visible: { type: Boolean, default: false },
  cuenta: { type: Object, required: true },
  tipoMovimiento: { type: String, required: true }
})
const emit = defineEmits(['cerrar', 'registrado'])

const { tasaInteresMensual, diaLimitePago, cargarCatalogo } = usarCatalogoTiposTarjeta()

const tarjetaVigente = computed(() => props.cuenta.tarjetaVigente ?? null)
const esConsumo = computed(() => props.tipoMovimiento === 'CONSUMO')
const titulo = computed(() => (esConsumo.value ? 'Registrar consumo' : 'Registrar pago'))
const descripcionModal = computed(() =>
  esConsumo.value ? 'El consumo aumentará la deuda de esta cuenta y se hará con la tarjeta vigente.' : 'El pago reducirá la deuda actual de esta cuenta.'
)

const formularioVacio = () => ({ monto: '', descripcion: '', fecha: '' })
const formulario = reactive(formularioVacio())
const erroresEnvio = ref({})
const mensajeError = ref('')
const enviando = ref(false)
const fechaMaxima = ref(obtenerFechaHoraLocalActual())

const limiteCredito = computed(() => Number(props.cuenta.limiteCredito))
const deudaActual = computed(() => Number(props.cuenta.saldoActual))
const creditoDisponible = computed(() => Number(props.cuenta.creditoDisponible))
const montoIngresado = computed(() => Number(formulario.monto))
const hayMonto = computed(() => formulario.monto !== '' && montoIngresado.value > 0)

const montoMaximo = computed(() => (esConsumo.value ? creditoDisponible.value : deudaActual.value))
const enMora = computed(() => props.cuenta.estadoPago === 'EN_MORA')
const consumoBloqueadoPorMora = computed(() => esConsumo.value && enMora.value)
const consumoBloqueadoPorBloqueo = computed(() => esConsumo.value && tarjetaVigente.value?.estadoTarjeta === 'BLOQUEADA')
const consumoBloqueadoPorVencimiento = computed(() => esConsumo.value && esTarjetaVigenteVencida(tarjetaVigente.value))
const consumoBloqueado = computed(() => consumoBloqueadoPorBloqueo.value || consumoBloqueadoPorVencimiento.value || consumoBloqueadoPorMora.value)
const montoMora = computed(() => Number(props.cuenta.montoEnMora))
const montoExcedido = computed(() => hayMonto.value && montoIngresado.value > montoMaximo.value)
const excesoSobreMaximo = computed(() => montoIngresado.value - montoMaximo.value)

const deudaResultante = computed(() =>
  esConsumo.value ? deudaActual.value + montoIngresado.value : deudaActual.value - montoIngresado.value
)
const disponibleResultante = computed(() => Math.max(0, limiteCredito.value - deudaResultante.value))

const pagoMinimoPendiente = computed(() => Number(props.cuenta.pagoMinimoPendiente ?? 0))
const pagoContadoPendiente = computed(() => Number(props.cuenta.pagoContadoPendiente ?? 0))
const opcionesPago = computed(() =>
  [
    enMora.value && { clave: 'MORA', etiqueta: 'Monto en mora', monto: montoMora.value },
    { clave: 'MINIMO', etiqueta: 'Pago mínimo', monto: pagoMinimoPendiente.value },
    { clave: 'CONTADO', etiqueta: 'Pago de contado', monto: pagoContadoPendiente.value }
  ].filter((opcion) => opcion && opcion.monto > 0)
)
const opcionSeleccionada = computed(
  () => opcionesPago.value.find((opcion) => hayMonto.value && Math.abs(opcion.monto - montoIngresado.value) < 0.005)?.clave ?? 'OTRO'
)
function seleccionarOpcionPago(opcion) {
  formulario.monto = opcion.monto.toFixed(2)
}
const pagoNoCubreMinimo = computed(
  () => !esConsumo.value && hayMonto.value && !montoExcedido.value && pagoMinimoPendiente.value > 0 && montoIngresado.value < pagoMinimoPendiente.value
)

const errorMonto = computed(() => {
  if (erroresEnvio.value.monto) return erroresEnvio.value.monto
  if (formulario.monto !== '' && !(montoIngresado.value > 0)) return 'El monto debe ser mayor a cero'
  if (montoExcedido.value) {
    return esConsumo.value
      ? `Supera el disponible por ${formatearMoneda(excesoSobreMaximo.value)}.`
      : `Supera la deuda actual por ${formatearMoneda(excesoSobreMaximo.value)}.`
  }
  return ''
})

const mensajeCoberturaMora = computed(() => {
  if (esConsumo.value || !enMora.value) return ''
  if (montoIngresado.value >= montoMora.value) return ' Con este pago la cuenta queda al día.'
  return ` Con este pago aún faltan ${formatearMoneda(montoMora.value - montoIngresado.value)} para cubrir la mora.`
})

const descripcionTarjeta = computed(
  () => `Cuenta ${props.cuenta.descripcionTipoTarjeta} ${formatearNumeroEnmascarado(props.cuenta.numeroCuentaEnmascarado)} · tarjeta vigente •••• ${obtenerUltimosCuatroDigitos(tarjetaVigente.value?.numeroTarjetaEnmascarado)} · ${props.cuenta.nombreCliente}`
)

watch(
  () => props.visible,
  (esVisible) => {
    if (!esVisible) return
    cargarCatalogo()
    Object.assign(formulario, formularioVacio())
    erroresEnvio.value = {}
    mensajeError.value = ''
    fechaMaxima.value = obtenerFechaHoraLocalActual()
  }
)

watch(
  () => [formulario.monto, formulario.descripcion, formulario.fecha],
  () => {
    erroresEnvio.value = {}
  }
)

function validarCamposRestantes() {
  const errores = {}
  if (!(montoIngresado.value > 0)) errores.monto = 'El monto debe ser mayor a cero'
  if (!formulario.descripcion.trim()) errores.descripcion = 'La descripción es obligatoria'
  else if (formulario.descripcion.length > 250) errores.descripcion = 'La descripción no puede exceder 250 caracteres'
  if (formulario.fecha && formulario.fecha > obtenerFechaHoraLocalActual()) errores.fecha = 'La fecha no puede ser futura'
  erroresEnvio.value = errores
  return Object.keys(errores).length === 0
}

async function guardarMovimiento() {
  mensajeError.value = ''
  if (consumoBloqueado.value || montoExcedido.value || !validarCamposRestantes()) return
  enviando.value = true
  try {
    const datos = { monto: montoIngresado.value, descripcion: formulario.descripcion.trim() }
    if (formulario.fecha) datos.fecha = formulario.fecha.length === 16 ? `${formulario.fecha}:00` : formulario.fecha
    if (esConsumo.value) await registrarConsumo(props.cuenta.id, datos)
    else await registrarPago(props.cuenta.id, datos)
    emit('registrado', esConsumo.value ? 'Consumo registrado correctamente' : 'Pago registrado correctamente')
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
    erroresEnvio.value = obtenerErroresDeCampos(error)
  } finally {
    enviando.value = false
  }
}
</script>

<template>
  <VentanaModal :visible="visible" :titulo="titulo" :descripcion="descripcionModal" @cerrar="emit('cerrar')">
    <form novalidate @submit.prevent="guardarMovimiento">
      <div class="modal-cuerpo">
        <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

        <CampoFormulario id="cuenta-movimiento" etiqueta="Cuenta y tarjeta vigente *">
          <input id="cuenta-movimiento" type="text" :value="descripcionTarjeta" readonly />
        </CampoFormulario>

        <div class="modal-resumen">
          <template v-if="esConsumo">
            <div><p class="dato-etiqueta">Límite de crédito</p><strong>{{ formatearMoneda(limiteCredito) }}</strong></div>
            <div><p class="dato-etiqueta">Deuda actual</p><strong>{{ formatearMoneda(deudaActual) }}</strong></div>
            <div><p class="dato-etiqueta">Disponible</p><strong>{{ formatearMoneda(creditoDisponible) }}</strong></div>
          </template>
          <template v-else>
            <div><p class="dato-etiqueta">Deuda actual</p><strong>{{ formatearMoneda(deudaActual) }}</strong></div>
            <div><p class="dato-etiqueta">Pago mínimo pendiente</p><strong>{{ formatearMoneda(pagoMinimoPendiente) }}</strong></div>
            <div><p class="dato-etiqueta">Pago de contado pendiente</p><strong>{{ formatearMoneda(pagoContadoPendiente) }}</strong></div>
            <div v-if="cuenta.fechaLimitePago"><p class="dato-etiqueta">Fecha límite</p><strong>{{ formatearFecha(cuenta.fechaLimitePago) }}</strong></div>
          </template>
        </div>

        <fieldset v-if="!esConsumo" class="opciones-pago">
          <legend class="dato-etiqueta">Monto a pagar</legend>
          <button
            v-for="opcion in opcionesPago"
            :key="opcion.clave"
            type="button"
            class="opcion-pago"
            :class="{ 'opcion-pago--seleccionada': opcionSeleccionada === opcion.clave }"
            :aria-pressed="opcionSeleccionada === opcion.clave"
            @click="seleccionarOpcionPago(opcion)"
          >
            <span>{{ opcion.etiqueta }}</span>
            <strong>{{ formatearMoneda(opcion.monto) }}</strong>
          </button>
          <span class="opcion-pago" :class="{ 'opcion-pago--seleccionada': opcionSeleccionada === 'OTRO' }">
            <span>Otro monto</span>
            <strong>Escríbalo abajo</strong>
          </span>
        </fieldset>

        <div class="rejilla-campos rejilla-sin-margen">
          <CampoFormulario id="monto-movimiento" :etiqueta="esConsumo ? 'Monto del consumo *' : 'Monto del pago *'" :error="errorMonto">
            <CampoMoneda id="monto-movimiento" v-model="formulario.monto" />
          </CampoFormulario>
          <CampoFormulario
            id="fecha-movimiento"
            etiqueta="Fecha y hora"
            :error="erroresEnvio.fecha"
            ayuda="Opcional. Si se deja vacía se usa la actual."
          >
            <input id="fecha-movimiento" v-model="formulario.fecha" type="datetime-local" :max="fechaMaxima" />
          </CampoFormulario>
        </div>

        <CampoFormulario id="descripcion-movimiento" etiqueta="Descripción *" :error="erroresEnvio.descripcion">
          <textarea id="descripcion-movimiento" v-model="formulario.descripcion" rows="3" maxlength="250"></textarea>
        </CampoFormulario>

        <AvisoInformativo v-if="consumoBloqueadoPorBloqueo" variante="error" titulo="Tarjeta bloqueada">
          La tarjeta está bloqueada por {{ tarjetaVigente.descripcionMotivoBloqueo }}. No se pueden registrar consumos.
        </AvisoInformativo>
        <AvisoInformativo v-else-if="consumoBloqueadoPorVencimiento" variante="error" titulo="Tarjeta vencida">
          La tarjeta venció el {{ formatearFecha(tarjetaVigente.fechaVencimiento) }}. Debe renovarse antes de registrar consumos.
        </AvisoInformativo>
        <AvisoInformativo v-else-if="consumoBloqueadoPorMora" variante="error" titulo="Cuenta en mora">
          La cuenta tiene {{ formatearMoneda(montoMora) }} vencidos. Registra un pago para ponerte al día antes de realizar consumos.
        </AvisoInformativo>
        <AvisoInformativo v-else-if="esConsumo && montoExcedido" variante="error" titulo="No se puede registrar este consumo">
          La deuda resultante sería {{ formatearMoneda(deudaResultante) }} y excedería el límite de {{ formatearMoneda(limiteCredito) }}.
          Ingresa un monto de hasta {{ formatearMoneda(creditoDisponible) }}.
        </AvisoInformativo>
        <AvisoInformativo v-if="pagoNoCubreMinimo" variante="advertencia" titulo="El pago no cubre el pago mínimo">
          Este pago no cubre el pago mínimo del ciclo actual ({{ formatearMoneda(pagoMinimoPendiente) }}). Si no se completa
          <template v-if="cuenta.fechaLimitePago">antes del {{ formatearFecha(cuenta.fechaLimitePago) }}</template>
          <template v-else>antes de la fecha límite</template>, la cuenta entrará en mora.
        </AvisoInformativo>
        <AvisoInformativo v-if="!esConsumo && hayMonto && !montoExcedido" variante="exito" titulo="Saldo después de registrar este pago">
          La deuda será {{ formatearMoneda(deudaResultante) }} y el crédito disponible será {{ formatearMoneda(disponibleResultante) }}.
          El movimiento aún no se ha registrado.{{ mensajeCoberturaMora }}
        </AvisoInformativo>
        <AvisoInformativo v-if="!esConsumo && tasaInteresMensual" titulo="Intereses">
          Si no paga el contado<template v-if="diaLimitePago"> antes del día {{ diaLimitePago }}</template>, se cobrará un interés del {{ tasaInteresMensual }} % sobre el saldo no pagado.
        </AvisoInformativo>
      </div>

      <div class="modal-pie">
        <button type="button" class="boton boton-secundario" :disabled="enviando" @click="emit('cerrar')">Cancelar</button>
        <button type="submit" class="boton" :disabled="enviando || montoExcedido || consumoBloqueado">{{ enviando ? 'Guardando...' : titulo }}</button>
      </div>
    </form>
  </VentanaModal>
</template>

<style scoped>
.rejilla-sin-margen { margin-bottom: 0; }
.opciones-pago { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 10px; margin: 0 0 20px; padding: 0; border: 0; }
.opciones-pago legend { margin-bottom: 8px; }
.opcion-pago {
  display: flex; flex-direction: column; gap: 4px; padding: 12px 14px; text-align: left;
  border: 1px solid var(--color-borde); border-radius: var(--radio); background: var(--color-superficie);
  font: inherit; font-size: 13px; color: var(--color-texto-suave); cursor: pointer;
}
.opcion-pago strong { color: var(--color-texto); font-size: 15px; }
span.opcion-pago { cursor: default; }
.opcion-pago--seleccionada { border-color: var(--color-primario); background: var(--color-primario-suave); color: var(--color-primario); }
button.opcion-pago:focus-visible { outline: 2px solid var(--color-primario); outline-offset: 2px; }
@media (max-width: 700px) { .rejilla-sin-margen { grid-template-columns: 1fr; } }
</style>
