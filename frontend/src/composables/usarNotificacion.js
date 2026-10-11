import { ref } from 'vue'

const notificacion = ref({ mensaje: '', tipo: 'exito' })

export function usarNotificacion() {
  function mostrarNotificacion(mensaje, tipo = 'exito') {
    notificacion.value = { mensaje, tipo }
  }

  function cerrarNotificacion() {
    notificacion.value = { mensaje: '', tipo: 'exito' }
  }

  return { notificacion, mostrarNotificacion, cerrarNotificacion }
}
