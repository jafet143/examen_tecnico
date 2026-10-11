<script setup>
defineProps({
  codigo: { type: String, default: '' },
  descripcion: { type: String, required: true }
})
</script>

<template>
  <span class="indicador-tipo" :class="codigo && `tipo-tarjeta--${codigo.toLowerCase()}`">
    <span class="indicador-tipo-punto"></span>{{ descripcion }}
  </span>
</template>

<style scoped>
.indicador-tipo { display: inline-flex; align-items: center; gap: 8px; }
.indicador-tipo-punto { width: 10px; height: 10px; border-radius: 50%; background: var(--tarjeta-punto, var(--color-borde)); box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.12); }
</style>
