<script setup>
import { reactive, ref, computed, watch } from 'vue'
import VentanaModal from './VentanaModal.vue'
import CampoFormulario from './CampoFormulario.vue'
import AvisoInformativo from './AvisoInformativo.vue'
import MensajeAlerta from './MensajeAlerta.vue'
import IconoSistema from './IconoSistema.vue'
import { reponerTarjeta } from '../servicios/tarjetasServicio'
import { usarCatalogoTiposTarjeta } from '../composables/usarCatalogoTiposTarjeta'
import { obtenerMensajeError, obtenerErroresDeCampos } from '../utilidades/errores'
import { formatearMoneda, formatearFecha, formatearNumeroEnmascarado, obtenerUltimosCuatroDigitos } from '../utilidades/formatos'
import { MOTIVO_REPOSICION_ROBO, MOTIVO_REPOSICION_EXTRAVIO, MOTIVO_REPOSICION_DETERIORO, MOTIVO_REPOSICION_RENOVACION, ESTADO_TARJETA_BLOQUEADA } from '../utilidades/constantes'

const props = defineProps({
  visible: { type: Boolean, default: false },
  tarjeta: { type: Object, required: true }
})
const emit = defineEmits(['cerrar', 'repuesta'])

const { motivosReposicion, motivosBloqueo, comisionReposicionExtravio } = usarCatalogoTiposTarjeta()

const motivosDisponibles = computed(() => motivosReposicion.value.filter((motivo) => motivo.codigo !== MOTIVO_REPOSICION_RENOVACION))

const motivoTomadoDelBloqueo = computed(() => {
  if (props.tarjeta.estadoTarjeta !== ESTADO_TARJETA_BLOQUEADA) return null
  const bloqueo = motivosBloqueo.value.find((motivo) => motivo.codigo === props.tarjeta.motivoBloqueo)
  if (!bloqueo || bloqueo.permiteDesbloqueo) return null
  return motivosDisponibles.value.find((motivo) => motivo.codigo === props.tarjeta.motivoBloqueo) ?? null
})
const detalleBloqueo = computed(() =>
  [
    props.tarjeta.fechaBloqueo && `el ${formatearFecha(props.tarjeta.fechaBloqueo)}`,
    props.tarjeta.usuarioBloqueo && `por ${props.tarjeta.usuarioBloqueo}`
  ]
    .filter(Boolean)
    .join(' ')
)

const formulario = reactive({ motivoReposicion: '', comentario: '', requisitoPresentado: false })
const erroresCampos = ref({})
const mensajeError = ref('')
const enviando = ref(false)

const textoRequisito = computed(() => {
  if (formulario.motivoReposicion === MOTIVO_REPOSICION_ROBO) return 'El cliente presentó la denuncia del robo'
  if (formulario.motivoReposicion === MOTIVO_REPOSICION_DETERIORO) return 'El cliente presentó la tarjeta deteriorada'
  return ''
})
const cobraComision = computed(() => formulario.motivoReposicion === MOTIVO_REPOSICION_EXTRAVIO)
const requisitoPendiente = computed(() => textoRequisito.value !== '' && !formulario.requisitoPresentado)

watch(
  () => formulario.motivoReposicion,
  () => {
    formulario.requisitoPresentado = false
  }
)

watch(
  () => props.visible,
  (esVisible) => {
    if (!esVisible) return
    Object.assign(formulario, { motivoReposicion: motivoTomadoDelBloqueo.value?.codigo ?? '', comentario: '', requisitoPresentado: false })
    erroresCampos.value = {}
    mensajeError.value = ''
  }
)

async function confirmarReposicion() {
  mensajeError.value = ''
  const errores = {}
  if (!formulario.motivoReposicion) errores.motivoReposicion = 'Seleccione el motivo de la reposición'
  if (formulario.comentario.length > 250) errores.comentario = 'El comentario no puede exceder 250 caracteres'
  erroresCampos.value = errores
  if (Object.keys(errores).length > 0 || requisitoPendiente.value) return
  enviando.value = true
  try {
    const tarjetaNueva = await reponerTarjeta(props.tarjeta.id, {
      motivoReposicion: formulario.motivoReposicion,
      comentario: formulario.comentario.trim() || null,
      denunciaPresentada: formulario.motivoReposicion === MOTIVO_REPOSICION_ROBO && formulario.requisitoPresentado,
      tarjetaDeterioradaPresentada: formulario.motivoReposicion === MOTIVO_REPOSICION_DETERIORO && formulario.requisitoPresentado
    })
    emit('repuesta', tarjetaNueva, cobraComision.value ? comisionReposicionExtravio.value : null)
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
    titulo="Reponer tarjeta"
    :descripcion="`Tarjeta terminada en ${obtenerUltimosCuatroDigitos(tarjeta.numeroTarjetaEnmascarado)} · ${tarjeta.nombreCliente}`"
    @cerrar="emit('cerrar')"
  >
    <form novalidate @submit.prevent="confirmarReposicion">
      <div class="modal-cuerpo">
        <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

        <div v-if="motivoTomadoDelBloqueo" class="motivo-desde-bloqueo">
          <span class="motivo-desde-bloqueo-etiqueta">Motivo de la reposición</span>
          <div class="motivo-desde-bloqueo-valor">
            <IconoSistema nombre="candado" :tamano="18" />
            <div>
              <strong>{{ motivoTomadoDelBloqueo.descripcion }}</strong>
              <p>Tomado del bloqueo registrado{{ detalleBloqueo ? ` ${detalleBloqueo}` : '' }}.</p>
            </div>
          </div>
        </div>

        <fieldset v-else class="grupo-opciones" :class="{ 'con-error': erroresCampos.motivoReposicion }">
          <legend>Motivo de la reposición *</legend>
          <div class="opciones-tarjeta">
            <label v-for="motivo in motivosDisponibles" :key="motivo.codigo" class="opcion-tarjeta" :class="{ seleccionada: formulario.motivoReposicion === motivo.codigo }">
              <input v-model="formulario.motivoReposicion" type="radio" name="motivoReposicion" :value="motivo.codigo" />
              <IconoSistema nombre="tarjeta" />
              {{ motivo.descripcion }}
              <IconoSistema v-if="formulario.motivoReposicion === motivo.codigo" class="icono-seleccion" nombre="check-circulo" :tamano="18" />
            </label>
          </div>
          <p v-if="erroresCampos.motivoReposicion" class="mensaje-error"><IconoSistema nombre="alerta" :tamano="14" />{{ erroresCampos.motivoReposicion }}</p>
        </fieldset>

        <label v-if="textoRequisito" class="casilla">
          <input v-model="formulario.requisitoPresentado" type="checkbox" />
          {{ textoRequisito }} *
        </label>
        <AvisoInformativo v-if="cobraComision" variante="advertencia" :titulo="`Se cobrará una comisión de ${formatearMoneda(comisionReposicionExtravio)} a la cuenta`">
          El cargo se registra automáticamente en la cuenta al reponer la tarjeta por extravío.
        </AvisoInformativo>

        <CampoFormulario id="comentario-reposicion" etiqueta="Comentario" :error="erroresCampos.comentario" ayuda="Opcional. Máximo 250 caracteres.">
          <textarea id="comentario-reposicion" v-model="formulario.comentario" rows="3" maxlength="250"></textarea>
        </CampoFormulario>

        <AvisoInformativo titulo="Qué ocurrirá al reponer">
          Se emitirá una nueva tarjeta anclada a la misma cuenta {{ formatearNumeroEnmascarado(tarjeta.numeroCuentaEnmascarado) }}. El saldo, el límite y el historial se conservan.
          La tarjeta actual quedará reemplazada (y bloqueada si el motivo es robo, extravío o sospecha de fraude).
        </AvisoInformativo>
      </div>
      <div class="modal-pie">
        <button type="button" class="boton boton-secundario" :disabled="enviando" @click="emit('cerrar')">Cancelar</button>
        <button type="submit" class="boton" :disabled="enviando || requisitoPendiente" :title="requisitoPendiente ? `Marque: ${textoRequisito}` : ''">{{ enviando ? 'Reponiendo...' : 'Reponer tarjeta' }}</button>
      </div>
    </form>
  </VentanaModal>
</template>

<style scoped>
.motivo-desde-bloqueo { display: flex; flex-direction: column; gap: 8px; }
.motivo-desde-bloqueo-etiqueta { font-size: 13px; font-weight: 600; color: var(--color-texto); }
.motivo-desde-bloqueo-valor { display: flex; align-items: flex-start; gap: 10px; padding: 12px 14px; border: 1px solid var(--color-primario); border-radius: var(--radio); background: var(--color-primario-suave); color: var(--color-primario); }
.motivo-desde-bloqueo-valor strong { font-size: 14px; }
.motivo-desde-bloqueo-valor p { margin: 2px 0 0; font-size: 13px; color: var(--color-texto-suave); }
</style>
