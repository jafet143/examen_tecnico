import { ref } from 'vue'
import { listarSolicitudes } from '../servicios/solicitudesServicio'

const cantidadPendientes = ref(0)

export function usarSolicitudesPendientes() {
  async function actualizarCantidadPendientes() {
    try {
      cantidadPendientes.value = (await listarSolicitudes('PENDIENTE')).length
    } catch {
    }
  }

  return { cantidadPendientes, actualizarCantidadPendientes }
}
