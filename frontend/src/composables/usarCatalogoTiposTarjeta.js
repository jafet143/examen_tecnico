import { ref, computed } from 'vue'
import { obtenerCatalogoTiposTarjeta } from '../servicios/catalogosServicio'
import { obtenerMensajeError } from '../utilidades/errores'

const catalogo = ref(null)
const errorCatalogo = ref('')
let cargaEnCurso = null

export function usarCatalogoTiposTarjeta() {
  async function cargarCatalogo() {
    if (catalogo.value) return
    cargaEnCurso ??= obtenerCatalogoTiposTarjeta()
      .then((datos) => {
        catalogo.value = datos
        errorCatalogo.value = ''
      })
      .catch((error) => {
        errorCatalogo.value = obtenerMensajeError(error)
      })
      .finally(() => {
        cargaEnCurso = null
      })
    await cargaEnCurso
  }

  const tiposTarjeta = computed(() => catalogo.value?.tiposTarjeta ?? [])
  const porcentajeMaximoLimiteSobreIngreso = computed(() => catalogo.value?.porcentajeMaximoLimiteSobreIngreso ?? null)
  const motivosBloqueo = computed(() => catalogo.value?.motivosBloqueo ?? [])
  const motivosReposicion = computed(() => catalogo.value?.motivosReposicion ?? [])
  const motivosCancelacion = computed(() => catalogo.value?.motivosCancelacion ?? [])
  const tiposCargoManual = computed(() => catalogo.value?.tiposCargoManual ?? [])
  const comisionReposicionExtravio = computed(() => catalogo.value?.comisionReposicionExtravio ?? null)
  const mesesMinimosEstabilidadLaboral = computed(() => catalogo.value?.mesesMinimosEstabilidadLaboral ?? null)
  const diaCorte = computed(() => catalogo.value?.diaCorte ?? null)
  const diaLimitePago = computed(() => catalogo.value?.diaLimitePago ?? null)
  const porcentajePagoMinimo = computed(() => catalogo.value?.porcentajePagoMinimo ?? null)
  const tasaInteresMensual = computed(() => catalogo.value?.tasaInteresMensual ?? null)

  return { tiposTarjeta, motivosBloqueo, motivosReposicion, motivosCancelacion, tiposCargoManual, comisionReposicionExtravio, mesesMinimosEstabilidadLaboral, porcentajeMaximoLimiteSobreIngreso, diaCorte, diaLimitePago, porcentajePagoMinimo, tasaInteresMensual, errorCatalogo, cargarCatalogo }
}
