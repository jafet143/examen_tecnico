<script setup>
import { ref, computed, onMounted } from 'vue'
import SelectorPestanas from '../componentes/SelectorPestanas.vue'
import EsqueletoCarga from '../componentes/EsqueletoCarga.vue'
import EncabezadoPagina from '../componentes/EncabezadoPagina.vue'
import IconoSistema from '../componentes/IconoSistema.vue'
import MensajeAlerta from '../componentes/MensajeAlerta.vue'
import EtiquetaEstado from '../componentes/EtiquetaEstado.vue'
import AvisoInformativo from '../componentes/AvisoInformativo.vue'
import Paginacion from '../componentes/Paginacion.vue'
import { listarSolicitudes } from '../servicios/solicitudesServicio'
import { tienePermiso } from '../autenticacion/servicioAutenticacion'
import { PERMISOS } from '../utilidades/permisos'
import { usarPaginacion } from '../composables/usarPaginacion'
import { ESTADOS_SOLICITUD } from '../utilidades/constantes'
import { formatearMoneda, formatearFecha, formatearCodigoSolicitud } from '../utilidades/formatos'
import { ordenarSolicitudesRecientesPrimero } from '../utilidades/ordenamiento'
import { obtenerMensajeError } from '../utilidades/errores'

const puedeRegistrar = tienePermiso(PERMISOS.SOLICITUDES_REGISTRAR)
const puedeDecidir = tienePermiso(PERMISOS.SOLICITUDES_DECIDIR)

const solicitudes = ref([])
const estadoFiltro = ref('')
const textoBusqueda = ref('')
const cargando = ref(true)
const mensajeError = ref('')

const pestanas = computed(() => [
  { valor: '', etiqueta: 'Todas', cantidad: solicitudes.value.length },
  ...ESTADOS_SOLICITUD.map((estado) => ({
    valor: estado.valor,
    etiqueta: estado.etiqueta,
    cantidad: solicitudes.value.filter((solicitud) => solicitud.estado === estado.valor).length
  }))
])

const solicitudesFiltradas = computed(() => {
  const texto = textoBusqueda.value.trim().toLowerCase()
  return solicitudes.value.filter((solicitud) => {
    if (estadoFiltro.value && solicitud.estado !== estadoFiltro.value) return false
    if (!texto) return true
    return `${solicitud.nombreCliente} ${solicitud.codigoCliente} ${formatearCodigoSolicitud(solicitud.id)}`.toLowerCase().includes(texto)
  })
})
const { paginaActual, elementosDePagina: solicitudesDePagina } = usarPaginacion(solicitudesFiltradas)

function describirAccion(solicitud) {
  return solicitud.estado === 'PENDIENTE' && puedeDecidir ? 'Revisar' : 'Ver detalle'
}

async function cargarSolicitudes() {
  cargando.value = true
  mensajeError.value = ''
  try {
    solicitudes.value = ordenarSolicitudesRecientesPrimero(await listarSolicitudes(''))
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    cargando.value = false
  }
}

onMounted(cargarSolicitudes)
</script>

<template>
  <div>
    <EncabezadoPagina
      :migas="[{ texto: 'Gestión de tarjetas' }, { texto: 'Solicitudes' }]"
      titulo="Solicitudes"
      descripcion="Da seguimiento a las solicitudes de tarjetas de crédito y sus decisiones."
    >
      <RouterLink v-if="puedeRegistrar" class="boton" to="/solicitudes/nueva"><IconoSistema nombre="mas" :tamano="18" />Nueva solicitud</RouterLink>
    </EncabezadoPagina>

    <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

    <div class="barra-herramientas">
      <SelectorPestanas v-model="estadoFiltro" :opciones="pestanas" etiqueta-grupo="Filtrar solicitudes por estado" />
      <div class="buscador">
        <IconoSistema nombre="buscar" :tamano="18" />
        <input v-model="textoBusqueda" type="search" placeholder="Buscar cliente, código o solicitud" aria-label="Buscar solicitudes" />
      </div>
    </div>

    <section class="panel">
      <EsqueletoCarga v-if="cargando" variante="tabla" etiqueta="Cargando solicitudes" />
      <p v-else-if="solicitudesFiltradas.length === 0" class="estado-vacio">
        {{ solicitudes.length === 0 ? 'Aún no hay solicitudes registradas.' : 'Ninguna solicitud coincide con el filtro.' }}
      </p>
      <template v-else>
        <div class="contenedor-tabla">
          <table class="tabla">
            <thead>
              <tr>
                <th>Solicitud</th>
                <th>Cliente</th>
                <th>Tipo</th>
                <th>Fecha</th>
                <th>Ingreso mensual</th>
                <th>Estado</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="solicitud in solicitudesDePagina" :key="solicitud.id">
                <td>{{ formatearCodigoSolicitud(solicitud.id) }}</td>
                <td>
                  <p>{{ solicitud.nombreCliente }}</p>
                  <p class="tabla-secundaria">Código <span class="codigo-cliente">{{ solicitud.codigoCliente }}</span></p>
                </td>
                <td>{{ solicitud.descripcionTipoTarjeta }}</td>
                <td>{{ formatearFecha(solicitud.fechaSolicitud) }}</td>
                <td>{{ formatearMoneda(solicitud.ingresoMensual) }}</td>
                <td><EtiquetaEstado :estado="solicitud.estado" /></td>
                <td>
                  <RouterLink class="enlace-accion" :to="`/solicitudes/${solicitud.id}/revision`">
                    {{ describirAccion(solicitud) }}<IconoSistema nombre="flecha-derecha" :tamano="16" />
                  </RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Paginacion v-model:pagina-actual="paginaActual" :total="solicitudesFiltradas.length" etiqueta-singular="solicitud" etiqueta-plural="solicitudes" />
      </template>
    </section>

    <AvisoInformativo titulo="Las decisiones se registran una sola vez">
      Solo las solicitudes en estado Pendiente pueden aprobarse o rechazarse. Las decisiones conservan su fecha, motivo o comentario y, si se aprueban, el monto autorizado.
    </AvisoInformativo>
  </div>
</template>
