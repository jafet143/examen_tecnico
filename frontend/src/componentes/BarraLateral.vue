<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import IconoSistema from './IconoSistema.vue'
import { usarSolicitudesPendientes } from '../composables/usarSolicitudesPendientes'
import { tienePermiso } from '../autenticacion/servicioAutenticacion'
import { PERMISOS } from '../utilidades/permisos'

defineProps({
  abierta: { type: Boolean, default: false }
})
const emit = defineEmits(['cerrar'])

const ruta = useRoute()
const { cantidadPendientes, actualizarCantidadPendientes } = usarSolicitudesPendientes()

const todasLasOpciones = [
  { texto: 'Resumen', ruta: '/resumen', icono: 'inicio' },
  { texto: 'Clientes', ruta: '/clientes', icono: 'usuarios', permiso: PERMISOS.CLIENTES_CONSULTAR },
  { texto: 'Solicitudes', ruta: '/solicitudes', icono: 'documento', permiso: PERMISOS.SOLICITUDES_CONSULTAR, conInsignia: true },
  { texto: 'Cuentas y tarjetas', ruta: '/cuentas', icono: 'tarjeta', permiso: PERMISOS.CUENTAS_CONSULTAR }
]
const opcionesMenu = computed(() => todasLasOpciones.filter((opcion) => !opcion.permiso || tienePermiso(opcion.permiso)))
const puedeConsultarSolicitudes = tienePermiso(PERMISOS.SOLICITUDES_CONSULTAR)

function refrescarInsignia() {
  if (puedeConsultarSolicitudes) actualizarCantidadPendientes()
}
watch(() => ruta.fullPath, refrescarInsignia)
onMounted(refrescarInsignia)
</script>

<template>
  <div v-if="abierta" class="barra-lateral-fondo" @click="emit('cerrar')"></div>
  <aside class="barra-lateral" :class="{ abierta }" aria-label="Navegación principal">
    <div class="marca">
      <p class="marca-nombre">CHN</p>
      <p class="marca-descripcion">Crédito Hipotecario Nacional de Guatemala</p>
    </div>

    <p class="seccion">Gestión de tarjetas</p>
    <nav class="menu">
      <RouterLink v-for="opcion in opcionesMenu" :key="opcion.ruta" :to="opcion.ruta" active-class="activo" @click="emit('cerrar')">
        <IconoSistema :nombre="opcion.icono" />
        <span>{{ opcion.texto }}</span>
        <span v-if="opcion.conInsignia && cantidadPendientes > 0" class="insignia">{{ cantidadPendientes }}</span>
      </RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.barra-lateral {
  position: fixed; inset: 0 auto 0 0; z-index: 40; display: flex; flex-direction: column; width: var(--ancho-barra-lateral);
  padding: 30px 20px 24px; background: var(--color-primario-oscuro); color: #fff; overflow-y: auto;
}
.marca { padding: 0 10px; margin-bottom: 28px; }
.marca-nombre { font-size: 36px; font-weight: 700; line-height: 1; }
.marca-descripcion { margin-top: 14px; font-size: 12px; line-height: 1.5; color: rgba(255, 255, 255, 0.85); }
.seccion { padding: 0 0 10px; margin: 0 -0px; font-size: 11px; font-weight: 600; letter-spacing: 0.05em; text-transform: uppercase; color: rgba(255, 255, 255, 0.75); }
.menu { display: flex; flex-direction: column; gap: 8px; }
.menu a {
  display: flex; align-items: center; gap: 12px; height: 48px; padding: 0 16px; border-radius: var(--radio);
  color: rgba(255, 255, 255, 0.82); font-size: 15px;
}
.menu a:hover { background: rgba(255, 255, 255, 0.08); }
.menu a.activo { background: var(--color-navegacion-activa); color: #fff; }
.insignia {
  display: inline-flex; align-items: center; justify-content: center; min-width: 24px; height: 24px; padding: 0 7px;
  border-radius: 6px; background: var(--color-navegacion-activa); font-size: 12px; font-weight: 500;
}
.menu a.activo .insignia { background: rgba(255, 255, 255, 0.12); }
.barra-lateral-fondo { display: none; }

@media (max-width: 900px) {
  .barra-lateral { transform: translateX(-100%); transition: transform 0.2s ease; }
  .barra-lateral.abierta { transform: translateX(0); }
  .barra-lateral-fondo { display: block; position: fixed; inset: 0; z-index: 35; background: rgba(18, 56, 106, 0.45); }
}
</style>
