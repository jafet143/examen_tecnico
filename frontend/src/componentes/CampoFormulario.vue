<script setup>
import IconoSistema from './IconoSistema.vue'

defineProps({
  id: { type: String, required: true },
  etiqueta: { type: String, required: true },
  error: { type: String, default: '' },
  ayuda: { type: String, default: '' }
})
</script>

<template>
  <div class="campo" :class="{ 'con-error': error }">
    <label :for="id">{{ etiqueta }}</label>
    <slot />
    <p v-if="error" class="mensaje-error"><IconoSistema nombre="alerta" :tamano="14" />{{ error }}</p>
    <p v-else-if="ayuda" class="ayuda">{{ ayuda }}</p>
  </div>
</template>
