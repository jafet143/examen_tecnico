<script setup>
import { ref, computed, onMounted } from 'vue'
import EsqueletoCarga from '../componentes/EsqueletoCarga.vue'
import EncabezadoPagina from '../componentes/EncabezadoPagina.vue'
import IconoSistema from '../componentes/IconoSistema.vue'
import MensajeAlerta from '../componentes/MensajeAlerta.vue'
import EtiquetaEstado from '../componentes/EtiquetaEstado.vue'
import TarjetaIndicador from '../componentes/TarjetaIndicador.vue'
import { listarClientes } from '../servicios/clientesServicio'
import { listarSolicitudes } from '../servicios/solicitudesServicio'
import { listarCuentas } from '../servicios/cuentasServicio'
import { ESTADO_CUENTA_ACTIVA } from '../utilidades/constantes'
import { tienePermiso } from '../autenticacion/servicioAutenticacion'
import { PERMISOS } from '../utilidades/permisos'
import {
  formatearMoneda,
  formatearFecha,
  formatearFechaCorta,
  formatearPorcentaje,
  formatearCodigoSolicitud
} from '../utilidades/formatos'
import { ordenarSolicitudesRecientesPrimero } from '../utilidades/ordenamiento'
import { obtenerMensajeError } from '../utilidades/errores'

const MAXIMO_PENDIENTES_VISIBLES = 5
const MAXIMO_ACTIVIDADES = 5

const puedeVerClientes = ref(tienePermiso(PERMISOS.CLIENTES_CONSULTAR))
const puedeVerSolicitudes = ref(tienePermiso(PERMISOS.SOLICITUDES_CONSULTAR))
const puedeVerCuentas = ref(tienePermiso(PERMISOS.CUENTAS_CONSULTAR))
const puedeRegistrarSolicitud = tienePermiso(PERMISOS.SOLICITUDES_REGISTRAR)
const puedeDecidir = tienePermiso(PERMISOS.SOLICITUDES_DECIDIR)

const clientes = ref([])
const solicitudes = ref([])
const cuentas = ref([])
const cargando = ref(true)
const mensajeError = ref('')

const solicitudesPendientes = computed(() => solicitudes.value.filter((solicitud) => solicitud.estado === 'PENDIENTE'))
const pendientesVisibles = computed(() => solicitudesPendientes.value.slice(0, MAXIMO_PENDIENTES_VISIBLES))

const tarjetasEmitidas = computed(() => cuentas.value.reduce((total, cuenta) => total + 1 + Number(cuenta.cantidadReposiciones ?? 0), 0))
const descripcionCuentas = computed(() =>
  cuentas.value.length === 0 ? 'Aún sin cuentas abiertas' : `${tarjetasEmitidas.value} ${tarjetasEmitidas.value === 1 ? 'tarjeta emitida' : 'tarjetas emitidas'}`
)

const sumar = (campo) => cuentas.value.reduce((total, cuenta) => total + Number(cuenta[campo]), 0)
const cuentasEnMora = computed(() => cuentas.value.filter((cuenta) => cuenta.estadoPago === 'EN_MORA').length)
const deudaCartera = computed(() => sumar('saldoActual'))
const carteraCredito = computed(() => sumar('limiteCredito'))
const creditoDisponible = computed(() => sumar('creditoDisponible'))
const porcentajeUtilizado = computed(() => (carteraCredito.value > 0 ? (deudaCartera.value / carteraCredito.value) * 100 : 0))

const actividadReciente = computed(() => {
  const eventos = []
  for (const solicitud of solicitudes.value) {
    const detalle = `${solicitud.descripcionTipoTarjeta} · ${formatearCodigoSolicitud(solicitud.id)}`
    eventos.push({
      clave: `solicitud-${solicitud.id}`,
      icono: 'documento-mas',
      titulo: `Nueva solicitud de ${solicitud.nombreCliente}`,
      detalle: `${detalle} · Pendiente`,
      fecha: solicitud.fechaSolicitud
    })
    if (solicitud.fechaDecision) {
      const fueAprobada = solicitud.estado === 'APROBADA'
      eventos.push({
        clave: `decision-${solicitud.id}`,
        icono: fueAprobada ? 'documento-check' : 'documento-x',
        titulo: `Solicitud ${fueAprobada ? 'aprobada' : 'rechazada'} · ${solicitud.nombreCliente}`,
        detalle: fueAprobada ? `${detalle} · ${formatearMoneda(solicitud.montoAprobado)}` : detalle,
        fecha: solicitud.fechaDecision
      })
    }
  }
  return eventos.sort((anterior, siguiente) => new Date(siguiente.fecha) - new Date(anterior.fecha)).slice(0, MAXIMO_ACTIVIDADES)
})

async function cargarSeccion(puedeVer, cargarDatosSeccion, destino) {
  if (!puedeVer.value) return
  try {
    destino.value = await cargarDatosSeccion()
  } catch (error) {
    if (error.response?.status === 403) puedeVer.value = false
    else mensajeError.value = obtenerMensajeError(error)
  }
}

async function cargarDatos() {
  await Promise.all([
    cargarSeccion(puedeVerClientes, listarClientes, clientes),
    cargarSeccion(puedeVerSolicitudes, async () => ordenarSolicitudesRecientesPrimero(await listarSolicitudes('')), solicitudes),
    cargarSeccion(puedeVerCuentas, () => listarCuentas(ESTADO_CUENTA_ACTIVA), cuentas)
  ])
  cargando.value = false
}

onMounted(cargarDatos)
</script>

<template>
  <div>
    <EncabezadoPagina
      :migas="[{ texto: 'Gestión de tarjetas' }, { texto: 'Resumen operativo' }]"
      titulo="Resumen operativo"
      descripcion="Una vista de la gestión de clientes, solicitudes, cuentas y tarjetas."
    >
      <RouterLink v-if="puedeRegistrarSolicitud" class="boton" to="/solicitudes/nueva"><IconoSistema nombre="mas" :tamano="18" />Nueva solicitud</RouterLink>
    </EncabezadoPagina>

    <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />
    <EsqueletoCarga v-if="cargando" variante="resumen" etiqueta="Cargando resumen" />

    <template v-else>
      <div class="rejilla-indicadores">
        <TarjetaIndicador v-if="puedeVerClientes" titulo="Clientes registrados" :valor="String(clientes.length)" descripcion="Información y contacto disponibles" icono="usuarios" />
        <TarjetaIndicador v-if="puedeVerSolicitudes" titulo="Solicitudes pendientes" :valor="String(solicitudesPendientes.length)" descripcion="En espera de una decisión" icono="reloj" destacado />
        <TarjetaIndicador v-if="puedeVerCuentas" titulo="Cuentas activas" :valor="String(cuentas.length)" :descripcion="descripcionCuentas" icono="tarjeta" />
        <TarjetaIndicador v-if="puedeVerCuentas" titulo="Deuda de la cartera" :valor="formatearMoneda(deudaCartera)" descripcion="Consumos menos pagos registrados" icono="cartera" />
      </div>

      <div v-if="puedeVerSolicitudes" class="rejilla-paneles">
        <section class="panel">
          <div class="panel-cabecera">
            <div>
              <h2 class="panel-titulo">Solicitudes pendientes</h2>
              <p class="panel-subtitulo">
                {{ solicitudesPendientes.length }} {{ solicitudesPendientes.length === 1 ? 'solicitud por revisar' : 'solicitudes por revisar' }}
              </p>
            </div>
            <RouterLink class="enlace-accion" to="/solicitudes">Ver todas<IconoSistema nombre="flecha-derecha" :tamano="16" /></RouterLink>
          </div>
          <p v-if="pendientesVisibles.length === 0" class="estado-vacio">No hay solicitudes pendientes.</p>
          <div v-else class="contenedor-tabla">
            <table class="tabla tabla-pendientes">
              <thead>
                <tr>
                  <th>Cliente</th>
                  <th>Tipo</th>
                  <th>Ingreso mensual</th>
                  <th>Estado</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="solicitud in pendientesVisibles" :key="solicitud.id">
                  <td>
                    <p class="celda-fuerte">{{ solicitud.nombreCliente }}</p>
                    <p class="tabla-secundaria">{{ formatearCodigoSolicitud(solicitud.id) }} · {{ formatearFecha(solicitud.fechaSolicitud) }}</p>
                  </td>
                  <td>{{ solicitud.descripcionTipoTarjeta }}</td>
                  <td>{{ formatearMoneda(solicitud.ingresoMensual) }}</td>
                  <td><EtiquetaEstado :estado="solicitud.estado" /></td>
                  <td>
                    <RouterLink class="enlace-accion" :to="`/solicitudes/${solicitud.id}/revision`">
                      {{ puedeDecidir ? 'Revisar' : 'Ver detalle' }}<IconoSistema nombre="flecha-derecha" :tamano="16" />
                    </RouterLink>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="nota-panel"><IconoSistema nombre="info" :tamano="16" />Solo las solicitudes pendientes pueden aprobarse o rechazarse.</p>
        </section>

        <section class="panel">
          <div class="panel-cabecera">
            <div>
              <h2 class="panel-titulo">Actividad reciente</h2>
              <p class="panel-subtitulo">Últimos movimientos y decisiones</p>
            </div>
          </div>
          <p v-if="actividadReciente.length === 0" class="estado-vacio">Aún no hay actividad registrada.</p>
          <ul v-else class="lista-actividad">
            <li v-for="evento in actividadReciente" :key="evento.clave">
              <span class="icono-actividad"><IconoSistema :nombre="evento.icono" :tamano="18" /></span>
              <div>
                <p class="celda-fuerte">{{ evento.titulo }}</p>
                <p class="tabla-secundaria">{{ evento.detalle }}</p>
                <p class="fecha-actividad">{{ formatearFechaCorta(evento.fecha) }}</p>
              </div>
            </li>
          </ul>
        </section>
      </div>

      <section v-if="puedeVerCuentas" class="panel cartera">
        <div class="cartera-total">
          <p class="panel-titulo">Cartera de crédito</p>
          <p class="cartera-cifra">{{ formatearMoneda(carteraCredito) }}</p>
          <p class="panel-subtitulo">Límite total de {{ cuentas.length === 1 ? 'la cuenta' : `las ${cuentas.length} cuentas` }}</p>
          <RouterLink class="enlace-accion" to="/cuentas">Ver cuentas y tarjetas<IconoSistema nombre="flecha-derecha" :tamano="16" /></RouterLink>
        </div>
        <div class="cartera-uso">
          <div class="cartera-uso-cabecera">
            <strong>{{ formatearPorcentaje(porcentajeUtilizado) }} del crédito utilizado</strong>
            <span>Disponible: {{ formatearMoneda(creditoDisponible) }}</span>
          </div>
          <div class="barra-uso" role="progressbar" :aria-valuenow="Math.round(porcentajeUtilizado)" aria-valuemin="0" aria-valuemax="100">
            <div class="barra-uso-relleno" :style="{ width: `${porcentajeUtilizado}%` }"></div>
          </div>
          <p class="cartera-mora" :class="{ 'cartera-mora-activa': cuentasEnMora > 0 }">Cuentas en mora: <strong>{{ cuentasEnMora }}</strong></p>
          <p class="panel-subtitulo">El saldo representa deuda. Los consumos suman y los pagos restan.</p>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.rejilla-indicadores { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
.rejilla-paneles { display: grid; grid-template-columns: minmax(0, 1.75fr) minmax(0, 1fr); gap: 24px; align-items: start; }
.tabla-pendientes td:first-child { white-space: normal; min-width: 10rem; }
.nota-panel { display: flex; align-items: center; gap: 10px; padding: 16px 20px; border-top: 1px solid var(--color-borde); font-size: 13px; color: var(--color-texto-suave); }
.lista-actividad { list-style: none; margin: 0; padding: 0 24px 24px; display: flex; flex-direction: column; gap: 22px; }
.lista-actividad li { display: flex; gap: 14px; }
.icono-actividad { display: inline-flex; flex: none; align-items: center; justify-content: center; width: 36px; height: 36px; border-radius: var(--radio); background: var(--color-primario-suave); color: var(--color-primario); }
.fecha-actividad { margin-top: 4px; font-size: 11.5px; color: var(--color-texto-suave); }
.cartera { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 32px; padding: 24px; }
.cartera-total { padding-right: 32px; border-right: 1px solid var(--color-borde); }
.cartera-cifra { margin: 12px 0 14px; font-size: 28px; font-weight: 700; }
.cartera-uso { display: flex; flex-direction: column; justify-content: center; gap: 14px; }
.cartera-uso-cabecera { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px; font-size: 14px; }
.cartera-mora { font-size: 13px; color: var(--color-texto-suave); }
.cartera-mora-activa, .cartera-mora-activa strong { color: var(--color-peligro); }
.cartera-uso-cabecera span { color: var(--color-texto-suave); }
@media (max-width: 1100px) {
  .rejilla-paneles { grid-template-columns: 1fr; }
}
@media (max-width: 700px) {
  .cartera { grid-template-columns: 1fr; gap: 20px; }
  .cartera-total { padding-right: 0; border-right: 0; padding-bottom: 20px; border-bottom: 1px solid var(--color-borde); }
}
</style>
