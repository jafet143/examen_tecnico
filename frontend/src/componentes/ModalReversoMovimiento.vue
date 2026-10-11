<script setup>
import { ref, computed, watch } from 'vue'
import VentanaModal from './VentanaModal.vue'
import CampoFormulario from './CampoFormulario.vue'
import AvisoInformativo from './AvisoInformativo.vue'
import MensajeAlerta from './MensajeAlerta.vue'
import { reversarConsumo, reversarPago, reversarCargo } from '../servicios/movimientosServicio'
import { TIPO_MOVIMIENTO, TEXTOS_CARGO, MOTIVO_AJUSTE_MINIMO, MOTIVO_AJUSTE_MAXIMO } from '../utilidades/constantes'
import { formatearMoneda, formatearFecha } from '../utilidades/formatos'
import { obtenerMensajeError, obtenerErroresDeCampos } from '../utilidades/errores'

const MOTIVO_MINIMO = MOTIVO_AJUSTE_MINIMO
const MOTIVO_MAXIMO = MOTIVO_AJUSTE_MAXIMO

const props = defineProps({
  visible: { type: Boolean, default: false },
  movimiento: { type: Object, default: null },
  cuenta: { type: Object, required: true }
})
const emit = defineEmits(['cerrar', 'reversado'])

const motivo = ref('')
const erroresCampos = ref({})
const mensajeError = ref('')
const enviando = ref(false)

const esConsumo = computed(() => props.movimiento?.tipoMovimiento === TIPO_MOVIMIENTO.CONSUMO)
const esCargo = computed(() => props.movimiento?.tipoMovimiento === TIPO_MOVIMIENTO.CARGO)
const textosCargo = computed(() => (esCargo.value ? TEXTOS_CARGO[props.movimiento.tipoCargo] : null))
const nombreMovimiento = computed(() => (esCargo.value ? 'cargo' : esConsumo.value ? 'consumo' : 'pago'))
const tituloModal = computed(() => textosCargo.value?.tituloAjuste ?? `Reversar ${nombreMovimiento.value}`)
const textoBotonConfirmar = computed(() => textosCargo.value?.botonAjuste ?? `Reversar ${nombreMovimiento.value}`)
const tipoMostrado = computed(() => textosCargo.value?.nombre ?? (esConsumo.value ? 'Consumo' : 'Pago'))
const mensajeExito = computed(() => textosCargo.value?.exitoAjuste ?? (esConsumo.value ? 'Consumo reversado correctamente' : 'Pago reversado correctamente'))
const saldoActual = computed(() => Number(props.cuenta.saldoActual))
const limiteCredito = computed(() => Number(props.cuenta.limiteCredito))
const montoMovimiento = computed(() => Number(props.movimiento?.monto ?? 0))
const saldoResultante = computed(() => (esConsumo.value || esCargo.value ? saldoActual.value - montoMovimiento.value : saldoActual.value + montoMovimiento.value))

const errorSaldo = computed(() => {
  if (!props.movimiento) return ''
  if ((esConsumo.value || esCargo.value) && saldoResultante.value < 0) {
    return `No se puede reversar el ${nombreMovimiento.value}: el saldo quedaría en ${formatearMoneda(saldoResultante.value)}. Reverse primero el pago que lo cubrió`
  }
  if (!esConsumo.value && !esCargo.value && saldoResultante.value > limiteCredito.value) {
    return `No se puede reversar el pago: el saldo de ${formatearMoneda(saldoResultante.value)} superaría el límite de crédito de ${formatearMoneda(limiteCredito.value)}`
  }
  return ''
})

watch(
  () => props.visible,
  (esVisible) => {
    if (!esVisible) return
    motivo.value = ''
    erroresCampos.value = {}
    mensajeError.value = ''
  }
)

watch(motivo, () => {
  erroresCampos.value = {}
})

async function confirmarReverso() {
  mensajeError.value = ''
  if (errorSaldo.value) return
  const motivoLimpio = motivo.value.trim()
  if (motivoLimpio.length < MOTIVO_MINIMO || motivoLimpio.length > MOTIVO_MAXIMO) {
    erroresCampos.value = { motivo: `El motivo es obligatorio (entre ${MOTIVO_MINIMO} y ${MOTIVO_MAXIMO} caracteres)` }
    return
  }
  enviando.value = true
  try {
    if (esCargo.value) await reversarCargo(props.movimiento.id, motivoLimpio)
    else if (esConsumo.value) await reversarConsumo(props.movimiento.id, motivoLimpio)
    else await reversarPago(props.movimiento.id, motivoLimpio)
    emit('reversado', mensajeExito.value)
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
    erroresCampos.value = obtenerErroresDeCampos(error)
  } finally {
    enviando.value = false
  }
}
</script>

<template>
  <VentanaModal
    :visible="visible && movimiento !== null"
    :titulo="tituloModal"
    descripcion="El movimiento no se borra: queda marcado como reversado y deja de contar en el saldo."
    @cerrar="emit('cerrar')"
  >
    <form v-if="movimiento" novalidate @submit.prevent="confirmarReverso">
      <div class="modal-cuerpo">
        <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

        <div class="modal-resumen">
          <div><p class="dato-etiqueta">Tipo</p><strong>{{ tipoMostrado }}</strong></div>
          <div><p class="dato-etiqueta">Fecha</p><strong>{{ formatearFecha(movimiento.fecha) }}</strong></div>
          <div><p class="dato-etiqueta">Monto</p><strong>{{ formatearMoneda(movimiento.monto) }}</strong></div>
          <div class="dato-descripcion"><p class="dato-etiqueta">Descripción</p><strong>{{ movimiento.descripcion }}</strong></div>
        </div>

        <AvisoInformativo v-if="errorSaldo" variante="error" :titulo="`No se puede reversar este ${nombreMovimiento}`">
          {{ errorSaldo }}
        </AvisoInformativo>
        <AvisoInformativo v-else variante="advertencia" titulo="Efecto en el saldo">
          El saldo pasará de {{ formatearMoneda(saldoActual) }} a {{ formatearMoneda(saldoResultante) }}.
        </AvisoInformativo>

        <CampoFormulario id="motivo-reverso" etiqueta="Motivo *" :error="erroresCampos.motivo" :ayuda="`Obligatorio. Entre ${MOTIVO_MINIMO} y ${MOTIVO_MAXIMO} caracteres.`">
          <textarea id="motivo-reverso" v-model="motivo" rows="3" :maxlength="MOTIVO_MAXIMO"></textarea>
        </CampoFormulario>
      </div>
      <div class="modal-pie">
        <button type="button" class="boton boton-secundario" :disabled="enviando" @click="emit('cerrar')">Cancelar</button>
        <button type="submit" class="boton boton-peligro" :disabled="enviando || Boolean(errorSaldo)">
          {{ enviando ? 'Procesando...' : textoBotonConfirmar }}
        </button>
      </div>
    </form>
  </VentanaModal>
</template>

<style scoped>
.dato-descripcion { grid-column: 1 / -1; }
</style>
