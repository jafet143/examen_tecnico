<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  id: { type: String, required: true },
  modelValue: { type: [String, Number], default: '' }
})
const emit = defineEmits(['update:modelValue'])

const DECIMALES_MONEDA = 2
const formatoMoneda = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: DECIMALES_MONEDA,
  maximumFractionDigits: DECIMALES_MONEDA
})

const enEdicion = ref(false)
const textoVisible = ref('')

function tieneValor(valor) {
  return valor !== '' && valor !== null && valor !== undefined && !Number.isNaN(Number(valor))
}

function formatearParaMostrar(valor) {
  return tieneValor(valor) ? formatoMoneda.format(Number(valor)) : ''
}

function dejarSoloImporte(texto) {
  const soloDigitosYPunto = texto.replace(/[^\d.]/g, '')
  const [enteros, ...partesDecimales] = soloDigitosYPunto.split('.')
  if (partesDecimales.length === 0) return enteros
  return `${enteros}.${partesDecimales.join('').slice(0, DECIMALES_MONEDA)}`
}

watch(
  () => props.modelValue,
  (valor) => {
    if (!enEdicion.value) textoVisible.value = formatearParaMostrar(valor)
  },
  { immediate: true }
)

function alEscribir(evento) {
  const importe = dejarSoloImporte(evento.target.value)
  textoVisible.value = importe
  evento.target.value = importe
  emit('update:modelValue', importe)
}

function alEnfocar() {
  enEdicion.value = true
  textoVisible.value = tieneValor(props.modelValue) ? String(props.modelValue) : ''
}

function alSalir() {
  enEdicion.value = false
  if (tieneValor(props.modelValue)) {
    const importeConDecimales = Number(props.modelValue).toFixed(DECIMALES_MONEDA)
    emit('update:modelValue', importeConDecimales)
    textoVisible.value = formatearParaMostrar(importeConDecimales)
  } else {
    textoVisible.value = ''
  }
}
</script>

<template>
  <div class="campo-moneda">
    <span>Q</span>
    <input
      :id="id"
      :value="textoVisible"
      type="text"
      inputmode="decimal"
      autocomplete="off"
      placeholder="0.00"
      @input="alEscribir"
      @focus="alEnfocar"
      @blur="alSalir"
    />
  </div>
</template>
