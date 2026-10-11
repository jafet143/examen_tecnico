<script setup>
import { ref, computed } from 'vue'
import IconoSistema from './IconoSistema.vue'
import { formatearNumeroEnmascarado, formatearNumeroCompleto } from '../utilidades/formatos'

const props = defineProps({
  enmascarado: { type: String, default: '' },
  completo: { type: String, default: null }
})

const visible = ref(false)
const puedeMostrarse = computed(() => Boolean(props.completo))
const numeroEnmascarado = computed(() => formatearNumeroEnmascarado(props.enmascarado))
const numeroCompleto = computed(() => (puedeMostrarse.value ? formatearNumeroCompleto(props.completo) : ''))
const mostrandoCompleto = computed(() => visible.value && puedeMostrarse.value)
const etiquetaBoton = computed(() => (visible.value ? 'Ocultar número' : 'Mostrar número'))
</script>

<template>
  <span class="numero-protegido">
    <span class="numero-protegido-espacio">
      <span class="numero-protegido-texto" :class="{ oculto: mostrandoCompleto }" :aria-hidden="mostrandoCompleto">{{ numeroEnmascarado }}</span>
      <span v-if="puedeMostrarse" class="numero-protegido-texto" :class="{ oculto: !mostrandoCompleto }" :aria-hidden="!mostrandoCompleto">{{ numeroCompleto }}</span>
    </span>
    <button
      v-if="puedeMostrarse"
      type="button"
      class="boton-icono boton-ojo"
      :aria-label="etiquetaBoton"
      :aria-pressed="visible"
      :title="etiquetaBoton"
      @click.prevent.stop="visible = !visible"
    >
      <IconoSistema :nombre="visible ? 'ojo-tachado' : 'ojo'" :tamano="16" />
    </button>
  </span>
</template>

<style scoped>
.numero-protegido { display: inline-flex; align-items: center; gap: 2px; white-space: nowrap; }
.numero-protegido-espacio { display: inline-grid; }
.numero-protegido-texto {
  grid-area: 1 / 1;
  font-variant-numeric: tabular-nums;
  transition: opacity var(--duracion-media, 250ms) ease, filter var(--duracion-media, 250ms) ease, visibility var(--duracion-media, 250ms);
}
.numero-protegido-texto.oculto { visibility: hidden; opacity: 0; filter: blur(3px); }
.boton-ojo { width: 24px; height: 24px; color: var(--color-texto-suave); }
</style>
