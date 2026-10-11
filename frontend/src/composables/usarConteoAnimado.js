import { ref, watch, onBeforeUnmount } from 'vue'

const DURACION_CONTEO_MS = 900
const PATRON_NUMERO = /-?\d[\d,]*(?:\.\d+)?/

function prefiereMenosMovimiento() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

export function usarConteoAnimado(obtenerTexto) {
  const textoMostrado = ref(obtenerTexto())
  let cuadroAnimacion = null

  function animarHasta(textoFinal) {
    cancelAnimationFrame(cuadroAnimacion)
    const coincidencia = String(textoFinal ?? '').match(PATRON_NUMERO)
    if (!coincidencia || prefiereMenosMovimiento()) {
      textoMostrado.value = textoFinal
      return
    }
    const numeroOriginal = coincidencia[0]
    const valorFinal = Number(numeroOriginal.replace(/,/g, ''))
    const decimales = numeroOriginal.includes('.') ? numeroOriginal.split('.')[1].length : 0
    const formato = new Intl.NumberFormat('en-US', {
      minimumFractionDigits: decimales,
      maximumFractionDigits: decimales,
      useGrouping: numeroOriginal.includes(',')
    })
    const inicio = performance.now()
    const avanzar = (instante) => {
      const progreso = Math.min(1, (instante - inicio) / DURACION_CONTEO_MS)
      const progresoSuavizado = 1 - Math.pow(1 - progreso, 3)
      textoMostrado.value = textoFinal.replace(numeroOriginal, formato.format(valorFinal * progresoSuavizado))
      if (progreso < 1) cuadroAnimacion = requestAnimationFrame(avanzar)
    }
    cuadroAnimacion = requestAnimationFrame(avanzar)
  }

  watch(obtenerTexto, animarHasta, { immediate: true })
  onBeforeUnmount(() => cancelAnimationFrame(cuadroAnimacion))

  return textoMostrado
}
