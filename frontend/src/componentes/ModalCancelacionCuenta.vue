<script setup>
import { reactive, ref, computed, watch } from 'vue'
import VentanaModal from './VentanaModal.vue'
import CampoFormulario from './CampoFormulario.vue'
import AvisoInformativo from './AvisoInformativo.vue'
import MensajeAlerta from './MensajeAlerta.vue'
import IconoSistema from './IconoSistema.vue'
import { cancelarCuenta } from '../servicios/cuentasServicio'
import { usarCatalogoTiposTarjeta } from '../composables/usarCatalogoTiposTarjeta'
import { formatearMoneda, formatearNumeroEnmascarado } from '../utilidades/formatos'
import { obtenerMensajeError, obtenerErroresDeCampos } from '../utilidades/errores'

const props = defineProps({
  visible: { type: Boolean, default: false },
  cuenta: { type: Object, required: true }
})
const emit = defineEmits(['cerrar', 'cancelada'])

const { motivosCancelacion } = usarCatalogoTiposTarjeta()

const formulario = reactive({ motivoCancelacion: '', comentario: '' })
const erroresCampos = ref({})
const mensajeError = ref('')
const enviando = ref(false)

const requisitos = computed(() => [
  { clave: 'al-dia', cumplido: props.cuenta.estadoPago === 'AL_DIA', texto: 'Al día en pagos' },
  { clave: 'saldo-cero', cumplido: Number(props.cuenta.saldoActual) === 0, texto: `Saldo ${formatearMoneda(0)} (saldo actual: ${formatearMoneda(props.cuenta.saldoActual)})` }
])
const cumpleRequisitos = computed(() => requisitos.value.every((requisito) => requisito.cumplido))

watch(
  () => props.visible,
  (esVisible) => {
    if (!esVisible) return
    Object.assign(formulario, { motivoCancelacion: '', comentario: '' })
    erroresCampos.value = {}
    mensajeError.value = ''
  }
)

async function confirmarCancelacion() {
  mensajeError.value = ''
  const errores = {}
  if (!formulario.motivoCancelacion) errores.motivoCancelacion = 'Seleccione el motivo de la cancelación'
  if (formulario.comentario.length > 250) errores.comentario = 'El comentario no puede exceder 250 caracteres'
  erroresCampos.value = errores
  if (Object.keys(errores).length > 0 || !cumpleRequisitos.value) return
  enviando.value = true
  try {
    await cancelarCuenta(props.cuenta.id, {
      motivoCancelacion: formulario.motivoCancelacion,
      comentario: formulario.comentario.trim() || null
    })
    emit('cancelada')
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
    titulo="Cancelar cuenta"
    :descripcion="`Cuenta ${cuenta.descripcionTipoTarjeta} ${formatearNumeroEnmascarado(cuenta.numeroCuentaEnmascarado)} · ${cuenta.nombreCliente}`"
    @cerrar="emit('cerrar')"
  >
    <form novalidate @submit.prevent="confirmarCancelacion">
      <div class="modal-cuerpo">
        <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

        <div>
          <p class="titulo-requisitos">Requisitos para cancelar</p>
          <ul class="lista-cumplimiento">
            <li v-for="requisito in requisitos" :key="requisito.clave" :class="requisito.cumplido ? 'cumplido' : 'incumplido'">
              <IconoSistema :nombre="requisito.cumplido ? 'check-circulo' : 'x-circulo'" :tamano="20" />
              <span>{{ requisito.texto }}<span class="texto-oculto-visual">: {{ requisito.cumplido ? 'cumple' : 'no cumple' }}</span></span>
            </li>
          </ul>
        </div>

        <AvisoInformativo v-if="!cumpleRequisitos" variante="error" titulo="No se puede cancelar esta cuenta todavía">
          La cuenta debe estar al día en sus pagos y con saldo Q 0.00. Registre los pagos pendientes y vuelva a intentarlo.
        </AvisoInformativo>

        <fieldset class="grupo-opciones" :class="{ 'con-error': erroresCampos.motivoCancelacion }">
          <legend>Motivo de la cancelación *</legend>
          <div class="opciones-tarjeta">
            <label v-for="motivo in motivosCancelacion" :key="motivo.codigo" class="opcion-tarjeta" :class="{ seleccionada: formulario.motivoCancelacion === motivo.codigo }">
              <input v-model="formulario.motivoCancelacion" type="radio" name="motivoCancelacion" :value="motivo.codigo" />
              <IconoSistema nombre="prohibido" />
              {{ motivo.descripcion }}
              <IconoSistema v-if="formulario.motivoCancelacion === motivo.codigo" class="icono-seleccion" nombre="check-circulo" :tamano="18" />
            </label>
          </div>
          <p v-if="erroresCampos.motivoCancelacion" class="mensaje-error"><IconoSistema nombre="alerta" :tamano="14" />{{ erroresCampos.motivoCancelacion }}</p>
        </fieldset>

        <CampoFormulario id="comentario-cancelacion" etiqueta="Comentario" :error="erroresCampos.comentario" ayuda="Opcional. Máximo 250 caracteres.">
          <textarea id="comentario-cancelacion" v-model="formulario.comentario" rows="3" maxlength="250"></textarea>
        </CampoFormulario>

        <AvisoInformativo variante="advertencia" titulo="Qué ocurrirá al cancelar">
          La cuenta y su tarjeta quedarán canceladas. El historial se conserva y el cliente seguirá activo.
        </AvisoInformativo>
      </div>
      <div class="modal-pie">
        <button type="button" class="boton boton-secundario" :disabled="enviando" @click="emit('cerrar')">Volver</button>
        <button type="submit" class="boton boton-peligro" :disabled="enviando || !cumpleRequisitos" :title="cumpleRequisitos ? '' : 'La cuenta no cumple los requisitos para cancelarse'">
          {{ enviando ? 'Cancelando...' : 'Cancelar cuenta' }}
        </button>
      </div>
    </form>
  </VentanaModal>
</template>

<style scoped>
.titulo-requisitos { margin-bottom: 10px; font-size: 13px; font-weight: 600; }
.lista-cumplimiento { display: flex; flex-direction: column; gap: 10px; margin: 0; padding: 0; list-style: none; }
.lista-cumplimiento li { display: flex; align-items: center; gap: 10px; padding: 12px 14px; border-radius: var(--radio); font-size: 14px; font-weight: 500; }
.cumplido { background: var(--color-exito-suave); color: var(--color-exito); }
.incumplido { background: var(--color-peligro-suave); color: var(--color-peligro); }
.texto-oculto-visual { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
</style>
