<script setup>
import { ref, watch } from 'vue'
import VentanaModal from './VentanaModal.vue'
import CampoFormulario from './CampoFormulario.vue'
import AvisoInformativo from './AvisoInformativo.vue'
import MensajeAlerta from './MensajeAlerta.vue'
import { exonerarMora } from '../servicios/cuentasServicio'
import { MOTIVO_AJUSTE_MINIMO, MOTIVO_AJUSTE_MAXIMO } from '../utilidades/constantes'
import { formatearNumeroEnmascarado } from '../utilidades/formatos'
import { obtenerMensajeError, obtenerErroresDeCampos } from '../utilidades/errores'

const props = defineProps({
  visible: { type: Boolean, default: false },
  cuenta: { type: Object, required: true }
})
const emit = defineEmits(['cerrar', 'exonerada'])

const motivo = ref('')
const erroresCampos = ref({})
const mensajeError = ref('')
const enviando = ref(false)

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

async function confirmarExoneracion() {
  mensajeError.value = ''
  const motivoLimpio = motivo.value.trim()
  if (motivoLimpio.length < MOTIVO_AJUSTE_MINIMO || motivoLimpio.length > MOTIVO_AJUSTE_MAXIMO) {
    erroresCampos.value = { motivo: `El motivo es obligatorio (entre ${MOTIVO_AJUSTE_MINIMO} y ${MOTIVO_AJUSTE_MAXIMO} caracteres)` }
    return
  }
  enviando.value = true
  try {
    emit('exonerada', await exonerarMora(props.cuenta.id, motivoLimpio))
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
    :visible="visible"
    titulo="Exonerar mora"
    :descripcion="`Cuenta ${cuenta.descripcionTipoTarjeta} ${formatearNumeroEnmascarado(cuenta.numeroCuentaEnmascarado)} · ${cuenta.nombreCliente}`"
    @cerrar="emit('cerrar')"
  >
    <form novalidate @submit.prevent="confirmarExoneracion">
      <div class="modal-cuerpo">
        <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

        <AvisoInformativo variante="advertencia" titulo="Se exonerarán todos los recargos por mora pendientes">
          Los recargos quedan reversados y dejan de contar en el saldo. La exoneración no paga la deuda: si la cuenta sigue con saldo al corte sin pagar, continúa en mora hasta que se pague.
        </AvisoInformativo>

        <CampoFormulario id="motivo-exoneracion" etiqueta="Motivo de la exoneración *" :error="erroresCampos.motivo" :ayuda="`Obligatorio. Entre ${MOTIVO_AJUSTE_MINIMO} y ${MOTIVO_AJUSTE_MAXIMO} caracteres.`">
          <textarea id="motivo-exoneracion" v-model="motivo" rows="3" :maxlength="MOTIVO_AJUSTE_MAXIMO"></textarea>
        </CampoFormulario>
      </div>
      <div class="modal-pie">
        <button type="button" class="boton boton-secundario" :disabled="enviando" @click="emit('cerrar')">Cancelar</button>
        <button type="submit" class="boton boton-peligro" :disabled="enviando">{{ enviando ? 'Exonerando...' : 'Exonerar mora' }}</button>
      </div>
    </form>
  </VentanaModal>
</template>
