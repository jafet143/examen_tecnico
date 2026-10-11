<script setup>
import { ref, nextTick, onBeforeUnmount } from 'vue'
import IconoSistema from './IconoSistema.vue'

defineProps({
  etiqueta: { type: String, default: 'Más acciones' },
  opciones: { type: Array, required: true }
})
const emit = defineEmits(['seleccionar'])

const abierto = ref(false)
const raiz = ref(null)
const boton = ref(null)
const lista = ref(null)

function obtenerItems() {
  return Array.from(lista.value?.querySelectorAll('[role="menuitem"]:not(:disabled)') ?? [])
}

function cerrarPorClicFuera(evento) {
  if (!raiz.value?.contains(evento.target)) cerrar()
}

async function abrir(enfocarUltimo = false) {
  abierto.value = true
  document.addEventListener('mousedown', cerrarPorClicFuera)
  await nextTick()
  const items = obtenerItems()
  items[enfocarUltimo ? items.length - 1 : 0]?.focus()
}

function cerrar(devolverFoco = false) {
  if (!abierto.value) return
  abierto.value = false
  document.removeEventListener('mousedown', cerrarPorClicFuera)
  if (devolverFoco) boton.value?.focus()
}

function alPulsarBoton(evento) {
  if (evento.key === 'ArrowDown') {
    evento.preventDefault()
    abrir()
  } else if (evento.key === 'ArrowUp') {
    evento.preventDefault()
    abrir(true)
  }
}

function alPulsarMenu(evento) {
  const items = obtenerItems()
  const posicion = items.indexOf(document.activeElement)
  const movimientos = {
    ArrowDown: (posicion + 1) % items.length,
    ArrowUp: (posicion - 1 + items.length) % items.length,
    Home: 0,
    End: items.length - 1
  }
  if (evento.key in movimientos) {
    evento.preventDefault()
    items[movimientos[evento.key]]?.focus()
  } else if (evento.key === 'Escape') {
    evento.preventDefault()
    evento.stopPropagation()
    cerrar(true)
  } else if (evento.key === 'Tab') {
    cerrar()
  }
}

function seleccionar(opcion) {
  cerrar(true)
  emit('seleccionar', opcion.clave)
}

onBeforeUnmount(() => document.removeEventListener('mousedown', cerrarPorClicFuera))
</script>

<template>
  <div v-if="opciones.length > 0" ref="raiz" class="menu-acciones">
    <button
      ref="boton"
      type="button"
      class="boton boton-secundario"
      aria-haspopup="menu"
      :aria-expanded="abierto"
      @click="abierto ? cerrar() : abrir()"
      @keydown="alPulsarBoton"
    >
      <IconoSistema nombre="mas-opciones" :tamano="18" />{{ etiqueta }}<IconoSistema nombre="chevron-abajo" :tamano="16" class="icono-despliegue" />
    </button>
    <Transition name="menu-desplegable">
    <div v-if="abierto" ref="lista" class="menu-acciones-lista" role="menu" :aria-label="etiqueta" @keydown="alPulsarMenu">
      <button
        v-for="opcion in opciones"
        :key="opcion.clave"
        type="button"
        role="menuitem"
        class="menu-acciones-item"
        :class="{ 'menu-acciones-item--peligro': opcion.peligro }"
        :disabled="opcion.deshabilitada"
        :title="opcion.ayuda"
        @click="seleccionar(opcion)"
      >
        <IconoSistema v-if="opcion.icono" :nombre="opcion.icono" :tamano="18" />{{ opcion.texto }}
      </button>
    </div>
    </Transition>
  </div>
</template>

<style scoped>
.menu-acciones { position: relative; }
.menu-acciones .boton .icono-despliegue { transition: transform var(--duracion-media) var(--curva-suave); }
.menu-acciones .boton[aria-expanded='true'] .icono-despliegue { transform: rotate(180deg); }
.menu-acciones-lista {
  transform-origin: top right;
  position: absolute; top: calc(100% + 6px); right: 0; z-index: 30; display: flex; flex-direction: column; min-width: 230px; padding: 6px;
  border: 1px solid var(--color-borde); border-radius: var(--radio); background: var(--color-superficie); box-shadow: var(--sombra-modal);
}
.menu-acciones-item {
  display: flex; align-items: center; gap: 10px; width: 100%; height: 40px; padding: 0 12px; border: 0; border-radius: 6px;
  background: transparent; color: var(--color-texto); font-size: 14px; font-weight: 500; text-align: left; cursor: pointer;
  transition: background-color var(--duracion-rapida) ease, padding-left var(--duracion-media) var(--curva-suave);
}
.menu-acciones-item:hover:not(:disabled) { padding-left: 16px; }
.menu-acciones-item:hover:not(:disabled), .menu-acciones-item:focus-visible { background: var(--color-primario-suave); outline: none; }
.menu-acciones-item--peligro { color: var(--color-peligro); }
.menu-acciones-item--peligro:hover:not(:disabled), .menu-acciones-item--peligro:focus-visible { background: var(--color-peligro-suave); }
.menu-acciones-item:disabled { color: var(--color-deshabilitado); cursor: not-allowed; }
</style>
