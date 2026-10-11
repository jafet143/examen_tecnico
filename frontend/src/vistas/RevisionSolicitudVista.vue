<script setup>
import CampoMoneda from '../componentes/CampoMoneda.vue'
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EsqueletoCarga from '../componentes/EsqueletoCarga.vue'
import EncabezadoPagina from '../componentes/EncabezadoPagina.vue'
import CampoFormulario from '../componentes/CampoFormulario.vue'
import AvisoInformativo from '../componentes/AvisoInformativo.vue'
import IconoSistema from '../componentes/IconoSistema.vue'
import MensajeAlerta from '../componentes/MensajeAlerta.vue'
import EtiquetaEstado from '../componentes/EtiquetaEstado.vue'
import { listarSolicitudes, aprobarSolicitud, rechazarSolicitud } from '../servicios/solicitudesServicio'
import { obtenerCliente } from '../servicios/clientesServicio'
import { listarCuentasDeCliente } from '../servicios/cuentasServicio'
import { ESTADO_CUENTA_ACTIVA } from '../utilidades/constantes'
import NumeroProtegido from '../componentes/NumeroProtegido.vue'
import { tienePermiso } from '../autenticacion/servicioAutenticacion'
import { PERMISOS } from '../utilidades/permisos'
import { usarNotificacion } from '../composables/usarNotificacion'
import { usarCatalogoTiposTarjeta } from '../composables/usarCatalogoTiposTarjeta'
import { formatearMoneda, formatearFecha, formatearFechaHoy, formatearCodigoSolicitud, describirAntiguedad } from '../utilidades/formatos'
import { obtenerMensajeError, obtenerErroresDeCampos } from '../utilidades/errores'

const ruta = useRoute()
const enrutador = useRouter()
const { mostrarNotificacion } = usarNotificacion()
const { porcentajeMaximoLimiteSobreIngreso, mesesMinimosEstabilidadLaboral, errorCatalogo, cargarCatalogo } = usarCatalogoTiposTarjeta()
const idSolicitud = computed(() => Number(ruta.params.idSolicitud))

const solicitud = ref(null)
const cliente = ref(null)
const cuentasDelCliente = ref([])
const formulario = reactive({ montoAprobado: '', comentarioDecision: '' })
const erroresCampos = ref({})
const mensajeError = ref('')
const cargando = ref(true)
const enviando = ref(false)

const puedeDecidir = computed(() => tienePermiso(PERMISOS.SOLICITUDES_DECIDIR) && solicitud.value?.estado === 'PENDIENTE')
const codigoSolicitud = computed(() => formatearCodigoSolicitud(idSolicitud.value))
const montoAprobadoIngresado = computed(() => Number(formulario.montoAprobado))
const clienteEnMora = computed(() => cliente.value?.tieneCuentasEnMora === true)

const antiguedadInsuficiente = computed(
  () => mesesMinimosEstabilidadLaboral.value !== null && Number(solicitud.value?.mesesAntiguedadLaboral) < mesesMinimosEstabilidadLaboral.value
)
const mensajeEstabilidad = computed(
  () => `Se requiere al menos 1 año de estabilidad laboral. Antigüedad actual: ${solicitud.value.mesesAntiguedadLaboral} meses`
)
const cuentaDelMismoTipo = computed(
  () => cuentasDelCliente.value.find((cuenta) => cuenta.estadoCuenta === ESTADO_CUENTA_ACTIVA && cuenta.tipoTarjeta === solicitud.value?.tipoTarjeta) ?? null
)
const mensajeCuentaExistente = computed(
  () => `El cliente ya tiene una cuenta ${cuentaDelMismoTipo.value.descripcionTipoTarjeta} (número ${cuentaDelMismoTipo.value.numeroCuentaEnmascarado}). Si perdió o dañó la tarjeta, solicite una reposición`
)

const montoMaximo = computed(() => {
  if (!solicitud.value || porcentajeMaximoLimiteSobreIngreso.value === null) return null
  return Math.round(Number(solicitud.value.ingresoMensual) * porcentajeMaximoLimiteSobreIngreso.value) / 100
})
const montoExcedido = computed(() => montoMaximo.value !== null && montoAprobadoIngresado.value > montoMaximo.value)
const ayudaMontoAprobado = computed(() =>
  montoMaximo.value === null
    ? 'Solo aplica si apruebas la solicitud.'
    : `Máximo ${formatearMoneda(montoMaximo.value)} (${porcentajeMaximoLimiteSobreIngreso.value} % del ingreso mensual)`
)
const errorMontoAprobado = computed(() => {
  if (erroresCampos.value.montoAprobado) return erroresCampos.value.montoAprobado
  return montoExcedido.value ? `Supera el máximo de ${formatearMoneda(montoMaximo.value)} por ${formatearMoneda(montoAprobadoIngresado.value - montoMaximo.value)}` : ''
})
const motivoAprobacionBloqueada = computed(() => {
  if (antiguedadInsuficiente.value) return mensajeEstabilidad.value
  if (cuentaDelMismoTipo.value) return mensajeCuentaExistente.value
  if (clienteEnMora.value) return 'El cliente tiene cuentas en mora'
  if (montoExcedido.value) return 'El monto aprobado supera el máximo permitido'
  return ''
})
const subtituloPagina = computed(() =>
  puedeDecidir.value
    ? `Registra una decisión para la solicitud de ${solicitud.value.nombreCliente}.`
    : `Detalle de la solicitud de ${solicitud.value.nombreCliente}.`
)

function validarAprobacion() {
  const errores = {}
  if (!(montoAprobadoIngresado.value > 0)) errores.montoAprobado = 'El monto aprobado debe ser mayor a cero'
  else if (montoExcedido.value) errores.montoAprobado = errorMontoAprobado.value
  if (formulario.comentarioDecision.length > 500) errores.comentarioDecision = 'El comentario no puede exceder 500 caracteres'
  erroresCampos.value = errores
  return Object.keys(errores).length === 0
}

function validarRechazo() {
  const errores = {}
  if (!formulario.comentarioDecision.trim()) errores.comentarioDecision = 'Indique el motivo del rechazo'
  else if (formulario.comentarioDecision.length > 500) errores.comentarioDecision = 'El comentario no puede exceder 500 caracteres'
  erroresCampos.value = errores
  return Object.keys(errores).length === 0
}

async function cargarSolicitud() {
  try {
    const solicitudes = await listarSolicitudes('')
    solicitud.value = solicitudes.find((solicitudListada) => solicitudListada.id === idSolicitud.value) ?? null
    if (!solicitud.value) {
      mensajeError.value = 'La solicitud indicada no existe'
      return
    }
    ;[cliente.value, cuentasDelCliente.value] = await Promise.all([
      obtenerCliente(solicitud.value.idCliente),
      listarCuentasDeCliente(solicitud.value.idCliente),
      cargarCatalogo()
    ])
    if (montoMaximo.value !== null) formulario.montoAprobado = montoMaximo.value
    else mensajeError.value = errorCatalogo.value
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    cargando.value = false
  }
}

async function registrarDecision(esAprobacion) {
  mensajeError.value = ''
  if (esAprobacion && motivoAprobacionBloqueada.value) return
  if (!(esAprobacion ? validarAprobacion() : validarRechazo())) return
  enviando.value = true
  try {
    const comentario = formulario.comentarioDecision.trim()
    if (esAprobacion) {
      await aprobarSolicitud(idSolicitud.value, { montoAprobado: montoAprobadoIngresado.value, comentarioDecision: comentario || null })
    } else {
      await rechazarSolicitud(idSolicitud.value, { comentarioDecision: comentario })
    }
    mostrarNotificacion(esAprobacion ? 'Solicitud aprobada: cuenta abierta y tarjeta emitida' : 'Solicitud rechazada')
    enrutador.push('/solicitudes')
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
    erroresCampos.value = obtenerErroresDeCampos(error)
  } finally {
    enviando.value = false
  }
}

onMounted(cargarSolicitud)
</script>

<template>
  <div>
    <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />
    <EsqueletoCarga v-if="cargando" variante="detalle" etiqueta="Cargando solicitud" />

    <template v-else-if="solicitud && cliente">
      <EncabezadoPagina
        :migas="[{ texto: 'Solicitudes', ruta: '/solicitudes' }, { texto: codigoSolicitud }, { texto: puedeDecidir ? 'Revisión' : 'Detalle' }]"
        :titulo="`${puedeDecidir ? 'Revisar solicitud' : 'Solicitud'} ${codigoSolicitud}`"
        :descripcion="subtituloPagina"
        ruta-regreso="/solicitudes"
      >
        <EtiquetaEstado :estado="solicitud.estado" />
      </EncabezadoPagina>

      <section class="panel">
        <div class="panel-cuerpo resumen-solicitud">
          <div><p class="dato-etiqueta">Cliente</p><p class="dato-valor">{{ solicitud.nombreCliente }}</p></div>
          <div><p class="dato-etiqueta">Código de cliente</p><p class="dato-valor codigo-cliente">{{ solicitud.codigoCliente }}</p></div>
          <div><p class="dato-etiqueta">Tipo de tarjeta</p><p class="dato-valor">{{ solicitud.descripcionTipoTarjeta }}</p></div>
          <div><p class="dato-etiqueta">Fecha de solicitud</p><p class="dato-valor">{{ formatearFecha(solicitud.fechaSolicitud) }}</p></div>
          <div><p class="dato-etiqueta">Ingreso mensual declarado</p><p class="dato-valor">{{ formatearMoneda(solicitud.ingresoMensual) }}</p></div>
          <div><p class="dato-etiqueta">Empleador</p><p class="dato-valor">{{ solicitud.nombreEmpleador }}</p></div>
          <div><p class="dato-etiqueta">Inicio laboral</p><p class="dato-valor">{{ formatearFecha(solicitud.fechaInicioLaboral) }}</p></div>
          <div>
            <p class="dato-etiqueta">Antigüedad laboral</p>
            <p class="dato-valor">
              {{ describirAntiguedad(solicitud.mesesAntiguedadLaboral) }}
              <span v-if="antiguedadInsuficiente" class="etiqueta etiqueta-en_mora etiqueta-pequena">Menos de 1 año</span>
            </p>
          </div>
        </div>
      </section>

      <div class="rejilla-revision">
        <aside class="panel">
          <div class="panel-cabecera"><h2 class="panel-titulo">Perfil del cliente</h2></div>
          <div class="panel-cuerpo lista-datos">
            <div><p class="dato-etiqueta">Nombre completo</p><p class="dato-valor">{{ cliente.nombreCompleto }}</p></div>
            <div><p class="dato-etiqueta">Número de identificación</p><p class="dato-valor">{{ cliente.numeroIdentificacion }}</p></div>
            <div><p class="dato-etiqueta">Fecha de nacimiento</p><p class="dato-valor">{{ formatearFecha(cliente.fechaNacimiento) }}</p></div>
            <div><p class="dato-etiqueta">Correo electrónico</p><p class="dato-valor">{{ cliente.correoElectronico }}</p></div>
            <div><p class="dato-etiqueta">Teléfono</p><p class="dato-valor">{{ cliente.telefono }}</p></div>
            <div><p class="dato-etiqueta">Dirección</p><p class="dato-valor">{{ cliente.direccion }}</p></div>
            <RouterLink class="enlace-accion" :to="`/clientes/${cliente.id}`">Ver detalle del cliente<IconoSistema nombre="flecha-derecha" :tamano="14" /></RouterLink>
          </div>
        </aside>

        <form v-if="puedeDecidir" class="panel" novalidate @submit.prevent>
          <div class="panel-cabecera">
            <div>
              <h2 class="panel-titulo">Decisión de la solicitud</h2>
              <p class="panel-subtitulo">La solicitud permanece pendiente hasta registrar la decisión</p>
            </div>
          </div>
          <div class="panel-cuerpo campos-decision">
            <div class="rejilla-campos">
              <CampoFormulario id="fechaDecision" etiqueta="Fecha de decisión *" ayuda="Se registra automáticamente al guardar">
                <input id="fechaDecision" type="text" :value="formatearFechaHoy()" readonly />
              </CampoFormulario>
              <CampoFormulario id="montoAprobado" etiqueta="Monto aprobado *" :error="errorMontoAprobado" :ayuda="ayudaMontoAprobado">
                <CampoMoneda id="montoAprobado" v-model="formulario.montoAprobado" />
              </CampoFormulario>
            </div>

            <CampoFormulario id="comentarioDecision" etiqueta="Comentario / motivo" :error="erroresCampos.comentarioDecision" ayuda="Opcional al aprobar, obligatorio al rechazar. Se guarda junto con la fecha de decisión.">
              <textarea id="comentarioDecision" v-model="formulario.comentarioDecision" rows="3" maxlength="500"></textarea>
            </CampoFormulario>

            <AvisoInformativo v-if="antiguedadInsuficiente" variante="error" titulo="No se puede aprobar: estabilidad laboral insuficiente">
              {{ mensajeEstabilidad }}. Aún puedes rechazar la solicitud.
            </AvisoInformativo>
            <AvisoInformativo v-if="cuentaDelMismoTipo" variante="error" titulo="No se puede aprobar: el cliente ya tiene una cuenta de este tipo">
              {{ mensajeCuentaExistente }}. Aún puedes rechazar la solicitud.
            </AvisoInformativo>
            <AvisoInformativo v-if="clienteEnMora" variante="error" titulo="No se puede aprobar: el cliente tiene cuentas en mora">
              Debe ponerse al día en sus pagos antes de recibir una nueva tarjeta. Aún puedes rechazar la solicitud.
            </AvisoInformativo>
            <AvisoInformativo titulo="Aprobar abrirá una cuenta y emitirá su primera tarjeta">
              Se creará una cuenta {{ solicitud.descripcionTipoTarjeta }} de {{ solicitud.nombreCliente }} con límite
              {{ formatearMoneda(montoAprobadoIngresado > 0 ? montoAprobadoIngresado : 0) }} y saldo inicial {{ formatearMoneda(0) }}.
              El límite será igual al monto aprobado.
            </AvisoInformativo>
            <AvisoInformativo variante="advertencia" titulo="Para rechazar, registra un motivo">
              El motivo es obligatorio. Una solicitud rechazada no registra monto aprobado y no genera cuenta ni tarjeta.
            </AvisoInformativo>
          </div>
          <div class="panel-pie">
            <RouterLink class="boton boton-secundario" to="/solicitudes">Cancelar</RouterLink>
            <div class="acciones-pie">
              <button type="button" class="boton boton-secundario" :disabled="enviando" @click="registrarDecision(false)">
                <IconoSistema nombre="x" :tamano="18" />Rechazar
              </button>
              <button type="button" class="boton" :disabled="enviando || Boolean(motivoAprobacionBloqueada)" :title="motivoAprobacionBloqueada" @click="registrarDecision(true)">
                <IconoSistema nombre="check" :tamano="18" />{{ enviando ? 'Procesando...' : 'Aprobar' }}
              </button>
            </div>
          </div>
        </form>

        <section v-else class="panel">
          <div class="panel-cabecera">
            <div>
              <h2 class="panel-titulo">Decisión de la solicitud</h2>
              <p class="panel-subtitulo">Modo de solo lectura</p>
            </div>
          </div>
          <div class="panel-cuerpo campos-decision">
            <AvisoInformativo v-if="solicitud.estado === 'PENDIENTE'" variante="advertencia" titulo="Pendiente de decisión">
              Esta solicitud aún no tiene una decisión. Solo un perfil con permiso para decidir puede aprobarla o rechazarla.
            </AvisoInformativo>
            <template v-else>
              <div class="rejilla-datos">
                <div><p class="dato-etiqueta">Estado</p><EtiquetaEstado :estado="solicitud.estado" /></div>
                <div><p class="dato-etiqueta">Fecha de decisión</p><p class="dato-valor">{{ formatearFecha(solicitud.fechaDecision) }}</p></div>
                <div>
                  <p class="dato-etiqueta">Monto aprobado</p>
                  <p class="dato-valor">{{ solicitud.estado === 'APROBADA' ? formatearMoneda(solicitud.montoAprobado) : 'No aplica' }}</p>
                </div>
                <div><p class="dato-etiqueta">Decidida por</p><p class="dato-valor">{{ solicitud.usuarioDecision || '—' }}</p></div>
              </div>
              <div>
                <p class="dato-etiqueta">Comentario / motivo</p>
                <p class="dato-valor">{{ solicitud.comentarioDecision || 'Sin comentario' }}</p>
              </div>
              <AvisoInformativo v-if="solicitud.idCuenta" variante="exito" titulo="Cuenta abierta y primera tarjeta emitida">
                <span class="datos-aprobacion">
                  <span>Cuenta <NumeroProtegido :enmascarado="solicitud.numeroCuentaEnmascarado" :completo="solicitud.numeroCuentaCompleto" /></span>
                  <span>Tarjeta <NumeroProtegido :enmascarado="solicitud.numeroTarjetaEnmascarado" :completo="solicitud.numeroTarjetaCompleto" /></span>
                </span>
                <RouterLink class="enlace-accion" :to="`/cuentas/${solicitud.idCuenta}`">Ver detalle de la cuenta<IconoSistema nombre="flecha-derecha" :tamano="14" /></RouterLink>
              </AvisoInformativo>
            </template>
          </div>
          <div class="panel-pie">
            <RouterLink class="boton boton-secundario" to="/solicitudes">Volver a solicitudes</RouterLink>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.resumen-solicitud { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 24px; padding-top: 24px; }
.campos-decision { display: flex; flex-direction: column; gap: 22px; }
.campos-decision .rejilla-campos { margin-bottom: 0; }
.acciones-pie { display: flex; gap: 10px; }
.datos-aprobacion { display: flex; flex-wrap: wrap; gap: 4px 24px; margin-bottom: 8px; }
</style>
