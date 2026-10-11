<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import IconoSistema from './IconoSistema.vue'
import { obtenerDatosUsuario, obtenerPerfil, cerrarSesion } from '../autenticacion/servicioAutenticacion'
import { obtenerIniciales } from '../utilidades/formatos'

const emit = defineEmits(['abrir-menu'])

const { nombreCompleto, nombre, apellido } = obtenerDatosUsuario()
const iniciales = obtenerIniciales(nombre, apellido) || '?'
const etiquetaPerfil = obtenerPerfil()

const menuUsuarioAbierto = ref(false)
const contenedorUsuario = ref(null)

function cerrarMenuAlHacerClicFuera(evento) {
  if (contenedorUsuario.value && !contenedorUsuario.value.contains(evento.target)) {
    menuUsuarioAbierto.value = false
  }
}

onMounted(() => document.addEventListener('click', cerrarMenuAlHacerClicFuera))
onBeforeUnmount(() => document.removeEventListener('click', cerrarMenuAlHacerClicFuera))
</script>

<template>
  <header class="barra-superior">
    <div class="barra-izquierda">
      <button type="button" class="boton-menu" aria-label="Abrir menú" @click="emit('abrir-menu')">
        <IconoSistema nombre="menu" :tamano="24" />
      </button>
      <h2 class="titulo-sistema">Sistema de Gestión de Tarjetas de Crédito</h2>
    </div>

    <div ref="contenedorUsuario" class="usuario">
      <button type="button" class="usuario-boton" :aria-expanded="menuUsuarioAbierto" @click="menuUsuarioAbierto = !menuUsuarioAbierto">
        <span class="avatar avatar-mediano">{{ iniciales }}</span>
        <span class="usuario-datos">
          <strong>{{ nombreCompleto }}</strong>
          <small>{{ etiquetaPerfil }}</small>
        </span>
        <IconoSistema nombre="chevron-abajo" :tamano="18" />
      </button>
      <div v-if="menuUsuarioAbierto" class="usuario-menu">
        <button type="button" @click="cerrarSesion">Cerrar sesión</button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.barra-superior {
  position: sticky; top: 0; z-index: 30; display: flex; align-items: center; justify-content: space-between; gap: 16px;
  height: var(--alto-barra-superior); padding: 0 32px; background: var(--color-superficie); border-bottom: 1px solid var(--color-borde);
}
.barra-izquierda { display: flex; align-items: center; gap: 12px; min-width: 0; }
.titulo-sistema { font-size: 16px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.boton-menu { display: none; border: 0; background: transparent; color: var(--color-texto); cursor: pointer; padding: 4px; }
.usuario { position: relative; flex: none; }
.usuario-boton { display: flex; align-items: center; gap: 12px; border: 0; background: transparent; color: var(--color-texto-suave); cursor: pointer; text-align: left; }
.usuario-datos { display: flex; flex-direction: column; gap: 3px; }
.usuario-datos strong { font-size: 13px; color: var(--color-texto); }
.usuario-datos small { font-size: 12px; color: var(--color-texto-suave); }
.usuario-menu {
  position: absolute; right: 0; top: calc(100% + 10px); min-width: 180px; padding: 6px; border: 1px solid var(--color-borde);
  border-radius: var(--radio); background: var(--color-superficie); box-shadow: var(--sombra-modal);
}
.usuario-menu button { width: 100%; padding: 10px 12px; border: 0; border-radius: 6px; background: transparent; text-align: left; font-size: 14px; cursor: pointer; }
.usuario-menu button:hover { background: var(--color-fondo); }

@media (max-width: 900px) {
  .barra-superior { padding: 0 16px; }
  .boton-menu { display: inline-flex; }
  .usuario-datos { display: none; }
}
@media (max-width: 900px) and (min-width: 600px) {
  .usuario-datos { display: flex; }
}
@media (max-width: 700px) {
  .titulo-sistema { font-size: 14px; }
}
</style>
