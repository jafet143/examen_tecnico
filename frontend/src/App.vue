<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BarraLateral from './componentes/BarraLateral.vue'
import BarraSuperior from './componentes/BarraSuperior.vue'
import MensajeAlerta from './componentes/MensajeAlerta.vue'
import { usarNotificacion } from './composables/usarNotificacion'

const ruta = useRoute()
const menuLateralAbierto = ref(false)
const { notificacion, cerrarNotificacion } = usarNotificacion()

watch(() => ruta.fullPath, () => {
  menuLateralAbierto.value = false
})
</script>

<template>
  <BarraLateral :abierta="menuLateralAbierto" @cerrar="menuLateralAbierto = false" />
  <div class="aplicacion">
    <BarraSuperior @abrir-menu="menuLateralAbierto = true" />
    <div class="notificaciones-flotantes">
      <MensajeAlerta :mensaje="notificacion.mensaje" :tipo="notificacion.tipo" :auto-cerrar-ms="5000" @cerrar="cerrarNotificacion" />
    </div>
    <main class="area-trabajo">
      <RouterView v-slot="{ Component }">
        <Transition name="pagina" mode="out-in">
          <component :is="Component" :key="ruta.path" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

<style>
.aplicacion { display: flex; flex-direction: column; min-height: 100vh; margin-left: var(--ancho-barra-lateral); }
.area-trabajo { flex: 1; width: 100%; padding: 32px 32px 40px; }
@media (max-width: 900px) {
  .aplicacion { margin-left: 0; }
  .area-trabajo { padding: 24px 16px 32px; }
}
</style>
