<script setup>
import { watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  mensaje: { type: String, default: '' },
  tipo: { type: String, default: 'error' },
  autoCerrarMs: { type: Number, default: 0 }
})
const emit = defineEmits(['cerrar'])

let temporizador = null

watch(
  () => props.mensaje,
  (mensajeNuevo) => {
    clearTimeout(temporizador)
    if (mensajeNuevo && props.autoCerrarMs > 0) {
      temporizador = setTimeout(() => emit('cerrar'), props.autoCerrarMs)
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => clearTimeout(temporizador))
</script>

<template>
  <Transition name="alerta">
    <div v-if="mensaje" :key="mensaje" class="alerta" :class="tipo === 'exito' ? 'alerta-exito' : 'alerta-error'" role="alert">
      <span>{{ mensaje }}</span>
      <button type="button" aria-label="Cerrar mensaje" @click="emit('cerrar')">&times;</button>
      <span v-if="autoCerrarMs > 0" class="alerta-progreso" :style="{ '--duracion-progreso': `${autoCerrarMs}ms` }" aria-hidden="true"></span>
    </div>
  </Transition>
</template>
