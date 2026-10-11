<script setup>
import { watch, onBeforeUnmount } from 'vue'
import IconoSistema from './IconoSistema.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  titulo: { type: String, default: '' },
  descripcion: { type: String, default: '' }
})
const emit = defineEmits(['cerrar'])

function manejarTecla(evento) {
  if (evento.key === 'Escape') emit('cerrar')
}

watch(
  () => props.visible,
  (esVisible) => {
    if (esVisible) document.addEventListener('keydown', manejarTecla)
    else document.removeEventListener('keydown', manejarTecla)
  },
  { immediate: true }
)

onBeforeUnmount(() => document.removeEventListener('keydown', manejarTecla))
</script>

<template>
  <Transition name="modal">
  <div v-if="visible" class="modal-fondo" @mousedown.self="emit('cerrar')">
    <div class="modal" role="dialog" aria-modal="true" :aria-label="titulo">
      <div class="modal-encabezado">
        <div>
          <h2>{{ titulo }}</h2>
          <p v-if="descripcion">{{ descripcion }}</p>
        </div>
        <button type="button" aria-label="Cerrar" @click="emit('cerrar')"><IconoSistema nombre="x" /></button>
      </div>
      <slot />
    </div>
  </div>
  </Transition>
</template>
