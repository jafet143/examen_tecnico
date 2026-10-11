import { ref, computed, watch } from 'vue'

export function usarPaginacion(elementos, tamanoPagina = 10) {
  const paginaActual = ref(1)
  const totalPaginas = computed(() => Math.max(1, Math.ceil(elementos.value.length / tamanoPagina)))

  const elementosDePagina = computed(() => {
    const inicio = (paginaActual.value - 1) * tamanoPagina
    return elementos.value.slice(inicio, inicio + tamanoPagina)
  })

  watch(elementos, () => {
    paginaActual.value = 1
  })

  return { paginaActual, totalPaginas, elementosDePagina, tamanoPagina }
}
