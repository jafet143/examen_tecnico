<script setup>
import { ref } from 'vue'
import IconoSistema from './IconoSistema.vue'
import ChipTarjeta from './ChipTarjeta.vue'
import { formatearNumeroEnmascarado, formatearVencimiento } from '../utilidades/formatos'

defineProps({
  numeroTarjeta: { type: String, required: true },
  codigoTipo: { type: String, default: '' },
  tipo: { type: String, required: true },
  titular: { type: String, required: true },
  fechaVencimiento: { type: String, default: '' },
  bloqueada: { type: Boolean, default: false },
  cancelada: { type: Boolean, default: false }
})

const INCLINACION_MAXIMA_GRADOS = 8
const tarjeta = ref(null)
const estiloMovimiento = ref({})

function seguirPuntero(evento) {
  const limites = tarjeta.value.getBoundingClientRect()
  const proporcionHorizontal = (evento.clientX - limites.left) / limites.width
  const proporcionVertical = (evento.clientY - limites.top) / limites.height
  estiloMovimiento.value = {
    '--inclinacion-y': `${(proporcionHorizontal - 0.5) * 2 * INCLINACION_MAXIMA_GRADOS}deg`,
    '--inclinacion-x': `${(0.5 - proporcionVertical) * 2 * INCLINACION_MAXIMA_GRADOS}deg`,
    '--brillo-x': `${proporcionHorizontal * 100}%`,
    '--brillo-y': `${proporcionVertical * 100}%`,
    '--opacidad-brillo': 1
  }
}

function restablecerPosicion() {
  estiloMovimiento.value = {}
}
</script>

<template>
  <div
    ref="tarjeta"
    class="tarjeta-visual"
    :class="[codigoTipo && `tipo-tarjeta--${codigoTipo.toLowerCase()}`, { 'tarjeta-visual--bloqueada': bloqueada || cancelada }]"
    :style="estiloMovimiento"
    aria-label="Representación de la tarjeta"
    @pointermove="seguirPuntero"
    @pointerleave="restablecerPosicion"
  >
    <span class="tarjeta-visual-brillo" aria-hidden="true"></span>
    <div class="tarjeta-visual-superior">
      <span class="tarjeta-visual-marca">CHN</span>
      <span class="tarjeta-visual-tipo">{{ tipo }}</span>
    </div>
    <ChipTarjeta />
    <p class="tarjeta-visual-numero">{{ formatearNumeroEnmascarado(numeroTarjeta) }}</p>
    <div class="tarjeta-visual-fila">
      <p class="tarjeta-visual-titular">{{ titular }}</p>
      <p v-if="fechaVencimiento" class="tarjeta-visual-vence"><small>VENCE</small> {{ formatearVencimiento(fechaVencimiento) }}</p>
    </div>
    <div v-if="cancelada" class="tarjeta-visual-velo tarjeta-visual-velo--cancelada"><IconoSistema nombre="prohibido" :tamano="28" />CANCELADA</div>
    <div v-else-if="bloqueada" class="tarjeta-visual-velo"><IconoSistema nombre="candado" :tamano="28" />BLOQUEADA</div>
  </div>
</template>

<style scoped>
.tarjeta-visual {
  display: flex; flex-direction: column; justify-content: space-between; gap: 14px; width: 100%; max-width: 400px;
  aspect-ratio: 85.6 / 53.98; min-height: 196px; padding: 22px 24px;
  position: relative; overflow: hidden; border-radius: 14px; background: var(--tarjeta-fondo, linear-gradient(135deg, #174b8c, #12386a)); color: var(--tarjeta-texto, #fff); box-shadow: var(--sombra);
  transform: perspective(900px) rotateX(var(--inclinacion-x, 0deg)) rotateY(var(--inclinacion-y, 0deg));
  transition: transform 0.35s var(--curva-suave), box-shadow 0.35s var(--curva-suave);
  will-change: transform;
}
.tarjeta-visual:hover { box-shadow: 0 18px 40px rgba(18, 56, 106, 0.28); transition-duration: 0.08s, 0.35s; }
.tarjeta-visual-brillo {
  position: absolute; inset: 0; pointer-events: none; opacity: var(--opacidad-brillo, 0);
  background: radial-gradient(circle at var(--brillo-x, 50%) var(--brillo-y, 50%), rgba(255, 255, 255, 0.35), transparent 55%);
  transition: opacity 0.35s ease;
  mix-blend-mode: soft-light;
}
@media (prefers-reduced-motion: reduce) {
  .tarjeta-visual { transform: none; }
  .tarjeta-visual-brillo { display: none; }
}
.tarjeta-visual-superior { display: flex; justify-content: space-between; align-items: center; }
.tarjeta-visual-marca { font-size: 26px; font-weight: 700; }
.tarjeta-visual-tipo { font-size: 13px; letter-spacing: 0.04em; text-transform: uppercase; color: var(--tarjeta-texto-suave, rgba(255, 255, 255, 0.85)); }
.tarjeta-visual-fila { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; }
.tarjeta-visual-vence { flex: none; font-size: 13px; font-weight: 600; letter-spacing: 0.06em; }
.tarjeta-visual-vence small { margin-right: 4px; font-size: 9px; font-weight: 400; color: var(--tarjeta-texto-suave, rgba(255, 255, 255, 0.85)); }
.tarjeta-visual-numero { font-size: 19px; font-weight: 600; letter-spacing: 0.14em; }
.tarjeta-visual-titular { font-size: 12px; letter-spacing: 0.05em; text-transform: uppercase; color: var(--tarjeta-texto-suave, rgba(255, 255, 255, 0.85)); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tarjeta-visual--bloqueada { filter: grayscale(1); }
.tarjeta-visual-velo {
  position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;
  background: rgba(32, 46, 67, 0.78); color: #fff; font-size: 14px; font-weight: 700; letter-spacing: 0.12em;
}
.tarjeta-visual-velo--cancelada { background: rgba(101, 116, 138, 0.85); }
</style>
