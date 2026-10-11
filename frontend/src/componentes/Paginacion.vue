<script setup>
import { computed } from 'vue'
import IconoSistema from './IconoSistema.vue'

const props = defineProps({
  paginaActual: { type: Number, required: true },
  total: { type: Number, required: true },
  tamanoPagina: { type: Number, default: 10 },
  etiquetaSingular: { type: String, required: true },
  etiquetaPlural: { type: String, required: true }
})
const emit = defineEmits(['update:paginaActual'])

const totalPaginas = computed(() => Math.max(1, Math.ceil(props.total / props.tamanoPagina)))
const primerElemento = computed(() => (props.total === 0 ? 0 : (props.paginaActual - 1) * props.tamanoPagina + 1))
const ultimoElemento = computed(() => Math.min(props.total, props.paginaActual * props.tamanoPagina))
const etiqueta = computed(() => (props.total === 1 ? props.etiquetaSingular : props.etiquetaPlural))

function irAPagina(numeroPagina) {
  if (numeroPagina >= 1 && numeroPagina <= totalPaginas.value) emit('update:paginaActual', numeroPagina)
}
</script>

<template>
  <div class="pie-tabla">
    <span>Mostrando {{ primerElemento }}–{{ ultimoElemento }} de {{ total }} {{ etiqueta }}</span>
    <nav class="paginas" aria-label="Paginación">
      <button type="button" aria-label="Página anterior" :disabled="paginaActual === 1" @click="irAPagina(paginaActual - 1)">
        <IconoSistema nombre="chevron-izquierda" :tamano="16" />
      </button>
      <button
        v-for="numeroPagina in totalPaginas"
        :key="numeroPagina"
        type="button"
        :class="{ activa: numeroPagina === paginaActual }"
        :aria-current="numeroPagina === paginaActual ? 'page' : undefined"
        @click="irAPagina(numeroPagina)"
      >
        {{ numeroPagina }}
      </button>
      <button type="button" aria-label="Página siguiente" :disabled="paginaActual === totalPaginas" @click="irAPagina(paginaActual + 1)">
        <IconoSistema nombre="chevron-derecha" :tamano="16" />
      </button>
    </nav>
  </div>
</template>

<style scoped>
.paginas { display: flex; align-items: center; gap: 4px; }
.paginas button {
  display: inline-flex; align-items: center; justify-content: center; min-width: 30px; height: 30px; padding: 0 8px;
  border: 0; border-radius: 6px; background: transparent; color: var(--color-texto-suave); font-size: 13px; font-weight: 600; cursor: pointer;
}
.paginas button:hover:not(:disabled) { background: var(--color-fondo); }
.paginas button.activa { background: var(--color-primario-suave); color: var(--color-primario); }
.paginas button:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
