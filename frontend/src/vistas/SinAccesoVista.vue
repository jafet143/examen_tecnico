<script setup>
import EncabezadoPagina from '../componentes/EncabezadoPagina.vue'
import IconoSistema from '../componentes/IconoSistema.vue'
</script>

<template>
  <div>
    <EncabezadoPagina :migas="[{ texto: 'Sin acceso' }]" titulo="Sin acceso" ruta-regreso="/resumen" />
    <section class="panel estado-vacio sin-acceso">
      <span class="icono-sin-acceso"><IconoSistema nombre="candado" :tamano="28" /></span>
      <p class="mensaje-sin-acceso">No tienes permiso para ver esta página</p>
      <p>Si necesitas acceder, solicita a un administrador que revise los permisos de tu perfil.</p>
      <div class="acciones-sin-acceso">
        <RouterLink class="boton" to="/resumen">Ir al inicio</RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.sin-acceso { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 56px 24px; }
.icono-sin-acceso { display: inline-flex; align-items: center; justify-content: center; width: 64px; height: 64px; border-radius: 50%; background: var(--color-primario-suave); color: var(--color-primario); }
.mensaje-sin-acceso { font-size: 20px; font-weight: 600; color: var(--color-texto); }
.acciones-sin-acceso { display: flex; flex-wrap: wrap; justify-content: center; gap: 12px; margin-top: 12px; }
</style>
