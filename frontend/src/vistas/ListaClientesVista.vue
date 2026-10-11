<script setup>
import { ref, computed, onMounted } from 'vue'
import EsqueletoCarga from '../componentes/EsqueletoCarga.vue'
import EncabezadoPagina from '../componentes/EncabezadoPagina.vue'
import IconoSistema from '../componentes/IconoSistema.vue'
import MensajeAlerta from '../componentes/MensajeAlerta.vue'
import Paginacion from '../componentes/Paginacion.vue'
import EtiquetaEstado from '../componentes/EtiquetaEstado.vue'
import ModalEliminarCliente from '../componentes/ModalEliminarCliente.vue'
import { listarClientes } from '../servicios/clientesServicio'
import { tienePermiso } from '../autenticacion/servicioAutenticacion'
import { PERMISOS } from '../utilidades/permisos'
import { usarPaginacion } from '../composables/usarPaginacion'
import { usarNotificacion } from '../composables/usarNotificacion'
import { obtenerIniciales, obtenerCiudadDeDireccion } from '../utilidades/formatos'
import { obtenerMensajeError } from '../utilidades/errores'

const puedeGestionar = tienePermiso(PERMISOS.CLIENTES_GESTIONAR)
const puedeEliminar = tienePermiso(PERMISOS.CLIENTES_ELIMINAR)
const { mostrarNotificacion } = usarNotificacion()

const clientes = ref([])
const textoBusqueda = ref('')
const cargando = ref(true)
const mensajeError = ref('')
const clienteAEliminar = ref(null)

const clientesFiltrados = computed(() => {
  const texto = textoBusqueda.value.trim().toLowerCase()
  if (!texto) return clientes.value
  return clientes.value.filter((cliente) =>
    `${cliente.nombreCompleto} ${cliente.numeroIdentificacion} ${cliente.codigoCliente}`.toLowerCase().includes(texto)
  )
})
const { paginaActual, elementosDePagina: clientesDePagina } = usarPaginacion(clientesFiltrados)

async function cargarClientes() {
  cargando.value = true
  mensajeError.value = ''
  try {
    clientes.value = await listarClientes()
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    cargando.value = false
  }
}

async function alEliminarCliente(clienteEliminado) {
  clienteAEliminar.value = null
  mostrarNotificacion(`Cliente ${clienteEliminado.nombreCompleto} eliminado correctamente`)
  await cargarClientes()
}

onMounted(cargarClientes)
</script>

<template>
  <div>
    <EncabezadoPagina
      :migas="[{ texto: 'Gestión de tarjetas' }, { texto: 'Clientes' }]"
      titulo="Clientes"
      descripcion="Consulta la información y administra el registro de tus clientes."
    >
      <RouterLink v-if="puedeGestionar" class="boton" to="/clientes/nuevo"><IconoSistema nombre="mas" :tamano="18" />Agregar cliente</RouterLink>
    </EncabezadoPagina>

    <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

    <div class="barra-herramientas">
      <div class="buscador">
        <IconoSistema nombre="buscar" :tamano="18" />
        <input v-model="textoBusqueda" type="search" placeholder="Buscar por nombre, código o identificación" aria-label="Buscar clientes" />
      </div>
      <p class="texto-conteo">{{ clientes.length }} {{ clientes.length === 1 ? 'cliente registrado' : 'clientes registrados' }}</p>
    </div>

    <section class="panel">
      <EsqueletoCarga v-if="cargando" variante="tabla" etiqueta="Cargando clientes" />
      <p v-else-if="clientesFiltrados.length === 0" class="estado-vacio">
        {{ clientes.length === 0 ? 'Aún no hay clientes registrados.' : 'Ningún cliente coincide con la búsqueda.' }}
      </p>
      <template v-else>
        <div class="contenedor-tabla">
          <table class="tabla">
            <thead>
              <tr>
                <th>Nombre y apellido</th>
                <th>Identificación</th>
                <th>Correo electrónico</th>
                <th>Teléfono</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cliente in clientesDePagina" :key="cliente.id">
                <td>
                  <div class="celda-persona">
                    <span class="avatar">{{ obtenerIniciales(cliente.nombre, cliente.apellido) }}</span>
                    <div>
                      <p class="celda-fuerte">
                        {{ cliente.nombreCompleto }}
                        <EtiquetaEstado v-if="cliente.tieneCuentasEnMora" estado="EN_MORA" class="etiqueta-pequena" />
                        <EtiquetaEstado v-if="cliente.cantidadProductosActivos === 0" estado="SIN_PRODUCTO" etiqueta="Sin producto activo" class="etiqueta-pequena" />
                      </p>
                      <p class="tabla-secundaria">Código <span class="codigo-cliente">{{ cliente.codigoCliente }}</span> · {{ obtenerCiudadDeDireccion(cliente.direccion) }}</p>
                    </div>
                  </div>
                </td>
                <td>{{ cliente.numeroIdentificacion }}</td>
                <td>{{ cliente.correoElectronico }}</td>
                <td>{{ cliente.telefono }}</td>
                <td>
                  <div class="celda-acciones">
                    <RouterLink class="enlace-accion" :to="`/clientes/${cliente.id}`">Ver<IconoSistema nombre="flecha-derecha" :tamano="16" /></RouterLink>
                    <RouterLink v-if="puedeGestionar" class="boton-icono" :to="`/clientes/${cliente.id}/editar`" aria-label="Editar cliente" title="Editar cliente">
                      <IconoSistema nombre="lapiz" :tamano="18" />
                    </RouterLink>
                    <button v-if="puedeEliminar" type="button" class="boton-icono boton-icono-peligro" aria-label="Eliminar cliente" title="Eliminar cliente" @click="clienteAEliminar = cliente">
                      <IconoSistema nombre="papelera" :tamano="18" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Paginacion v-model:pagina-actual="paginaActual" :total="clientesFiltrados.length" etiqueta-singular="cliente" etiqueta-plural="clientes" />
      </template>
    </section>

    <ModalEliminarCliente :cliente="clienteAEliminar" @cerrar="clienteAEliminar = null" @eliminado="alEliminarCliente" />
  </div>
</template>
