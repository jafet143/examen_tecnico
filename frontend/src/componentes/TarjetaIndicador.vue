<script setup>
import IconoSistema from './IconoSistema.vue'
import { usarConteoAnimado } from '../composables/usarConteoAnimado'

const props = defineProps({
  titulo: { type: String, required: true },
  valor: { type: String, required: true },
  descripcion: { type: String, default: '' },
  icono: { type: String, default: '' },
  destacado: { type: Boolean, default: false }
})

const valorMostrado = usarConteoAnimado(() => props.valor)
</script>

<template>
  <article class="indicador" :class="{ destacado }">
    <div class="indicador-cabecera">
      <span>{{ titulo }}</span>
      <IconoSistema v-if="icono" :nombre="icono" />
    </div>
    <p class="indicador-valor" :aria-label="valor">{{ valorMostrado }}</p>
    <p class="indicador-descripcion">{{ descripcion }}</p>
  </article>
</template>

<style scoped>
.indicador {
  min-height: 146px; padding: 22px; border: 1px solid var(--color-borde); border-radius: var(--radio);
  background: var(--color-superficie); box-shadow: var(--sombra);
  transition: transform var(--duracion-media) var(--curva-suave), box-shadow var(--duracion-media) var(--curva-suave), border-color var(--duracion-media) ease;
}
.indicador:hover { transform: translateY(-3px); box-shadow: var(--sombra-elevada); border-color: #c9d6e6; }
.indicador-cabecera .icono { transition: transform var(--duracion-media) var(--curva-rebote); }
.indicador:hover .indicador-cabecera .icono { transform: scale(1.15) rotate(-6deg); }
.indicador-valor { font-variant-numeric: tabular-nums; }
.indicador-cabecera { display: flex; justify-content: space-between; align-items: center; font-size: 14px; color: var(--color-texto-suave); }
.indicador-cabecera .icono { color: var(--color-primario); }
.indicador-valor { margin: 14px 0 16px; font-size: 32px; font-weight: 700; line-height: 1.1; }
.indicador-descripcion { font-size: 12.5px; color: var(--color-texto-suave); }
.indicador.destacado { background: var(--color-primario-oscuro); border-color: var(--color-primario-oscuro); color: #fff; }
.indicador.destacado .indicador-cabecera, .indicador.destacado .indicador-descripcion { color: rgba(255, 255, 255, 0.85); }
.indicador.destacado .indicador-cabecera .icono { color: #fff; }
</style>
