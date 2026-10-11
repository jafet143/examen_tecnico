<script setup>
import CampoMoneda from './CampoMoneda.vue'
import { reactive, ref, computed, watch } from 'vue'
import VentanaModal from './VentanaModal.vue'
import CampoFormulario from './CampoFormulario.vue'
import AvisoInformativo from './AvisoInformativo.vue'
import MensajeAlerta from './MensajeAlerta.vue'
import { registrarCargo } from '../servicios/cuentasServicio'
import { usarCatalogoTiposTarjeta } from '../composables/usarCatalogoTiposTarjeta'
import { formatearMoneda, formatearNumeroEnmascarado, obtenerFechaHoraLocalActual } from '../utilidades/formatos'
import { obtenerMensajeError, obtenerErroresDeCampos } from '../utilidades/errores'

const props = defineProps({
  visible: { type: Boolean, default: false },
  cuenta: { type: Object, required: true }
})
const emit = defineEmits(['cerrar', 'registrado'])

const { tiposCargoManual } = usarCatalogoTiposTarjeta()

const formularioVacio = () => ({ tipoCargo: '', monto: '', descripcion: '', fecha: '' })
const formulario = reactive(formularioVacio())
const erroresCampos = ref({})
const mensajeError = ref('')
const enviando = ref(false)
const fechaMaxima = ref(obtenerFechaHoraLocalActual())

const montoIngresado = computed(() => Number(formulario.monto))
const saldoResultante = computed(() => Number(props.cuenta.saldoActual) + montoIngresado.value)
const superaLimite = computed(() => montoIngresado.value > 0 && saldoResultante.value > Number(props.cuenta.limiteCredito))

watch(
  () => props.visible,
  (esVisible) => {
    if (!esVisible) return
    Object.assign(formulario, formularioVacio())
    erroresCampos.value = {}
    mensajeError.value = ''
    fechaMaxima.value = obtenerFechaHoraLocalActual()
  }
)

watch(
  () => [formulario.tipoCargo, formulario.monto, formulario.descripcion, formulario.fecha],
  () => {
    erroresCampos.value = {}
  }
)

function validar() {
  const errores = {}
  if (!formulario.tipoCargo) errores.tipoCargo = 'Seleccione el tipo de cargo'
  if (!(montoIngresado.value > 0)) errores.monto = 'El monto debe ser mayor a cero'
  if (!formulario.descripcion.trim()) errores.descripcion = 'La descripción es obligatoria'
  else if (formulario.descripcion.length > 250) errores.descripcion = 'La descripción no puede exceder 250 caracteres'
  if (formulario.fecha && formulario.fecha > obtenerFechaHoraLocalActual()) errores.fecha = 'La fecha no puede ser futura'
  erroresCampos.value = errores
  return Object.keys(errores).length === 0
}

async function guardarCargo() {
  mensajeError.value = ''
  if (!validar()) return
  enviando.value = true
  try {
    const datos = { tipoCargo: formulario.tipoCargo, monto: montoIngresado.value, descripcion: formulario.descripcion.trim() }
    if (formulario.fecha) datos.fecha = formulario.fecha.length === 16 ? `${formulario.fecha}:00` : formulario.fecha
    await registrarCargo(props.cuenta.id, datos)
    emit('registrado', 'Cargo registrado correctamente')
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
    titulo="Registrar cargo"
    :descripcion="`Cuenta ${cuenta.descripcionTipoTarjeta} ${formatearNumeroEnmascarado(cuenta.numeroCuentaEnmascarado)} · ${cuenta.nombreCliente}`"
    @cerrar="emit('cerrar')"
  >
    <form novalidate @submit.prevent="guardarCargo">
      <div class="modal-cuerpo">
        <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

        <div class="rejilla-campos rejilla-sin-margen">
          <CampoFormulario id="tipo-cargo" etiqueta="Tipo de cargo *" :error="erroresCampos.tipoCargo">
            <select id="tipo-cargo" v-model="formulario.tipoCargo">
              <option value="" disabled>Selecciona el tipo</option>
              <option v-for="tipo in tiposCargoManual" :key="tipo.codigo" :value="tipo.codigo">{{ tipo.descripcion }}</option>
            </select>
          </CampoFormulario>
          <CampoFormulario id="monto-cargo" etiqueta="Monto *" :error="erroresCampos.monto">
            <CampoMoneda id="monto-cargo" v-model="formulario.monto" />
          </CampoFormulario>
        </div>

        <CampoFormulario id="descripcion-cargo" etiqueta="Descripción *" :error="erroresCampos.descripcion" ayuda="Máximo 250 caracteres.">
          <textarea id="descripcion-cargo" v-model="formulario.descripcion" rows="3" maxlength="250"></textarea>
        </CampoFormulario>

        <CampoFormulario id="fecha-cargo" etiqueta="Fecha y hora" :error="erroresCampos.fecha" ayuda="Opcional. Si se deja vacía se usa la actual.">
          <input id="fecha-cargo" v-model="formulario.fecha" type="datetime-local" :max="fechaMaxima" />
        </CampoFormulario>

        <AvisoInformativo v-if="superaLimite" variante="advertencia" titulo="El cargo dejará la cuenta en sobregiro">
          El saldo pasará a {{ formatearMoneda(saldoResultante) }} y superará el límite de {{ formatearMoneda(cuenta.limiteCredito) }}. Los cargos pueden exceder el límite; el disponible quedará en Q 0.00.
        </AvisoInformativo>
        <AvisoInformativo v-else titulo="Los cargos suman al saldo">
          Intereses y recargos por mora aumentan la deuda de la cuenta, igual que un consumo. Pueden corregirse después con una devolución o una exoneración.
        </AvisoInformativo>
      </div>
      <div class="modal-pie">
        <button type="button" class="boton boton-secundario" :disabled="enviando" @click="emit('cerrar')">Cancelar</button>
        <button type="submit" class="boton" :disabled="enviando">{{ enviando ? 'Guardando...' : 'Registrar cargo' }}</button>
      </div>
    </form>
  </VentanaModal>
</template>

<style scoped>
.rejilla-sin-margen { margin-bottom: 0; }
@media (max-width: 700px) { .rejilla-sin-margen { grid-template-columns: 1fr; } }
</style>
