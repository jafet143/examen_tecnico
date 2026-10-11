<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EsqueletoCarga from '../componentes/EsqueletoCarga.vue'
import EncabezadoPagina from '../componentes/EncabezadoPagina.vue'
import IconoSistema from '../componentes/IconoSistema.vue'
import MensajeAlerta from '../componentes/MensajeAlerta.vue'
import EtiquetaEstado from '../componentes/EtiquetaEstado.vue'
import ModalEliminarCliente from '../componentes/ModalEliminarCliente.vue'
import InsigniasEstadoCuenta from '../componentes/InsigniasEstadoCuenta.vue'
import IndicadorTipoTarjeta from '../componentes/IndicadorTipoTarjeta.vue'
import { obtenerCliente, listarSolicitudesDeCliente } from '../servicios/clientesServicio'
import { listarCuentasDeCliente } from '../servicios/cuentasServicio'
import NumeroProtegido from '../componentes/NumeroProtegido.vue'
import { tienePermiso } from '../autenticacion/servicioAutenticacion'
import { PERMISOS } from '../utilidades/permisos'
import { ESTADO_CUENTA_CANCELADA } from '../utilidades/constantes'
import { usarNotificacion } from '../composables/usarNotificacion'
import {
  formatearMoneda,
  formatearFecha,
  formatearCodigoSolicitud,
  obtenerIniciales
} from '../utilidades/formatos'
import { obtenerMensajeError } from '../utilidades/errores'

const ruta = useRoute()
const enrutador = useRouter()
const idCliente = computed(() => Number(ruta.params.idCliente))
const puedeGestionar = tienePermiso(PERMISOS.CLIENTES_GESTIONAR)
const puedeEliminar = tienePermiso(PERMISOS.CLIENTES_ELIMINAR)
const puedeConsultarSolicitudes = tienePermiso(PERMISOS.SOLICITUDES_CONSULTAR)
const puedeRegistrarSolicitud = tienePermiso(PERMISOS.SOLICITUDES_REGISTRAR)
const { mostrarNotificacion } = usarNotificacion()

const cliente = ref(null)
const solicitudes = ref([])
const cuentas = ref([])
const cargando = ref(true)
const mensajeError = ref('')
const eliminacionSolicitada = ref(false)

const resumenSolicitudes = computed(() => {
  const total = solicitudes.value.length
  if (total === 0) return 'Sin solicitudes registradas'
  const contar = (estado) => solicitudes.value.filter((solicitud) => solicitud.estado === estado).length
  const detalle = [
    [contar('PENDIENTE'), 'pendiente', 'pendientes'],
    [contar('APROBADA'), 'aprobada', 'aprobadas'],
    [contar('RECHAZADA'), 'rechazada', 'rechazadas']
  ]
    .filter(([cantidad]) => cantidad > 0)
    .map(([cantidad, singular, plural]) => `${cantidad} ${cantidad === 1 ? singular : plural}`)
  return `${total} ${total === 1 ? 'solicitud' : 'solicitudes'} · ${detalle.join(', ')}`
})
const cuentasActivas = computed(() => cuentas.value.filter((cuenta) => cuenta.estadoCuenta !== ESTADO_CUENTA_CANCELADA))
const cuentasCanceladas = computed(() => cuentas.value.filter((cuenta) => cuenta.estadoCuenta === ESTADO_CUENTA_CANCELADA))
const resumenCuentas = computed(() => {
  const total = cuentasActivas.value.length
  if (total === 0) return 'Sin productos activos'
  return `${total} ${total === 1 ? 'producto activo' : 'productos activos'} · El límite corresponde al monto aprobado`
})
const resumenHistorial = computed(() => {
  const total = cuentasCanceladas.value.length
  if (total === 0) return 'Sin productos cancelados'
  return `${total} ${total === 1 ? 'producto cancelado' : 'productos cancelados'} · El historial se conserva`
})

function describirMontoAprobado(solicitud) {
  if (solicitud.estado === 'RECHAZADA') return 'No aplica'
  if (solicitud.estado === 'PENDIENTE') return 'Por definir'
  return formatearMoneda(solicitud.montoAprobado)
}

async function cargarDatos() {
  mensajeError.value = ''
  try {
    ;[cliente.value, solicitudes.value, cuentas.value] = await Promise.all([
      obtenerCliente(idCliente.value),
      puedeConsultarSolicitudes ? listarSolicitudesDeCliente(idCliente.value) : [],
      listarCuentasDeCliente(idCliente.value)
    ])
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    cargando.value = false
  }
}

function alEliminarCliente() {
  mostrarNotificacion(`Cliente ${cliente.value.nombreCompleto} eliminado correctamente`)
  enrutador.push('/clientes')
}

onMounted(cargarDatos)
</script>

<template>
  <div>
    <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />
    <EsqueletoCarga v-if="cargando" variante="detalle" etiqueta="Cargando cliente" />

    <template v-else-if="cliente">
      <EncabezadoPagina
        :migas="[{ texto: 'Clientes', ruta: '/clientes' }, { texto: 'Detalle del cliente' }]"
        :titulo="cliente.nombreCompleto"
        descripcion="Información del cliente y sus productos asociados."
        ruta-regreso="/clientes"
      >
        <template #insignia><span class="insignia-codigo">Código {{ cliente.codigoCliente }}</span></template>
        <RouterLink v-if="puedeGestionar" class="boton boton-secundario" :to="`/clientes/${cliente.id}/editar`">
          <IconoSistema nombre="lapiz" :tamano="18" />Editar cliente
        </RouterLink>
        <button v-if="puedeEliminar" type="button" class="boton boton-secundario" @click="eliminacionSolicitada = true">
          <IconoSistema nombre="papelera" :tamano="18" />Eliminar
        </button>
      </EncabezadoPagina>

      <section class="panel datos-cliente">
        <span class="avatar avatar-grande">{{ obtenerIniciales(cliente.nombre, cliente.apellido) }}</span>
        <div class="rejilla-datos-cliente">
          <div><p class="dato-etiqueta">Nombre</p><p class="dato-valor">{{ cliente.nombre }}</p></div>
          <div><p class="dato-etiqueta">Apellido</p><p class="dato-valor">{{ cliente.apellido }}</p></div>
          <div><p class="dato-etiqueta">Correo electrónico</p><p class="dato-valor">{{ cliente.correoElectronico }}</p></div>
          <div><p class="dato-etiqueta">Teléfono</p><p class="dato-valor">{{ cliente.telefono }}</p></div>
          <div><p class="dato-etiqueta">Número de identificación</p><p class="dato-valor">{{ cliente.numeroIdentificacion }}</p></div>
          <div><p class="dato-etiqueta">Fecha de nacimiento</p><p class="dato-valor">{{ formatearFecha(cliente.fechaNacimiento) }}</p></div>
          <div class="dato-ancho"><p class="dato-etiqueta">Dirección</p><p class="dato-valor">{{ cliente.direccion }}</p></div>
        </div>
      </section>

      <section v-if="puedeConsultarSolicitudes" class="panel">
        <div class="panel-cabecera">
          <div>
            <h2 class="panel-titulo">Solicitudes del cliente</h2>
            <p class="panel-subtitulo">{{ resumenSolicitudes }}</p>
          </div>
          <RouterLink v-if="puedeRegistrarSolicitud" class="boton boton-secundario" :to="{ path: '/solicitudes/nueva', query: { idCliente: cliente.id } }">
            <IconoSistema nombre="mas" :tamano="18" />Nueva solicitud
          </RouterLink>
        </div>
        <p v-if="solicitudes.length === 0" class="estado-vacio">Este cliente no tiene solicitudes.</p>
        <div v-else class="contenedor-tabla">
          <table class="tabla">
            <thead>
              <tr>
                <th>Solicitud</th>
                <th>Tipo</th>
                <th>Fecha</th>
                <th>Ingreso mensual</th>
                <th>Estado</th>
                <th>Monto aprobado</th>
                <th>Decisión y motivo</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="solicitud in solicitudes" :key="solicitud.id">
                <td><RouterLink class="enlace-accion" :to="`/solicitudes/${solicitud.id}/revision`">{{ formatearCodigoSolicitud(solicitud.id) }}</RouterLink></td>
                <td>{{ solicitud.descripcionTipoTarjeta }}</td>
                <td>{{ formatearFecha(solicitud.fechaSolicitud) }}</td>
                <td>{{ formatearMoneda(solicitud.ingresoMensual) }}</td>
                <td><EtiquetaEstado :estado="solicitud.estado" /></td>
                <td>{{ describirMontoAprobado(solicitud) }}</td>
                <td class="celda-ajustable">
                  <template v-if="solicitud.fechaDecision">
                    <p class="celda-fuerte">{{ formatearFecha(solicitud.fechaDecision) }}</p>
                    <p class="tabla-secundaria">{{ solicitud.comentarioDecision || 'Sin comentario' }}</p>
                  </template>
                  <span v-else class="tabla-secundaria">Sin decisión registrada</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <div class="panel-cabecera">
          <div>
            <h2 class="panel-titulo">Productos activos</h2>
            <p class="panel-subtitulo">{{ resumenCuentas }}</p>
          </div>
        </div>
        <p v-if="cuentasActivas.length === 0" class="estado-vacio">Este cliente no tiene productos activos.</p>
        <div v-else class="contenedor-tabla">
          <table class="tabla tabla-compacta">
            <thead>
              <tr>
                <th>N.º de cuenta</th>
                <th>Tarjeta vigente</th>
                <th>Tipo</th>
                <th>Límite</th>
                <th>Saldo / deuda</th>
                <th>Disponible</th>
                <th>Estado</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cuenta in cuentasActivas" :key="cuenta.id">
                <td><NumeroProtegido :enmascarado="cuenta.numeroCuentaEnmascarado" :completo="cuenta.numeroCuentaCompleto" /></td>
                <td><NumeroProtegido :enmascarado="cuenta.tarjetaVigente?.numeroTarjetaEnmascarado" :completo="cuenta.tarjetaVigente?.numeroTarjetaCompleto" /></td>
                <td><IndicadorTipoTarjeta :codigo="cuenta.tipoTarjeta" :descripcion="cuenta.descripcionTipoTarjeta" /></td>
                <td>{{ formatearMoneda(cuenta.limiteCredito) }}</td>
                <td>{{ formatearMoneda(cuenta.saldoActual) }}</td>
                <td>{{ formatearMoneda(cuenta.creditoDisponible) }}</td>
                <td><InsigniasEstadoCuenta :cuenta="cuenta" /></td>
                <td>
                  <RouterLink class="enlace-accion" :to="`/cuentas/${cuenta.id}`" :aria-label="`Ver detalle de la cuenta de ${cuenta.nombreCliente}`">Ver<IconoSistema nombre="flecha-derecha" :tamano="16" /></RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <div class="panel-cabecera">
          <div>
            <h2 class="panel-titulo">Historial de productos</h2>
            <p class="panel-subtitulo">{{ resumenHistorial }}</p>
          </div>
        </div>
        <p v-if="cuentasCanceladas.length === 0" class="estado-vacio">Este cliente no tiene productos cancelados.</p>
        <div v-else class="contenedor-tabla">
          <table class="tabla tabla-compacta">
            <thead>
              <tr>
                <th>Tipo</th>
                <th>N.º de cuenta</th>
                <th>Apertura</th>
                <th>Cancelación</th>
                <th>Motivo</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cuenta in cuentasCanceladas" :key="cuenta.id">
                <td><IndicadorTipoTarjeta :codigo="cuenta.tipoTarjeta" :descripcion="cuenta.descripcionTipoTarjeta" /></td>
                <td><NumeroProtegido :enmascarado="cuenta.numeroCuentaEnmascarado" :completo="cuenta.numeroCuentaCompleto" /></td>
                <td>{{ formatearFecha(cuenta.fechaApertura) }}</td>
                <td>
                  <p>{{ formatearFecha(cuenta.fechaCancelacion) }}</p>
                  <p v-if="cuenta.usuarioCancelacion" class="tabla-secundaria">por {{ cuenta.usuarioCancelacion }}</p>
                </td>
                <td class="celda-ajustable">
                  <EtiquetaEstado :estado="ESTADO_CUENTA_CANCELADA" etiqueta="Cancelada" class="etiqueta-pequena" />
                  <p>{{ cuenta.descripcionMotivoCancelacion }}</p>
                  <p v-if="cuenta.comentarioCancelacion" class="tabla-secundaria">{{ cuenta.comentarioCancelacion }}</p>
                </td>
                <td>
                  <RouterLink class="enlace-accion" :to="`/cuentas/${cuenta.id}`">Ver<IconoSistema nombre="flecha-derecha" :tamano="16" /></RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <ModalEliminarCliente :cliente="eliminacionSolicitada ? cliente : null" @cerrar="eliminacionSolicitada = false" @eliminado="alEliminarCliente" />
    </template>
  </div>
</template>

<style scoped>
.datos-cliente { display: flex; gap: 32px; padding: 32px 24px; }
.rejilla-datos-cliente { flex: 1; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 22px 24px; }
.dato-ancho { grid-column: span 2; }
@media (max-width: 1100px) { .rejilla-datos-cliente { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 700px) {
  .datos-cliente { flex-direction: column; gap: 20px; }
  .rejilla-datos-cliente { grid-template-columns: 1fr; }
  .dato-ancho { grid-column: auto; }
}
</style>
