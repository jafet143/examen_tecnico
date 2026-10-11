<script setup>
import { computed } from 'vue'
import { ESTADOS_SOLICITUD, ESTADOS_PAGO, ESTADOS_TARJETA, ESTADO_MOVIMIENTO_REVERSADO, ESTADO_TARJETA_VENCIDA } from '../utilidades/constantes'

const ESTADOS_CONOCIDOS = [
  ...ESTADOS_SOLICITUD,
  ...ESTADOS_PAGO,
  ...ESTADOS_TARJETA,
  { valor: ESTADO_MOVIMIENTO_REVERSADO, etiqueta: 'Reversado' },
  { valor: ESTADO_TARJETA_VENCIDA, etiqueta: 'Vencida' }
]

const props = defineProps({
  estado: { type: String, required: true },
  etiqueta: { type: String, default: '' }
})

const textoVisible = computed(
  () => props.etiqueta || ESTADOS_CONOCIDOS.find((estado) => estado.valor === props.estado)?.etiqueta || props.estado
)
</script>

<template>
  <span class="etiqueta" :class="`etiqueta-${estado.toLowerCase()}`">{{ textoVisible }}</span>
</template>
