<script setup>
import { reactive, ref, computed, watch } from 'vue'
import VentanaModal from './VentanaModal.vue'
import CampoFormulario from './CampoFormulario.vue'
import AvisoInformativo from './AvisoInformativo.vue'
import MensajeAlerta from './MensajeAlerta.vue'
import IconoSistema from './IconoSistema.vue'
import { bloquearTarjeta } from '../servicios/tarjetasServicio'
import { usarCatalogoTiposTarjeta } from '../composables/usarCatalogoTiposTarjeta'
import { obtenerMensajeError, obtenerErroresDeCampos } from '../utilidades/errores'
import { obtenerUltimosCuatroDigitos } from '../utilidades/formatos'

const props = defineProps({
  visible: { type: Boolean, default: false },
  tarjeta: { type: Object, required: true }
})
const emit = defineEmits(['cerrar', 'bloqueada'])

const { motivosBloqueo } = usarCatalogoTiposTarjeta()

const formulario = reactive({ motivoBloqueo: '', comentario: '' })
const erroresCampos = ref({})
const mensajeError = ref('')
const enviando = ref(false)

const motivoSeleccionado = computed(() => motivosBloqueo.value.find((motivo) => motivo.codigo === formulario.motivoBloqueo) ?? null)
const motivoSinDesbloqueo = computed(() => motivoSeleccionado.value?.permiteDesbloqueo === false)

watch(
  () => props.visible,
  (esVisible) => {
    if (!esVisible) return
    Object.assign(formulario, { motivoBloqueo: '', comentario: '' })
    erroresCampos.value = {}
    mensajeError.value = ''
  }
)

async function confirmarBloqueo() {
  mensajeError.value = ''
  const errores = {}
  if (!formulario.motivoBloqueo) errores.motivoBloqueo = 'Seleccione el motivo del bloqueo'
  if (formulario.comentario.length > 250) errores.comentario = 'El comentario no puede exceder 250 caracteres'
  erroresCampos.value = errores
  if (Object.keys(errores).length > 0) return
  enviando.value = true
  try {
    await bloquearTarjeta(props.tarjeta.id, {
      motivoBloqueo: formulario.motivoBloqueo,
      comentario: formulario.comentario.trim() || null
    })
    emit('bloqueada')
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
    titulo="Bloquear tarjeta"
    :descripcion="`Tarjeta terminada en ${obtenerUltimosCuatroDigitos(tarjeta.numeroTarjetaEnmascarado)} · ${tarjeta.nombreCliente}`"
    @cerrar="emit('cerrar')"
  >
    <form novalidate @submit.prevent="confirmarBloqueo">
      <div class="modal-cuerpo">
        <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

        <fieldset class="grupo-opciones" :class="{ 'con-error': erroresCampos.motivoBloqueo }">
          <legend>Motivo del bloqueo *</legend>
          <div class="opciones-tarjeta">
            <label v-for="motivo in motivosBloqueo" :key="motivo.codigo" class="opcion-tarjeta" :class="{ seleccionada: formulario.motivoBloqueo === motivo.codigo }">
              <input v-model="formulario.motivoBloqueo" type="radio" name="motivoBloqueo" :value="motivo.codigo" />
              <IconoSistema nombre="candado" />
              {{ motivo.descripcion }}
              <IconoSistema v-if="formulario.motivoBloqueo === motivo.codigo" class="icono-seleccion" nombre="check-circulo" :tamano="18" />
            </label>
          </div>
          <p v-if="erroresCampos.motivoBloqueo" class="mensaje-error"><IconoSistema nombre="alerta" :tamano="14" />{{ erroresCampos.motivoBloqueo }}</p>
        </fieldset>

        <CampoFormulario id="comentario-bloqueo" etiqueta="Comentario" :error="erroresCampos.comentario" ayuda="Opcional. Máximo 250 caracteres.">
          <textarea id="comentario-bloqueo" v-model="formulario.comentario" rows="3" maxlength="250"></textarea>
        </CampoFormulario>

        <AvisoInformativo v-if="motivoSinDesbloqueo" variante="advertencia" titulo="Bloqueo definitivo">
          Una tarjeta bloqueada por {{ motivoSeleccionado.descripcion.toLowerCase() }} no podrá desbloquearse. La única opción será generar una reposición.
        </AvisoInformativo>
        <AvisoInformativo titulo="Qué implica el bloqueo">
          Una tarjeta bloqueada no acepta consumos, pero sí pagos.
        </AvisoInformativo>
      </div>
      <div class="modal-pie">
        <button type="button" class="boton boton-secundario" :disabled="enviando" @click="emit('cerrar')">Cancelar</button>
        <button type="submit" class="boton boton-peligro" :disabled="enviando">{{ enviando ? 'Bloqueando...' : 'Bloquear tarjeta' }}</button>
      </div>
    </form>
  </VentanaModal>
</template>
