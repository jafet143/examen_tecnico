<script setup>
import { useRouter } from 'vue-router'
import IconoSistema from './IconoSistema.vue'

const props = defineProps({
  rutaRegreso: { type: String, default: '' },
  migas: { type: Array, default: () => [] },
  titulo: { type: String, required: true },
  descripcion: { type: String, default: '' }
})

const enrutador = useRouter()

function regresar() {
  if (window.history.state?.back) enrutador.back()
  else enrutador.push(props.rutaRegreso)
}
</script>

<template>
  <header class="encabezado-pagina">
    <div class="encabezado-navegacion">
      <button v-if="rutaRegreso" type="button" class="boton boton-secundario boton-regresar" @click="regresar">
        <IconoSistema nombre="flecha-izquierda" :tamano="16" />Regresar
      </button>
      <nav class="migas" aria-label="Ruta de navegación">
        <template v-for="(miga, posicion) in migas" :key="miga.texto">
          <span v-if="posicion > 0" class="migas-separador">/</span>
          <RouterLink v-if="miga.ruta" :to="miga.ruta">{{ miga.texto }}</RouterLink>
          <span v-else>{{ miga.texto }}</span>
        </template>
      </nav>
    </div>
    <div class="encabezado-fila">
      <div class="encabezado-texto">
        <div class="encabezado-titulo">
          <h1>{{ titulo }}</h1>
          <slot name="insignia" />
        </div>
        <p v-if="descripcion">{{ descripcion }}</p>
      </div>
      <div v-if="$slots.default" class="encabezado-acciones"><slot /></div>
    </div>
  </header>
</template>

<style scoped>
.encabezado-pagina { margin-bottom: 24px; }
.encabezado-navegacion { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 16px; margin-bottom: 14px; }
.boton-regresar { height: 32px; padding: 0 12px; gap: 6px; font-size: 13px; }
.migas { display: flex; flex-wrap: wrap; gap: 8px; font-size: 12.5px; color: var(--color-texto-suave); }
.migas a { color: var(--color-texto-suave); }
.migas a:hover { color: var(--color-primario); }
.encabezado-fila { display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between; gap: 16px; }
h1 { font-size: 30px; font-weight: 700; line-height: 1.2; color: var(--color-texto); }
.encabezado-texto p { margin-top: 10px; font-size: 15px; color: var(--color-texto-suave); }
.encabezado-titulo { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 14px; }
.encabezado-acciones { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
@media (max-width: 700px) { h1 { font-size: 24px; } }
</style>
