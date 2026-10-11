<script setup>
import { computed } from 'vue'
import IconoSistema from './IconoSistema.vue'

const props = defineProps({
  variante: { type: String, default: 'info' },
  titulo: { type: String, required: true }
})

const nombreIcono = computed(() => (props.variante === 'exito' ? 'check-circulo' : props.variante === 'info' || props.variante === 'neutro' ? 'info' : 'alerta'))
</script>

<template>
  <div class="aviso" :class="`aviso-${variante}`">
    <IconoSistema :nombre="nombreIcono" />
    <div>
      <p class="aviso-titulo">{{ titulo }}</p>
      <p v-if="$slots.default" class="aviso-texto"><slot /></p>
    </div>
  </div>
</template>
