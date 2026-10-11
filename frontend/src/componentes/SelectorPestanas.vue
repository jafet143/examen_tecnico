<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  opciones: { type: Array, required: true },
  etiquetaGrupo: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const contenedor = ref(null)
const estiloIndicador = ref({ opacity: 0 })
const animarIndicador = ref(false)
let observadorTamano = null

function ubicarIndicador() {
  const pestanaActiva = contenedor.value?.querySelector('[aria-selected="true"]')
  if (!pestanaActiva) {
    estiloIndicador.value = { opacity: 0 }
    return
  }
  estiloIndicador.value = {
    width: `${pestanaActiva.offsetWidth}px`,
    height: `${pestanaActiva.offsetHeight}px`,
    transform: `translate(${pestanaActiva.offsetLeft}px, ${pestanaActiva.offsetTop}px)`,
    opacity: 1
  }
}

function seleccionar(valor) {
  emit('update:modelValue', valor)
}

function moverConTeclado(evento, indiceActual) {
  const desplazamientos = { ArrowRight: 1, ArrowLeft: -1 }
  if (!(evento.key in desplazamientos)) return
  evento.preventDefault()
  const total = props.opciones.length
  const indiceNuevo = (indiceActual + desplazamientos[evento.key] + total) % total
  seleccionar(props.opciones[indiceNuevo].valor)
  nextTick(() => contenedor.value?.querySelectorAll('[role="tab"]')[indiceNuevo]?.focus())
}

watch(
  () => [props.modelValue, props.opciones],
  () => nextTick(ubicarIndicador),
  { deep: true }
)

onMounted(() => {
  ubicarIndicador()
  requestAnimationFrame(() => {
    animarIndicador.value = true
  })
  observadorTamano = new ResizeObserver(ubicarIndicador)
  observadorTamano.observe(contenedor.value)
})

onBeforeUnmount(() => observadorTamano?.disconnect())
</script>

<template>
  <div ref="contenedor" class="pestanas-filtro" role="tablist" :aria-label="etiquetaGrupo">
    <span class="pestanas-indicador" :class="{ 'pestanas-indicador--animado': animarIndicador }" :style="estiloIndicador" aria-hidden="true"></span>
    <button
      v-for="(opcion, indice) in opciones"
      :key="opcion.valor"
      type="button"
      role="tab"
      :class="{ activa: modelValue === opcion.valor }"
      :aria-selected="modelValue === opcion.valor"
      :tabindex="modelValue === opcion.valor ? 0 : -1"
      @click="seleccionar(opcion.valor)"
      @keydown="moverConTeclado($event, indice)"
    >
      {{ opcion.etiqueta }}<span v-if="opcion.cantidad !== undefined" class="conteo">{{ opcion.cantidad }}</span>
    </button>
  </div>
</template>

<style scoped>
.pestanas-filtro { position: relative; }
.pestanas-filtro button { position: relative; z-index: 1; transition: color var(--duracion-media, 250ms) ease; }
.pestanas-filtro button.activa { background: transparent; }
.pestanas-filtro button:not(.activa):hover { color: var(--color-texto); }
.pestanas-filtro button:focus-visible { outline: none; box-shadow: 0 0 0 3px rgba(23, 75, 140, 0.22); }
.pestanas-indicador {
  position: absolute;
  top: 0;
  left: 0;
  border-radius: 6px;
  background: var(--color-primario-suave);
  box-shadow: inset 0 0 0 1px rgba(23, 75, 140, 0.12);
  pointer-events: none;
}
.pestanas-indicador--animado {
  transition: transform 0.35s cubic-bezier(0.34, 1.3, 0.64, 1), width 0.35s cubic-bezier(0.34, 1.3, 0.64, 1), opacity 0.2s ease;
}
@media (prefers-reduced-motion: reduce) {
  .pestanas-indicador--animado { transition: none; }
}
</style>
