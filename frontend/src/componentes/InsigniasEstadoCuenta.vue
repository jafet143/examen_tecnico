<script setup>
import { computed } from 'vue'
import EtiquetaEstado from './EtiquetaEstado.vue'
import { ESTADO_TARJETA_BLOQUEADA, ESTADO_TARJETA_VENCIDA, ESTADO_CUENTA_CANCELADA } from '../utilidades/constantes'
import { esTarjetaVigenteVencida } from '../utilidades/tarjetas'

const props = defineProps({
  cuenta: { type: Object, required: true }
})

const cancelada = computed(() => props.cuenta.estadoCuenta === ESTADO_CUENTA_CANCELADA)
const vigenteBloqueada = computed(() => props.cuenta.tarjetaVigente?.estadoTarjeta === ESTADO_TARJETA_BLOQUEADA)
const vigenteVencida = computed(() => esTarjetaVigenteVencida(props.cuenta.tarjetaVigente))
</script>

<template>
  <span class="insignias-estado">
    <EtiquetaEstado v-if="cancelada" :estado="ESTADO_CUENTA_CANCELADA" etiqueta="Cancelada" />
    <template v-else>
      <EtiquetaEstado v-if="vigenteBloqueada" :estado="ESTADO_TARJETA_BLOQUEADA" :etiqueta="cuenta.tarjetaVigente.descripcionEstadoTarjeta" />
      <EtiquetaEstado v-if="vigenteVencida" :estado="ESTADO_TARJETA_VENCIDA" />
      <EtiquetaEstado v-if="cuenta.estadoPago" :estado="cuenta.estadoPago" :etiqueta="cuenta.descripcionEstadoPago" />
    </template>
  </span>
</template>

<style scoped>
.insignias-estado { display: inline-flex; flex-wrap: wrap; align-items: center; gap: 6px; }
</style>
